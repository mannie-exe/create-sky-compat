import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class PonderEncodingTest(unittest.TestCase):
    def test_generator_does_not_depend_on_the_host_text_encoding(self):
        result = subprocess.run(
            [sys.executable, '-X', 'warn_default_encoding', '-W', 'error::EncodingWarning',
             str(ROOT / 'tools/generate_ponder.py'), '--check'],
            cwd=ROOT, capture_output=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr.decode('utf-8', errors='replace'))
