"""Exercise the install helper without an Android device or network."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "tools" / "build-and-install.sh"


class BuildAndInstallTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "tools").mkdir()
        (self.root / "bin").mkdir()
        shutil.copy(SOURCE, self.root / "tools" / "build-and-install.sh")
        (self.root / "tools" / "build-and-install.sh").chmod(0o755)
        self.write_executable("gradlew", '#!/bin/sh\nprintf "gradle:%s\\n" "$*" >> "$TOOL_LOG"\nexit "${BUILD_FAIL:-0}"\n')
        self.write_executable("bin/adb", '#!/bin/sh\nprintf "adb:%s\\n" "$*" >> "$TOOL_LOG"\nexit 0\n')
        self.log = self.root / "calls.log"
        self.env = dict(os.environ, PATH=str(self.root / "bin") + os.pathsep + os.environ.get("PATH", ""), TOOL_LOG=str(self.log))

    def tearDown(self):
        self.temp.cleanup()

    def write_executable(self, name, content):
        path = self.root / name
        path.write_text(content)
        path.chmod(0o755)

    def run_helper(self, *args, **env):
        return subprocess.run([str(self.root / "tools" / "build-and-install.sh"), *args], env=dict(self.env, **env), capture_output=True, text=True, check=False)

    def calls(self):
        return self.log.read_text().splitlines() if self.log.exists() else []

    def test_single_device_builds_then_installs(self):
        result = self.run_helper()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["adb:get-state", "gradle:assembleDebug", "adb:install -r app/build/outputs/apk/debug/app-debug.apk"], self.calls())

    def test_serial_applies_to_both_adb_commands(self):
        result = self.run_helper("TV-123")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["adb:-s TV-123 get-state", "gradle:assembleDebug", "adb:-s TV-123 install -r app/build/outputs/apk/debug/app-debug.apk"], self.calls())

    def test_failed_build_never_installs(self):
        result = self.run_helper(BUILD_FAIL="1")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(["adb:get-state", "gradle:assembleDebug"], self.calls())

    def test_rejects_multiple_serial_arguments(self):
        result = self.run_helper("one", "two")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual([], self.calls())


if __name__ == "__main__":
    unittest.main()
