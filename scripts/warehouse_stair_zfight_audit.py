#!/usr/bin/env python3
"""Block CI if any assembled Rack Stairs face overlaps a coplanar face.

Unlike the generic single-model audit, this checks rotated elements, every
multipart stair section, all four directions, both rails and all upper docks.
All connections visible at once is a conservative superset of legal layouts.
Uses only the standard library so GitHub Actions needs no extra packages.
"""
import json
import math
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/caszual_additions"
MODELS = ROOT / "models/block"
MIN_OVERLAP = 0.05  # model pixels squared
# Normal and the four 0/1 corners of each cube face.
FACES = {
    "north": ((0, 0, -1), ((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0))),
    "south": ((0, 0, 1), ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1))),
    "east": ((1, 0, 0), ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0))),
    "west": ((-1, 0, 0), ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0))),
    "up": ((0, 1, 0), ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1))),
    "down": ((0, -1, 0), ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1))),
}


def rotate(p, axis, degrees):
    co, si = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    x, y, z = p
    if axis == "x":
        return (x, y * co - z * si, y * si + z * co)
    if axis == "y":
        return (x * co + z * si, y, -x * si + z * co)
    if axis == "z":
        return (x * co - y * si, x * si + y * co, z)
    raise ValueError(axis)


def transform(p, element, yaw, translation):
    if "rotation" in element:
        rot = element["rotation"]
        origin = rot["origin"]
        q = rotate(tuple(p[i] - origin[i] for i in range(3)),
                   rot["axis"], rot["angle"])
        p = tuple(q[i] + origin[i] for i in range(3))
    q = rotate(tuple(p[i] - 8 for i in range(3)), "y", yaw)
    return tuple(q[i] + 8 + translation[i] for i in range(3))


def signed_area(poly):
    return sum(p[0] * q[1] - p[1] * q[0]
               for p, q in zip(poly, poly[1:] + poly[:1])) / 2 if poly else 0


def cross(a, b, p):
    return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])


def intersection_area(left, right):
    """Convex polygon clipping (Sutherland-Hodgman), no Shapely dependency."""
    left, right = list(left), list(right)
    if signed_area(left) < 0:
        left.reverse()
    if signed_area(right) < 0:
        right.reverse()
    for a, b in zip(right, right[1:] + right[:1]):
        if not left:
            return 0
        clipped = []
        prev = left[-1]
        old = cross(a, b, prev)
        for point in left:
            new = cross(a, b, point)
            if (old >= -1e-8) != (new >= -1e-8):
                t = old / (old - new)
                clipped.append(tuple(prev[k] + t * (point[k] - prev[k])
                                     for k in range(2)))
            if new >= -1e-8:
                clipped.append(point)
            prev, old = point, new
        left = clipped
    return abs(signed_area(left))


def inspect(facing, state):
    groups = defaultdict(list)
    for part in range(9):
        run, height = part % 3, part // 3
        move = {"north": (0, height * 16, -run * 16),
                "south": (0, height * 16, run * 16),
                "east": (run * 16, height * 16, 0),
                "west": (-run * 16, height * 16, 0)}[facing]
        conditions = {"part": str(part), "facing": facing,
                      "rail_left": "true", "rail_right": "true",
                      **{f"dock_{d}": "true" for d in
                         ("north", "south", "east", "west")}}
        for part_model in state["multipart"]:
            if any(conditions.get(k) != v for k, v in
                   part_model.get("when", {}).items()):
                continue
            apply = part_model["apply"]
            model_name = apply["model"].split("/")[-1]
            model = json.loads((MODELS / f"{model_name}.json").read_text())
            for idx, e in enumerate(model.get("elements", [])):
                for face in e.get("faces", {}):
                    normal, template = FACES[face]
                    a, b = e["from"], e["to"]
                    corners = [tuple(a[i] + p[i] * (b[i] - a[i])
                                     for i in range(3)) for p in template]
                    vertices = [transform(p, e, apply.get("y", 0), move)
                                for p in corners]
                    if "rotation" in e:
                        r = e["rotation"]
                        normal = rotate(normal, r["axis"], r["angle"])
                    normal = rotate(normal, "y", apply.get("y", 0))
                    axis = max(range(3), key=lambda i: abs(normal[i]))
                    xy = [tuple(p[i] for i in range(3) if i != axis)
                          for p in vertices]
                    if abs(signed_area(xy)) < 1e-8:
                        continue
                    plane = round(sum(normal[i] * vertices[0][i]
                                      for i in range(3)), 4)
                    key = (tuple(round(n, 5) for n in normal), plane)
                    groups[key].append((xy, (part, model_name, idx, face)))
    failures = []
    count = 0
    for plane, faces in groups.items():
        count += len(faces)
        for i, (a, label) in enumerate(faces):
            for b, other in faces[i + 1:]:
                area = intersection_area(a, b)
                if area > MIN_OVERLAP:
                    failures.append((round(area, 3), plane, label, other))
    return count, failures


def main():
    state = json.loads((ROOT / "blockstates/rack_stairs.json").read_text())
    total = 0
    errors = []
    for facing in ("north", "east", "south", "west"):
        face_count, overlaps = inspect(facing, state)
        total += face_count
        print(f"{facing}: {face_count} assembled faces, "
              f"{len(overlaps)} overlapping coplanar pairs")
        errors.extend((facing, *entry) for entry in overlaps)
    for facing, area, plane, first, second in errors[:30]:
        print(f"  {facing} {area} px² {first} vs {second} @ {plane}")
    print(f"Rack Stairs rotated/multipart face audit: {total} faces, "
          f"{len(errors)} coplanar overlaps")
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
