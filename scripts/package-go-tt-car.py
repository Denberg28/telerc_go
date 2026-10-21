#!/usr/bin/env python3
"""Create a self-contained Arduino TT-car sketch from the shared Go runtime."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

VERSION = "0.1.0"
FQBN = "esp32:esp32:esp32s3:CDCOnBoot=cdc,USBMode=hwcdc,FlashSize=4M,PartitionScheme=default,PSRAM=disabled,FlashMode=dio"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stage(root, output, commit):
    if output.exists():
        raise RuntimeError("Use a fresh output directory to exclude stale images")
    sketch = output / "TeleRC_Go_TT_Car"
    sketch.mkdir(parents=True)
    shared = root / "firmware/go/TeleRC_Go_Rover"
    wrapper = (root / "firmware/go/TeleRC_Go_TT_Car/TeleRC_Go_TT_Car.ino").read_text()
    include = '#include "../TeleRC_Go_Rover/TeleRC_Go_Rover.ino"'
    assert wrapper.count(include) == 1
    (sketch / "TeleRC_Go_TT_Car.ino").write_text(wrapper.replace(include, (shared / "TeleRC_Go_Rover.ino").read_text()))
    for header in sorted(shared.glob("*.h")):
        shutil.copy2(header, sketch / header.name)
    shutil.copy2(root / "docs/GO_TT_CAR.md", output / "START_HERE.md")
    manifest = {"version": VERSION, "source_commit": commit, "esp32_core": "3.3.2", "fqbn": FQBN,
                "driver": "MX1508", "control": "rear-drive/front-steering-bounded-jog", "hardware_tested": False,
                "profiles": {"WiFi": "-DTELERC_GO_ELRS=0", "WiFi-ELRS": "-DTELERC_GO_ELRS=1"},
                "source_files": {p.relative_to(output).as_posix(): digest(p) for p in sorted(sketch.iterdir())}}
    (output / "BUILD_PROFILE.json").write_text(json.dumps(manifest, indent=2) + "\n")
    return output


def archive(output, binary_root=None):
    if binary_root is not None:
        for profile in ("WiFi", "WiFi-ELRS"):
            source = binary_root / profile
            for suffix in (".bin", ".bootloader.bin", ".partitions.bin", ".merged.bin"):
                p = source / ("TeleRC_Go_TT_Car.ino" + suffix)
                if not p.is_file() or not p.stat().st_size:
                    raise RuntimeError(f"Incomplete actual-board build: {p}")
            dest = output / "firmware" / profile
            dest.mkdir(parents=True, exist_ok=True)
            for p in sorted(source.glob("*.bin")):
                shutil.copy2(p, dest / p.name)
    files = [p for p in sorted(output.rglob("*")) if p.is_file() and p.name != "SHA256SUMS.txt"]
    (output / "SHA256SUMS.txt").write_text("".join(f"{digest(p)}  {p.relative_to(output).as_posix()}\n" for p in files))
    files.append(output / "SHA256SUMS.txt")
    kind = "firmware" if binary_root else "source"
    path = output.parent / f"TeleRC-Go-TT-Car-v{VERSION}-{kind}.zip"
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as bundle:
        for p in files:
            bundle.write(p, p.relative_to(output).as_posix())
    with zipfile.ZipFile(path) as bundle:
        assert bundle.testzip() is None
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--binaries", type=Path)
    options = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    if options.binaries:
        record = json.loads((options.output / "BUILD_PROFILE.json").read_text())
        if record["source_commit"] != options.commit:
            raise RuntimeError("Source commit mismatch; refuse to package")
    else:
        stage(root, options.output, options.commit)
    print(archive(options.output, options.binaries))


if __name__ == "__main__":
    main()
