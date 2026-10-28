"""Verify production package, increasing version and retained signer before release."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

TOOLS = Path(os.environ["ANDROID_HOME"]) / "build-tools/35.0.0"
SIGNER = "5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec"
BASELINE_SHA = "e2b1e6d98db4dbf6301a20fea5e384a7403db5ad142ab8c48d3752940261ef41"


def run(*args):
    return subprocess.check_output(list(map(str, args)), text=True)


def identity(path):
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",
                        run(TOOLS / "aapt", "dump", "badging", path)).groups()
    signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)",
                         run(TOOLS / "apksigner", "verify", "--print-certs", path))
    return package, signers


apk, baseline = map(Path, sys.argv[1:])
assert hashlib.sha256(baseline.read_bytes()).hexdigest() == BASELINE_SHA
package, signers = identity(apk)
previous, previous_signers = identity(baseline)
assert package == ("io.github.denberg28.telerc.go", "1001", "1.0.5"), package
assert previous == ("io.github.denberg28.telerc.go", "1000", "1.0.4"), previous
assert signers == previous_signers == [SIGNER]
run(TOOLS / "zipalign", "-c", "-P", "16", "4", apk)
permissions = run(TOOLS / "aapt", "dump", "permissions", apk)
for name in ["READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
             "CAMERA", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "RECORD_AUDIO"]:
    assert "android.permission." + name not in permissions, name
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None
    dex = b"".join(archive.read(name) for name in archive.namelist() if re.fullmatch(r"classes\d*\.dex", name))
    assert b"ThemeImageStore" in dex
    assert b"GoImageFixtureProvider" not in dex
    assert not any(name.startswith("lib/") for name in archive.namelist())
suites = [ET.parse(path).getroot() for path in Path("go/build/test-results/testDebugUnitTest").glob("TEST-*.xml")]
counts = {name: sum(int(suite.get(name, 0)) for suite in suites)
          for name in ["tests", "failures", "errors", "skipped"]}
assert counts == dict(tests=23, failures=0, errors=0, skipped=0), counts
metadata = dict(source_commit=os.environ["GO_SOURCE"], package=package[0], version=package[2],
                version_code=int(package[1]), upgrade_from=previous[2], signer_sha256=signers,
                sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), tests=counts)
(apk.parent / "APK_VERIFICATION.json").write_text(json.dumps(metadata, indent=2) + "\n")
print("PASS: production Go 1.0.5/code1001, exact baseline/signer, permissions, alignment/ZIP and 23 tests")
