"""Exercise the actual source packaging boundary, including accidental private inputs."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile


class GoReleaseSourceTests(unittest.TestCase):
    def run_package(self, extra=None, dirty=False):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "scripts").mkdir()
            for name in ["release_safety.py", "package_go_release_source.py"]:
                shutil.copyfile(Path(__file__).parent / name, root / "scripts" / name)
            (root / "public.txt").write_text("public source")
            (root / "old.apk").write_bytes(b"obsolete binary output")
            if extra:
                name, content = extra
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content)
            subprocess.run(["git", "init", "-q"], cwd=root, check=True)
            subprocess.run(["git", "add", "."], cwd=root, check=True)
            subprocess.run(["git", "-c", "user.name=Test", "-c", "user.email=test@example.invalid",
                            "-c", "commit.gpgsign=false", "commit", "-qm", "fixture"], cwd=root, check=True)
            commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
            if dirty:
                (root / "public.txt").write_text("unreviewed modification")
            result = subprocess.run([sys.executable, "scripts/package_go_release_source.py",
                                     "--source-commit", commit, "--output", "dist/source.zip"],
                                    cwd=root, capture_output=True, text=True)
            path = root / "dist/source.zip"
            if extra or dirty:
                self.assertNotEqual(result.returncode, 0, result.stdout)
                self.assertFalse(path.exists())
            else:
                self.assertEqual(result.returncode, 0, result.stderr)
                with zipfile.ZipFile(path) as archive:
                    self.assertEqual(archive.read("SOURCE_COMMIT.txt").decode().strip(), commit)
                    self.assertIn("public.txt", archive.namelist())
                    self.assertNotIn("old.apk", archive.namelist())

    def test_clean_source_contains_exact_commit_and_omits_old_binaries(self):
        self.run_package()

    def test_private_environment_is_rejected(self):
        self.run_package((".env.production", "TEST_ONLY=fixture"))

    def test_private_signing_input_is_rejected(self):
        self.run_package(("signing.jks", "not a real key"))

    def test_provisioned_config_is_rejected(self):
        self.run_package(("private-fleet/Config.h", "#define PROVISIONED 1\n"))

    def test_modified_tracked_source_is_rejected(self):
        self.run_package(dirty=True)


if __name__ == "__main__":
    unittest.main()
