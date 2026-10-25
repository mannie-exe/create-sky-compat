import hashlib
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = 'create-sky-compat'


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='release-check-')
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / 'tools').mkdir()
        shutil.copy2(ROOT / 'tools/release.py', self.root / 'tools/release.py')
        (self.root / 'gradle.properties').write_text('mod_version=0.1.0\n')
        (self.root / 'RELEASE_NOTES.md').write_text('Fixture only\n')
        self.git('init', '-q')
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'fixture')
        self.git('tag', 'v0.1.0')
        self.filename = f'{PROJECT}-0.1.0-mc1.21.1-neoforge.jar'
        for runner in ['ubuntu-24.04', 'windows-2025', 'macos-15']:
            target = self.root / 'build/ci' / f'build-{runner}/libs'
            target.mkdir(parents=True)
            (target / self.filename).write_bytes(b'fixture binary')
            (target / f'{PROJECT}-0.1.0-sources.jar').write_bytes(b'fixture sources')

    def git(self, *args):
        return subprocess.run(['git', *args], cwd=self.root, check=True, capture_output=True)

    def stage(self, tag='v0.1.0'):
        return subprocess.run([sys.executable, 'tools/release.py', tag, '--stage-only'],
                              cwd=self.root, text=True, capture_output=True)

    def test_stages_matching_builds_without_publishing(self):
        result = self.stage()
        self.assertEqual(0, result.returncode, result.stderr)
        release = self.root / 'build/release'
        self.assertEqual(5, len(list(release.iterdir())))
        manifest = (release / 'SHA512SUMS').read_text()
        self.assertIn(hashlib.sha512(b'fixture binary').hexdigest(), manifest)

    def test_rejects_cross_platform_mismatch(self):
        (self.root / 'build/ci/build-macos-15/libs' / self.filename).write_bytes(b'different')
        result = self.stage()
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Cross-platform build mismatch', result.stderr)

    def test_rejects_version_mismatch(self):
        result = self.stage('v0.2.0')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Tag must equal mod_version', result.stderr)

    def test_rejects_wrong_checkout(self):
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '--allow-empty', '-qm', 'later')
        result = self.stage()
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Release checkout does not match tag', result.stderr)

    def test_rejects_missing_artifact(self):
        (self.root / 'build/ci/build-windows-2025/libs' / self.filename).unlink()
        self.assertNotEqual(0, self.stage().returncode)
