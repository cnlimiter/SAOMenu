"""Regression coverage for screenshot-gate false positives."""

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

from diff_screens import diff_images


class ScreenshotGateTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="saomenu-gate-")
        self.addCleanup(temporary.cleanup)
        self.baseline = Path(temporary.name) / "baseline"
        self.current = Path(temporary.name) / "current"
        self.baseline.mkdir()
        self.current.mkdir()
        Image.new("RGB", (1, 1), (0, 0, 0)).save(self.baseline / "frame.png")
        Image.new("RGB", (1, 1), (255, 0, 0)).save(self.current / "frame.png")

    def test_single_channel_change_is_not_averaged_away(self):
        ratio, maximum, pixels = diff_images(
            self.baseline / "frame.png", self.current / "frame.png", 200
        )
        self.assertEqual((ratio, maximum, pixels), (1.0, 255, 1))

    def test_filter_matching_no_screenshot_fails(self):
        result = subprocess.run(
            [sys.executable, str(Path(__file__).with_name("diff_screens.py")),
             str(self.baseline), str(self.current), "--only", "absent-screen"],
            capture_output=True,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn(b"PASS", result.stdout)


if __name__ == "__main__":
    unittest.main()
