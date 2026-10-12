#!/usr/bin/env python3
"""Confirm Storage Scanner is compact in both hands and has no rear trigger."""
import json
from pathlib import Path
model=json.loads((Path(__file__).resolve().parents[1] /
    "src/main/resources/assets/caszual_additions/models/item/storage_scanner.json").read_text())
assert len(model["elements"])==81
assert all(e["to"][2]<=12 for e in model["elements"]), "Rear trigger protrusion reintroduced"
assert all(e["to"][i]>e["from"][i] for e in model["elements"] for i in range(3))
for name,scale in (
    ("firstperson_righthand",.475),("firstperson_lefthand",.475),
    ("thirdperson_righthand",.35),("thirdperson_lefthand",.35),
):
    assert model["display"][name]["scale"]==[scale]*3,name
assert model["display"]["gui"]["scale"]==[1.12]*3
assert model["display"]["ground"]["scale"]==[.5]*3
print("PASS: compact hand scales, trigger-free scanner, untouched inventory/ground transforms")
