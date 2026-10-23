"""Create a validated source snapshot atomically, without private build inputs."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

from release_safety import check_file

out = Path("release-dist")
out.mkdir(exist_ok=True)
manifest = json.loads(Path("ecosystem-release.json").read_text())
commit = os.environ["GITHUB_SHA"]
if not re.fullmatch(r"[0-9a-f]{40}", commit):
    raise ValueError("Exact source commit required")
tracked = subprocess.check_output(["git", "ls-files", "-z"]).decode().split("\0")
selected = []
for name in tracked:
    if not name:
        continue
    path = Path(name)
    check_file(path, name)
    # Archived output stays in git history, outside the clean source snapshot.
    if path.suffix.lower() in {".zip", ".apk", ".bin", ".elf", ".map"}:
        continue
    if path.name == "SOURCE_COMMIT.txt":
        continue
    selected.append((path, name))
destination = out / f"{manifest['product']}-v{manifest['application_version']}-source.zip"
temporary = destination.with_suffix(".zip.partial")
try:
    with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as archive:
        for path, name in selected:
            archive.write(path, name)
        archive.writestr("SOURCE_COMMIT.txt", commit + "\n")
    with zipfile.ZipFile(temporary) as archive:
        if archive.testzip() is not None:
            raise ValueError("Source archive CRC failure")
    temporary.replace(destination)
finally:
    temporary.unlink(missing_ok=True)
for name in ("ecosystem-release.json", "docs/REALIGNMENT_V1.md"):
    (out / Path(name).name).write_bytes(Path(name).read_bytes())
(out / "SHA256SUMS.txt").write_text("".join(
    hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
    for path in sorted(out.iterdir())
    if path.is_file() and path.name != "SHA256SUMS.txt"
))
