"""Package exact, clean tracked Go source; exclude old compiled artifacts."""

import argparse
from pathlib import Path
import re
import subprocess
import zipfile

from release_safety import check_archive, check_file


def package(commit, output):
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Exact source commit required")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    if actual != commit:
        raise ValueError("Source checkout differs from release commit")
    if subprocess.run(["git", "diff", "--quiet", "HEAD", "--"]).returncode:
        raise ValueError("Dirty tracked source cannot be released")
    names = subprocess.check_output(["git", "ls-files", "-z"]).decode().split("\0")
    selected = []
    for name in filter(None, names):
        path = Path(name)
        check_file(path, name)  # Reject private inputs even if their suffix would be omitted.
        if path.suffix.lower() not in {".zip", ".apk", ".bin", ".elf", ".map"} and path.name != "SOURCE_COMMIT.txt":
            selected.append((path, name))
    destination = Path(output)
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".partial")
    try:
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as archive:
            for path, name in selected:
                archive.write(path, name)
            archive.writestr("SOURCE_COMMIT.txt", commit + "\n")
        check_archive(temporary)
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)
    return destination


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    print(package(args.source_commit, args.output))
