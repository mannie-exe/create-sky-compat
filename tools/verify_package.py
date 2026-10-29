#!/usr/bin/env python3
"""Check release metadata and ensure fixture/upstream classes are not bundled."""
import json
import sys
import tomllib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
version = sys.argv[1]
with zipfile.ZipFile(root / f'build/libs/create-sky-compat-{version}-mc1.21.1-neoforge.jar') as jar:
    names = set(jar.namelist())
    metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version'] == version
    deps = {entry['modId']: entry for entry in metadata['dependencies']['create_sky_compat']}
    assert deps['minecraft']['versionRange'] == '[1.21.1]'
    assert deps['sky_archipelago']['versionRange'] == '[1.3.6]'
    assert deps['skyvillages']['versionRange'] == '[1.0.6]'
    assert deps['sable']['versionRange'] == '[2.0.6]'
    assert deps.pop('sky_village_neighbors')['type'] == 'incompatible'
    delivery = deps.pop('aeronautics_delivery_quests')
    assert delivery['type'] == 'optional' and delivery['versionRange'] == '[1.1.1]'
    assert {name for name, d in deps.items() if d['type'] == 'required'} == {'minecraft', 'neoforge', 'sky_archipelago'}
    assert all(deps[name]['type'] == 'optional' for name in ['skyvillages', 'sable', 'architectury'])
    assert deps['distanthorizons']['type'] == 'optional' and deps['distanthorizons']['versionRange'] == '[3.3.3]'
    assert 'seedrenderer' not in deps
    assert not any('/createskycompat/lod/' in n or '/mixin/lod/' in n or '/clientprobe/' in n for n in names)
    assert 'LICENSE' in names
    assert not any(n.startswith(('org/sathrek/', 'com/aureljz/', 'de/bluecolored/', 'com/palm1/', 'com/simibubi/')) or '/probe/' in n or n.endswith('.jar') for n in names)
    mixin = json.loads(jar.read('create_sky_compat.mixins.json'))
    assert mixin['required'] and mixin['injectors']['defaultRequire'] == 1
    assert mixin['plugin'] == 'design.inherent.createskycompat.OptionalMixinPlugin'
    assert 'design/inherent/createskycompat/mixin/StructureSupportMixin.class' in names
print('Release metadata and package boundaries verified')
