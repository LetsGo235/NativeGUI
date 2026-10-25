#!/usr/bin/env python3
"""Validate test-pack structure/assets with only Python's standard library.
Run: python tools/validate_release_gate_pack.py
"""
from pathlib import Path
import json
import struct

ROOT = Path(__file__).resolve().parents[1] / 'release-gate-pack'
assert (ROOT / 'pack.mcmeta').exists(), 'Missing pack.mcmeta'
meta = json.loads((ROOT / 'pack.mcmeta').read_text(encoding='utf-8'))
assert meta['pack']['pack_format'] == 34, 'Minecraft 1.21.1 expects pack_format 34'
assets = ROOT / 'assets/minecraft'
shulker = assets / 'optifine/gui/container/shulker'
for color in ('purple','red'):
    properties = dict(line.split('=',1) for line in (shulker/f'{color}.properties').read_text().splitlines() if '=' in line)
    assert properties['container'] == 'shulker_box'
    assert properties['colors'] == color
    assert properties['texture'] == color + '.png'
    assert (shulker/properties['texture']).is_file()
modern = json.loads((assets/'optigui/gui/named_chest.json').read_text())
assert modern['match']['@screen']['@title']['@text'] == 'NativeGUI JSON Test'
assert modern['containers'] == 'minecraft:chest'
for value in modern['textures'].values():
    namespace, path = value.split(':',1)
    assert (ROOT/'assets'/namespace/path).is_file(), f'JSON refers to missing texture: {value}'
for image in ROOT.rglob('*.png'):
    raw = image.read_bytes()
    assert raw.startswith(b'\x89PNG\r\n\x1a\n')
    assert raw[12:16] == b'IHDR'
    width, height = struct.unpack('>II',raw[16:24])
    assert width == height == 256, f'Unexpected texture dimensions: {image}'
print('Release-gate resource-pack checks passed (metadata, selectors, references, PNGs).')
