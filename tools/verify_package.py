#!/usr/bin/env python3
"""Check release metadata and ensure fixture/upstream classes are not bundled."""
import json
import sys
import tomllib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
version = sys.argv[1]
with zipfile.ZipFile(root / f'build/libs/sky-village-neighbors-{version}-mc1.21.1-neoforge.jar') as jar:
    names = set(jar.namelist())
    metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version'] == version
    deps = {entry['modId']: entry for entry in metadata['dependencies']['sky_village_neighbors']}
    assert deps['minecraft']['versionRange'] == '[1.21.1]'
    assert deps['sky_archipelago']['versionRange'] == '[1.3.6]'
    assert deps['skyvillages']['versionRange'] == '[1.0.6]'
    assert all(d['type'] == 'required' for d in deps.values())
    assert 'LICENSE' in names
    assert not any(n.startswith(('org/sathrek/', 'com/aureljz/')) or '/probe/' in n or n.endswith('.jar') for n in names)
    mixin = json.loads(jar.read('sky_village_neighbors.mixins.json'))
    assert mixin['required'] and mixin['injectors']['defaultRequire'] == 1
    assert 'design/inherent/skyvillageneighbors/mixin/StructureSupportMixin.class' in names
print('Release metadata and package boundaries verified')
