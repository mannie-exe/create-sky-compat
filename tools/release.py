#!/usr/bin/env python3
"""Publish only the identical, tested binaries from this tag's three CI jobs."""
import argparse
import hashlib
import pathlib
import shutil
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
version = next(
    line.split("=", 1)[1]
    for line in (root / "gradle.properties").read_text().splitlines()
    if line.startswith("mod_version=")
)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("tag")
parser.add_argument("--stage-only", action="store_true", help="Validate and stage without publishing")
args = parser.parse_args()
tag = args.tag
if tag != f"v{version}":
    raise SystemExit("Tag must equal mod_version")
head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
tag_head = subprocess.check_output(["git", "rev-parse", f"{tag}^{{commit}}"], cwd=root, text=True).strip()
if head != tag_head:
    raise SystemExit("Release checkout does not match tag")

destination = root / "build/release"
destination.mkdir(parents=True, exist_ok=False)
for filename in [
    f"sky-village-neighbors-{version}-mc1.21.1-neoforge.jar",
    f"sky-village-neighbors-{version}-sources.jar",
]:
    builds = [
        root / "build/ci" / f"build-{runner}" / "libs" / filename
        for runner in ["ubuntu-24.04", "windows-2025", "macos-15"]
    ]
    hashes = {hashlib.sha256(path.read_bytes()).hexdigest() for path in builds}
    if len(hashes) != 1:
        raise SystemExit(f"Cross-platform build mismatch: {filename}")
    shutil.copyfile(builds[0], destination / filename)

subprocess.run([
    "git", "archive", "--format=zip", f"--prefix=sky-village-neighbors-{version}/",
    f"--output={destination / f'sky-village-neighbors-{version}-source.zip'}", head,
], cwd=root, check=True)
assets = sorted(destination.iterdir())
for algorithm in ["sha256", "sha512"]:
    manifest = destination / f"{algorithm.upper()}SUMS"
    manifest.write_text("".join(
        f"{hashlib.new(algorithm, path.read_bytes()).hexdigest()}  {path.name}\n"
        for path in assets
    ), encoding="utf-8")
if args.stage_only:
    print("Verified release files:", destination)
    raise SystemExit(0)
subprocess.run([
    "gh", "release", "create", tag,
    *map(str, sorted(destination.iterdir())),
    "--verify-tag", "--draft", "--prerelease", "--title", f"Sky Village Neighbors {version}",
    "--notes-file", "RELEASE_NOTES.md",
], cwd=root, check=True)
subprocess.run(["gh", "release", "edit", tag, "--draft=false"], cwd=root, check=True)
