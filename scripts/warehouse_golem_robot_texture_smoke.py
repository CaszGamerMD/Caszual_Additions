#!/usr/bin/env python3
"""Verify both Storage Golem models share a gap-free head and worn robot atlas.

No third-party image library is required in CI: decode the 64x64 RGBA PNG
using the standard-library zlib PNG filters.
"""
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ATLAS = ROOT / "src/main/resources/assets/caszual_additions/textures/entity/storage_golem.png"
CLIENT = ROOT / "src/client/java/com/caszgamermd/caszualadditions/racks"


def texture(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "Missing/invalid Golem texture"
    chunks = []
    offset = 8
    width = height = None
    while offset < len(data):
        n = struct.unpack_from(">I", data, offset)[0]
        name = data[offset + 4:offset + 8]
        content = data[offset + 8:offset + 8 + n]
        assert zlib.crc32(name + content) & 0xffffffff == struct.unpack_from(
            ">I", data, offset + 8 + n
        )[0], "PNG chunk CRC mismatch"
        if name == b"IHDR":
            width, height, depth, kind, *_ = struct.unpack(">IIBBBBB", content)
            assert (width, height, depth, kind) == (64, 64, 8, 6), "Expected RGBA 64x64 atlas"
        if name == b"IDAT":
            chunks.append(content)
        offset += 12 + n
        if name == b"IEND":
            break
    assert width == 64 and height == 64 and chunks
    raw = zlib.decompress(b"".join(chunks))
    pixels = []
    previous = bytearray(width * 4)
    pos = 0
    for _ in range(height):
        mode = raw[pos]
        pos += 1
        current = bytearray(width * 4)
        for i in range(width * 4):
            b = raw[pos + i]
            a = current[i - 4] if i >= 4 else 0
            up = previous[i]
            c = previous[i - 4] if i >= 4 else 0
            if mode == 0: predict = 0
            elif mode == 1: predict = a
            elif mode == 2: predict = up
            elif mode == 3: predict = (a + up) // 2
            elif mode == 4:
                p = a + up - c
                d = [abs(p-a), abs(p-up), abs(p-c)]
                predict = (a, up, c)[d.index(min(d))]
            else: raise AssertionError("Unsupported PNG filter")
            current[i] = (b + predict) & 255
        pos += width * 4
        pixels.append([tuple(current[4*x:4*x+4]) for x in range(width)])
        previous = current
    return pixels


image = texture(ATLAS)
def at(x, y): return image[y][x]
def luminosity(x1, x2, y1, y2):
    return sum(sum(at(x, y)[:3])/3 for y in range(y1,y2) for x in range(x1,x2)) / ((x2-x1)*(y2-y1))

# Every UV island of the existing model must remain opaque (no missing skin).
islands = [
    (0,7,7,11),(7,7,10,11),(17,7,7,11),(24,7,10,11),(7,0,10,7),(17,0,10,7),
    (0,26,6,7),(6,26,8,7),(14,26,6,7),(20,26,8,7),(6,20,8,6),(14,20,8,6),
    (28,24,4,10),(32,24,4,10),(36,24,4,10),(40,24,4,10),(32,20,4,4),(36,20,4,4),
    (44,4,4,8),(48,4,4,8),(52,4,4,8),(56,4,4,8),(48,0,4,4),(52,0,4,4)
]
for x,y,w,h in islands:
    assert all(at(u,v)[3]==255 for v in range(y,y+h) for u in range(x,x+w)),(x,y)

# Industrial robot cues: paired bright cyan optics, slate panels,
# gloves and boots intentionally darker from embedded oil and soil.
for x in (8,11):
    r,g,b,_ = at(x,29)
    assert g > r*1.5 and b > r*1.4, "Robot eyes not cyan"
assert luminosity(32,36,31,34) < luminosity(32,36,25,28)*.77, "Gloves not dirty"
assert luminosity(48,52,10,12) < luminosity(48,52,5,8)*.8, "Boots not muddy"

for filename in ("StorageGolemModel.java", "MovingPalletModel.java"):
    content = (CLIENT / filename).read_text()
    assert '.texOffs(0,20)' in content, "Head UV changed"
    assert "PartPose.offset(0,6,0)" in content, f"Floating head in {filename}"
    assert 'PartPose.offset(0,13,0)' in content, "Torso reference changed"
for filename in ("StorageGolemRenderer.java", "MovingPalletRenderer.java"):
    assert 'textures/entity/storage_golem.png' in (CLIENT / filename).read_text()

print("PASS: both Golem heads touch bodies; unified slate robot texture includes dark work gloves and muddy boots")
