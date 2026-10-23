"""Stage independently verified Go signing/firmware producers without relabeling source."""

import hashlib
import io
import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile

from release_safety import check_archive, check_directory

SOURCE = "36323abbebfa0d02b069b4f23cb502607f65e7c0"
SIGNING_COMMIT = "d621edb54ec340dba3878775cd76909d68bed487"
SIGNING_RUN = 39911336206
FIRMWARE_RUN = 39902508641
APK_SHA = "e2b1e6d98db4dbf6301a20fea5e384a7403db5ad142ab8c48d3752940261ef41"
SIGNER = "5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def stage(signed_path, firmware_path, output, notes):
    check_archive(signed_path)
    check_archive(firmware_path)
    out = Path(output)
    out.mkdir(parents=True, exist_ok=False)
    with zipfile.ZipFile(signed_path) as signed, zipfile.ZipFile(firmware_path) as firmware:
        prefix = "release-dist/"
        entries = {name[len(prefix):]: signed.read(name) for name in signed.namelist()
                   if name.startswith(prefix) and not name.endswith("/")}
        required = {"TeleRC-Go-v1.0.4.apk", "telerc_go-v1.0.4-source.zip",
                    "APK_VERIFICATION.json", "SIGNING_PROVENANCE.json",
                    "SHA256SUMS.txt", "ecosystem-release.json", "REALIGNMENT_V1.md"}
        require(set(entries) == required, "Unexpected signed artifact inventory")
        sums = {}
        for line in entries["SHA256SUMS.txt"].decode("ascii").splitlines():
            digest, name = line.split("  ", 1)
            require(name not in sums, "Duplicate signed artifact checksum")
            sums[name] = digest
        require(set(sums) == required - {"SHA256SUMS.txt"}, "Incomplete signed checksums")
        for name, digest in sums.items():
            require(hashlib.sha256(entries[name]).hexdigest() == digest, "Signed artifact checksum failure")
        require(hashlib.sha256(entries["TeleRC-Go-v1.0.4.apk"]).hexdigest() == APK_SHA,
                "Signed APK differs from the verified retained-signer build")
        v = json.loads(entries["APK_VERIFICATION.json"])
        require(v["source_commit"] == SOURCE and v["package"] == "io.github.denberg28.telerc.go"
                and v["version"] == "1.0.4" and v["version_code"] == 1000
                and v["upgrade_from"] == "1.0.3" and v["signer_sha256"] == [SIGNER],
                "Application identity/version/signer mismatch")
        provenance = json.loads(entries["SIGNING_PROVENANCE.json"])
        require(provenance["source_commit"] == SOURCE
                and provenance["signing_repository"] == "Denberg28/TeleRC"
                and provenance["signing_workflow_commit"] == SIGNING_COMMIT
                and int(provenance["signing_workflow_run"]) == SIGNING_RUN,
                "Signing producer provenance mismatch")
        with zipfile.ZipFile(io.BytesIO(entries["telerc_go-v1.0.4-source.zip"])) as source:
            require(source.read("SOURCE_COMMIT.txt").decode().strip() == SOURCE,
                    "Source snapshot mismatch")
        suites = [ET.fromstring(signed.read(name)) for name in signed.namelist()
                  if name.startswith("go/build/test-results/testDebugUnitTest/TEST-")
                  and name.endswith(".xml")]
        require(sum(int(s.attrib["tests"]) for s in suites) == 21,
                "Missing or changed application test evidence")
        require(all(int(s.attrib.get("failures", 0)) == 0
                    and int(s.attrib.get("errors", 0)) == 0
                    and int(s.attrib.get("skipped", 0)) == 0 for s in suites),
                "Failed/incomplete application tests")
        fw_data = firmware.read("firmware-dist/telerc_go-v1.0-firmware.zip")
        with zipfile.ZipFile(io.BytesIO(fw_data)) as bundle:
            manifest = json.loads(bundle.read("FIRMWARE_MANIFEST.json"))
            require(manifest["source_commit"] == SOURCE and len(manifest["builds"]) == 3,
                    "Firmware source/profile mismatch")
            for profile in manifest["builds"]:
                for image in profile["images"]:
                    data = bundle.read(image["file"])
                    require(len(data) == image["bytes"]
                            and hashlib.sha256(data).hexdigest() == image["sha256"],
                            "Firmware image hash/size mismatch")
        for name, data in entries.items():
            if name != "SHA256SUMS.txt":
                (out / name).write_bytes(data)
        (out / "telerc_go-v1.0-firmware.zip").write_bytes(fw_data)
        with zipfile.ZipFile(out / "TeleRC-Go-v1-test-evidence.zip", "w", zipfile.ZIP_DEFLATED) as evidence:
            for role, archive in [("android", signed), ("firmware", firmware)]:
                for name in sorted(archive.namelist()):
                    if name.endswith((".xml", ".json", ".log", ".html", ".txt")):
                        evidence.writestr(role + "/" + name, archive.read(name))
    (out / "START_HERE.md").write_bytes(Path(notes).read_bytes())
    status = dict(release_tag="v1.0.0", application_version="1.0.4", source_commit=SOURCE,
                  signing_run=SIGNING_RUN, firmware_run=FIRMWARE_RUN,
                  publication_commit=os.environ.get("GITHUB_SHA"), channel="prerelease",
                  stable_qualified=False, hardware_validation="BLOCKED",
                  software_build_gates="PASS", android_tests=21, actual_firmware_profiles=3)
    (out / "RELEASE_STATUS.json").write_text(json.dumps(status, indent=2) + "\n")
    (out / "SHA256SUMS.txt").write_text("".join(
        hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
        for path in sorted(out.iterdir()) if path.name != "SHA256SUMS.txt"))
    check_directory(out)
    print("PASS: retained signer, exact source/APK, 21 tests, 3 firmware profiles, checksums and sanitation")


if __name__ == "__main__":
    stage(*sys.argv[1:])
