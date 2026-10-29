"""Stage immutable, qualified Go 1.0.6 build artifacts for the authorized prerelease."""
import hashlib
import io
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

from release_safety import check_archive, check_directory

SOURCE = "d66e4751b050caae7c9b53ef4e9235b153f78aa7"
SIGNING_COMMIT = "23c82095097fd1945a67ecffe1e99f2cb62e4dcf"
SIGNING_RUN = 40855768830
CHECKS_RUN = 40855741152
SIGNER = "5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec"
REQUIRED = {"TeleRC-Go-v1.0.6.apk", "telerc_go-v1.0.6-source.zip", "APK_VERIFICATION.json",
            "SIGNING_PROVENANCE.json", "BUILD_INFO.txt", "RELEASE_NOTES.md",
            "TEST_GUIDE.md", "SHA256SUMS.txt"}


def require(value, message):
    if not value:
        raise ValueError(message)


def test_counts(archive):
    suites = [ET.fromstring(archive.read(name)) for name in archive.namelist()
              if name.startswith("go/build/test-results/testDebugUnitTest/TEST-") and name.endswith(".xml")]
    return {key: sum(int(suite.get(key, 0)) for suite in suites)
            for key in ["tests", "failures", "errors", "skipped"]}


def stage(signed_path, checks_path, native_path, metadata, output):
    metadata = Path(metadata)
    read = lambda name: json.loads((metadata / (name + ".json")).read_text())
    signed_run, checks_run = read("signed-run"), read("checks-run")
    for run, identity, commit, repository in [
            (signed_run, SIGNING_RUN, SIGNING_COMMIT, "Denberg28/TeleRC"),
            (checks_run, CHECKS_RUN, SOURCE, "Denberg28/telerc_go")]:
        require(run["id"] == identity and run["head_sha"] == commit and run["conclusion"] == "success"
                and run["repository"]["full_name"] == repository, "Unqualified producer/source")
    require(any(job["name"] == "signed-go" and job["conclusion"] == "success"
                for job in read("signed-jobs")["jobs"]), "Signing job did not pass")
    for name in ["checks", "wallpaper"]:
        require(any(job["name"] == name and job["conclusion"] == "success"
                    for job in read("checks-jobs")["jobs"]), "Missing passing " + name + " job")
    producers = {}
    for role, path, run, name in [
            ("signed", signed_path, signed_run, "TeleRC-Go-v1.0.6-retained-signer"),
            ("checks", checks_path, checks_run, "TeleRC-Go-checks"),
            ("native", native_path, checks_run, "TeleRC-Go-wallpaper-evidence")]:
        artifact = read(role + "-artifact")
        digest = "sha256:" + hashlib.sha256(Path(path).read_bytes()).hexdigest()
        require(artifact["name"] == name and artifact["expired"] is False
                and artifact["workflow_run"]["id"] == run["id"]
                and artifact["workflow_run"]["head_sha"] == run["head_sha"]
                and artifact["digest"] == digest, "Artifact provenance/digest mismatch: " + role)
        check_archive(path)
        producers[role] = dict(run=run["id"], commit=run["head_sha"], artifact=artifact["id"], digest=digest)
    out = Path(output)
    out.mkdir(parents=True, exist_ok=False)
    with zipfile.ZipFile(signed_path) as signed, zipfile.ZipFile(checks_path) as checks, zipfile.ZipFile(native_path) as native:
        files = {name.removeprefix("release-dist/"): signed.read(name) for name in signed.namelist()
                 if name.startswith("release-dist/") and not name.endswith("/")}
        require(set(files) == REQUIRED, "Unexpected signed release inventory")
        sums = {}
        for line in files["SHA256SUMS.txt"].decode("ascii").splitlines():
            digest, name = line.split("  ", 1)
            require(name not in sums and re.fullmatch(r"[a-f0-9]{64}", digest), "Invalid producer checksums")
            sums[name] = digest
        require(set(sums) == REQUIRED - {"SHA256SUMS.txt"}, "Incomplete producer checksums")
        for name, digest in sums.items():
            require(hashlib.sha256(files[name]).hexdigest() == digest, "Producer checksum mismatch: " + name)
        verification = json.loads(files["APK_VERIFICATION.json"])
        require(verification["source_commit"] == SOURCE and verification["package"] == "io.github.denberg28.telerc.go"
                and verification["version"] == "1.0.6" and verification["version_code"] == 1002
                and verification["upgrade_from"] == "1.0.5" and verification["signer_sha256"] == [SIGNER]
                and verification["sha256"] == sums["TeleRC-Go-v1.0.6.apk"], "Production APK identity mismatch")
        provenance = json.loads(files["SIGNING_PROVENANCE.json"])
        require(provenance["source_commit"] == SOURCE and provenance["signing_repository"] == "Denberg28/TeleRC"
                and provenance["signing_workflow_commit"] == SIGNING_COMMIT
                and provenance["signing_workflow_run"] == SIGNING_RUN, "Signing provenance mismatch")
        with zipfile.ZipFile(io.BytesIO(files["telerc_go-v1.0.6-source.zip"])) as source:
            require(source.read("SOURCE_COMMIT.txt").decode().strip() == SOURCE, "Source snapshot mismatch")
        expected = dict(tests=34, failures=0, errors=0, skipped=0)
        require(test_counts(signed) == test_counts(checks) == verification["tests"] == expected,
                "Missing/failed/incomplete Go test evidence")
        result = native.read("result.txt")
        require(b"INSTRUMENTATION_CODE: -1" in result and b"PASS: gallery image picker" in result,
                "Native wallpaper regression did not pass")
        game_result=native.read("test-result.txt")
        require(b"INSTRUMENTATION_CODE: -1" in game_result and b"PASS: offline Test game" in game_result,
                "Native game/geometry regression did not pass")
        for name, data in files.items():
            if name != "SHA256SUMS.txt":
                (out / name).write_bytes(data)
        (out / "SIGNER_SHA256SUMS.txt").write_bytes(files["SHA256SUMS.txt"])
        guide = files["TEST_GUIDE.md"].decode()
        procedure = guide[guide.index("## Phone / restrained-wheel acceptance"):guide.index("## Local handoff")]
        notes = """# TeleRC Go 1.0.6 — Test game and aligned controls

- Test before Phone Connect opens an offline rover game and free-drive preview.
- Steer/Drive captions share centering, heights and horizontal/vertical spacing.
- Game entry disconnects/cancels live control; Controls returns without reconnecting or arming.
- Game/Test switching, Reset, music, saved high score and pause/neutral lifecycle.

Production package io.github.denberg28.telerc.go; version 1.0.6/code1002.
Retains the production certificate from 1.0.5, supporting an in-place upgrade.
Install this APK over the existing Go; no uninstall or firmware reflash is needed.
Actual phone upgrade, gallery provider and restrained-wheel acceptance remain pending.
Software checks include 34 Go  + 162 full-app JVM tests, 18 packaging tests, build/lint,
default sanitizers, native wallpaper and Game/Test/geometry at normal/large text.
This is a prerelease. The stable in-app feed skips prereleases; install it manually.
The previous release remains available; an Android downgrade may require uninstall/data loss.
See TEST_GUIDE.md, BUILD_INFO.txt and SHA256SUMS.txt for verification and acceptance.
"""
        (out / "RELEASE_NOTES.md").write_text(notes)
        (out / "TEST_GUIDE.md").write_text(notes + "\n\n" + procedure)
        with zipfile.ZipFile(out / "TeleRC-Go-v1.0.6-test-evidence.zip", "w", zipfile.ZIP_DEFLATED) as evidence:
            for role, archive in [("signed", signed), ("checks", checks)]:
                for name in archive.namelist():
                    if not name.endswith("/") and (name.startswith("go/build/test-results/")
                            or name.startswith("go/build/reports/") or name.startswith("app/build/reports/")):
                        evidence.writestr(role + "/" + name, archive.read(name))
            for name in ["result.txt", "go-wallpaper-custom.png", "go-1.0-settings.png", "go-1.3-settings.png",
                         "go-1.0-background.png", "go-1.3-background.png", "test-result.txt",
                         "go-test-1.0-controls.png", "go-test-1.3-controls.png",
                         "go-test-1.0-game.png", "go-test-1.3-game.png",
                         "go-test-1.0-drive.png", "go-test-1.3-drive.png"]:
                data = native.read(name)
                if name.endswith(".png"):
                    require(data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) > 1000, "Missing native screenshot")
                evidence.writestr("native/" + name, data)
            evidence.writestr("PRODUCERS.json", json.dumps(producers, indent=2) + "\n")
    status = dict(release_tag="go-v1.0.6", application_version="1.0.6", version_code=1002,
                  source_commit=SOURCE, package="io.github.denberg28.telerc.go", channel="prerelease",
                  stable_qualified=False, hardware_validation="BLOCKED", software_gates="PASS",
                  android_tests=34, packaging_tests=18, native_wallpaper="PASS", native_game_geometry="PASS",
                  firmware="unchanged; retain existing matched firmware", producers=producers)
    (out / "RELEASE_STATUS.json").write_text(json.dumps(status, indent=2) + "\n")
    with (out / "BUILD_INFO.txt").open("a") as stream:
        stream.write("PASS: independent full-app/host/default-sanitizer/native Go gates: https://github.com/Denberg28/telerc_go/actions/runs/" + str(CHECKS_RUN) + "\n")
    (out / "SHA256SUMS.txt").write_text("".join(hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
        for path in sorted(out.iterdir()) if path.is_file() and path.name != "SHA256SUMS.txt"))
    check_directory(out)
    print("PASS: immutable producers, signed APK/source, 34+18 tests, native wallpaper evidence, checksums and archive exclusions")


if __name__ == "__main__":
    stage(*sys.argv[1:])
