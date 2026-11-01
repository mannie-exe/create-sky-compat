#!/usr/bin/env python3
"""Generate English Ponder keys and a bounded workbench for authored scenes."""
import argparse
import gzip
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def outputs():
    source = (ROOT / 'src/main/java/design/inherent/createskycompat/linear/client/LinearScenes.java').read_text(encoding="utf-8")
    lang = ROOT / 'src/main/resources/assets/create_sky_compat/lang/en_us.json'
    strings = {k: v for k, v in json.loads(lang.read_text(encoding="utf-8")).items()
               if not k.startswith('create_sky_compat.ponder.linear_')}
    for match in re.finditer(r'public static void (\w+)\(.*?\n    }', source, re.S):
        body = match.group()
        title = re.search(r'start\(builder, util, "(\w+)", ("[^"]*")\)', body)
        if not title:
            raise ValueError(f'Scene has no title: {match[1]}')
        prefix = f'create_sky_compat.ponder.linear_{title[1]}'
        strings[prefix + '.header'] = json.loads(title[2])
        for index, text in enumerate(re.findall(r'explain\(scene, ("[^"]*")', body), 1):
            strings[f'{prefix}.text_{index}'] = json.loads(text)
    # Ponder derives editable bounds from placed blocks, not StructureTemplate.size.
    # Two anchors admit all scene instructions; the hidden upper anchor is cleared first.
    def named(kind, name, payload):
        b = name.encode()
        return bytes([kind]) + struct.pack('>H', len(b)) + b + payload
    def integer(value):
        return struct.pack('>i', value)
    nbt = b'\x0a\x00\x00' + named(3, 'DataVersion', integer(3955))
    nbt += named(9, 'size', b'\x03' + integer(3) + b''.join(integer(v) for v in [7, 6, 7]))
    name = b'minecraft:smooth_stone'
    palette = named(8, 'Name', struct.pack('>H', len(name)) + name) + b'\x00'
    nbt += named(9, 'palette', b'\x0a' + integer(1) + palette)
    blocks = b''
    for position in [[0, 0, 0], [6, 5, 6]]:
        blocks += named(9, 'pos', b'\x03' + integer(3) + b''.join(integer(v) for v in position))
        blocks += named(3, 'state', integer(0)) + b'\x00'
    nbt += named(9, 'blocks', b'\x0a' + integer(2) + blocks)
    nbt += named(9, 'entities', b'\x0a' + integer(0))
    nbt += b'\x00'
    structure = ROOT / 'src/main/resources/assets/create_sky_compat/ponder/linear/workbench.nbt'
    return {lang: (json.dumps(strings, ensure_ascii=False, indent=2) + '\n').encode(),
            structure: gzip.compress(nbt, compresslevel=9, mtime=0)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    for path, data in outputs().items():
        if path.exists():
            current = path.read_bytes()
            # The committed gzip is the distributed artifact. Python/zlib versions
            # may encode the same authored NBT differently; verify the full content.
            if (gzip.decompress(current) == gzip.decompress(data) if path.suffix == '.nbt' else current == data):
                continue
        if args.check:
            raise SystemExit(f'Stale Ponder resource: {path.relative_to(ROOT)}')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print('Ponder resources verified' if args.check else 'Ponder resources generated')


if __name__ == '__main__':
    main()
