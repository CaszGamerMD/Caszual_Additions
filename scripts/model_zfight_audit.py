"""Static z-fighting audit of Caszual Additions axis-aligned JSON item/block models.

Find faces that occupy the SAME plane, face the SAME direction, and share
nonzero area. Only these produce depth-buffer ambiguity; intersecting model
volumes alone are often intentional and should not be treated as z-fighting.

Rotated model elements, separate variants placed side-by-side, animated entity
models, and runtime/multipart renderers require in-game visual inspection.
This script reports potential issues, never removes geometry automatically.

Usage: python3 scripts/model_zfight_audit.py
"""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "src/main/resources/assets/caszual_additions/models"

# Direction: (coordinate axis, surface ("from" or "to"), in-plane axes).
FACES = {
    "north": (2, "from", (0, 1)),
    "south": (2, "to", (0, 1)),
    "west": (0, "from", (1, 2)),
    "east": (0, "to", (1, 2)),
    "down": (1, "from", (0, 2)),
    "up": (1, "to", (0, 2)),
}
EPSILON = 1e-5
MIN_AREA = 0.05  # pixel^2 in Blockbench's 16-pixels-per-block coordinates


def collisions(elements: list[dict]) -> list[tuple[str, str, str, float]]:
    planes: dict[tuple[str, int], list[tuple[str, tuple[float, ...]]]] = defaultdict(list)
    for index, element in enumerate(elements):
        if "rotation" in element:  # box coordinates are no longer world-space
            continue
        a = element.get("from")
        b = element.get("to")
        if not (isinstance(a, list) and isinstance(b, list)
                and len(a) == len(b) == 3):
            continue
        label = element.get("name", f"element_{index}")
        for direction, (axis, end, (u, v)) in FACES.items():
            if direction not in element.get("faces", {}):
                continue
            location = (a if end == "from" else b)[axis]
            planes[(direction, round(location * 10000))].append(
                (label, (a[u], b[u], a[v], b[v]))
            )

    result = []
    for (face, _), bounds in planes.items():
        for i, (name, a) in enumerate(bounds):
            for other, b in bounds[i + 1:]:
                width = max(0, min(a[1], b[1]) - max(a[0], b[0]))
                height = max(0, min(a[3], b[3]) - max(a[2], b[2]))
                area = width * height
                if area > MIN_AREA + EPSILON:
                    result.append((name, other, face, round(area, 3)))
    return sorted(result, key=lambda x: x[3], reverse=True)


def main() -> None:
    inspected = 0
    suspects = []
    for file in sorted(MODEL_DIR.rglob("*.json")):
        try:
            data = json.loads(file.read_text(encoding="utf-8"))
        except (OSError, ValueError) as error:
            print(f"INVALID MODEL: {file.relative_to(ROOT)}: {error}")
            continue
        elements = data.get("elements")
        if not isinstance(elements, list):
            continue  # vanilla parent-derived geometries cannot be checked here
        inspected += 1
        hits = collisions(elements)
        if hits:
            suspects.append((file, hits))

    print(f"Z-clip static audit: {inspected} models with explicit geometry inspected.")
    if not suspects:
        print("No same-plane, same-direction overlapping model quads detected.")
    for file, hits in sorted(suspects, key=lambda x: (-x[1][0][3], str(x[0]))):
        print(f"\n{file.relative_to(ROOT)}: {len(hits)} candidate overlaps")
        for a, b, face, area in hits[:8]:
            print(f"  {face:5} {a} / {b}: {area} pixel²")
        if len(hits) > 8:
            print(f"  ... {len(hits) - 8} more")
    print(f"\nReview {len(suspects)} model(s) with coplanar faces. "
          "Reported overlaps can be hidden internal geometry; check in-game.")
    # This is an audit, not a hard build failure. Avoid breaking releases
    # because a decorative multiblock intentionally shares a hidden face.


if __name__ == "__main__":
    main()
