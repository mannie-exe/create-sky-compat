#!/usr/bin/env python3
"""Trace typed nested features from a tools/probe.py --coverage capture."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('capture', type=Path, help='Resolved native coverage JSON')
parser.add_argument('--output', type=Path, required=True, help='Derived feature-closure JSON')
args = parser.parse_args()
data = json.loads(args.capture.read_text())
if data.get('complete') is not True:
    raise SystemExit('Coverage capture is incomplete')

placed_registry = data['placedFeatures']
configured_registry = data['configuredFeatures']
placed, configured, feature_types, modifiers = set(), set(), set(), set()
heights = []


def placement(value, path, top, stack=()):
    if isinstance(value, str):
        if value.startswith('#'):
            matches = [key for key, row in placed_registry.items() if value[1:] in row['tags']]
            if not matches:
                raise ValueError('Missing placement tag ' + value)
            for key in matches:
                placement(key, path + [value], top, stack)
            return
        if ('placed', value) in stack:
            raise ValueError('Feature cycle ' + value)
        placed.add(value)
        stack += (('placed', value),)
        path += [value]
        value = placed_registry[value]['value']
    for modifier in value['placement']:
        modifiers.add(modifier['type'])
        if modifier['type'] == 'minecraft:height_range':
            heights.append({
                'top': top, 'path': path, 'modifier': modifier, 'nested': len(path) > 1,
                'tagged': any(tag in {'create_sky_compat:relative_geology', 'create_sky_compat:relative_surface'}
                              for tag in placed_registry[top]['tags']),
            })
    configuration(value['feature'], path, top, stack)


def configuration(value, path, top, stack):
    if isinstance(value, str):
        if ('configured', value) in stack:
            raise ValueError('Feature cycle ' + value)
        configured.add(value)
        stack += (('configured', value),)
        value = configured_registry[value]['value']
    kind, config = value['type'], value['config']
    feature_types.add(kind)
    if kind == 'minecraft:random_selector':
        for index, child in enumerate(config['features']):
            placement(child['feature'], path + [f'choice[{index}]'], top, stack)
        placement(config['default'], path + ['default'], top, stack)
    elif kind == 'minecraft:simple_random_selector':
        choices = config['features'] if isinstance(config['features'], list) else [config['features']]
        for index, child in enumerate(choices):
            placement(child, path + [f'choice[{index}]'], top, stack)
    elif kind == 'minecraft:random_boolean_selector':
        for key in ['feature_true', 'feature_false']:
            placement(config[key], path + [key], top, stack)
    elif kind in ['minecraft:vegetation_patch', 'minecraft:waterlogged_vegetation_patch']:
        placement(config['vegetation_feature'], path + ['vegetation'], top, stack)
    elif kind in ['minecraft:root_system', 'minecraft:random_patch', 'minecraft:flower', 'minecraft:no_bonemeal_flower']:
        placement(config['feature'], path + ['nested'], top, stack)
    else:
        # The remaining reviewed codecs have no nested placement fields. Detect
        # unfamiliar inline content; new codecs with named children need source review.
        reject_unhandled_placement(config, kind)


def reject_unhandled_placement(value, kind):
    if isinstance(value, dict):
        if 'placement' in value and 'feature' in value:
            raise ValueError('Unhandled inline placed feature ' + kind)
        for child in value.values():
            reject_unhandled_placement(child, kind)
    elif isinstance(value, list):
        for child in value:
            reject_unhandled_placement(child, kind)


for top in data['topFeatures']:
    placement(top, [], top)
result = {
    'method': 'Typed traversal of Minecraft 1.21.1 selectors, vegetation, patches and root systems; registered and inline children',
    'registeredPlaced': sorted(placed), 'registeredConfigured': sorted(configured),
    'featureTypes': sorted(feature_types), 'placementTypes': sorted(modifiers), 'heightModifiers': heights,
}
args.output.write_text(json.dumps(result, indent=2) + '\n')
print('Registered placed/configured:', len(placed), len(configured))
print('Height paths:', len(heights), 'nested:', sum(row['nested'] for row in heights))
print('Untagged:', [(row['top'], row['path']) for row in heights if not row['tagged']])
