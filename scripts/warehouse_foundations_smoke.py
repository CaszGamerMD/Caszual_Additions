#!/usr/bin/env python3
"""Fail CI when warehouse 3D models, directional blockstates or contracts regress.

This tests static assets/source invariants only; real Minecraft gameplay and
rendering still require manual client/server integration testing.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources/assets/caszual_additions"

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

def blockstate(name):
    return json.loads((RES / "blockstates" / (name + ".json")).read_text())

def model(name):
    return json.loads((RES / "models/block" / (name + ".json")).read_text())

def assert_oriented(name, parts=None):
    variants=blockstate(name)["variants"]
    directions=("north","east","south","west")
    for facing in directions:
        for part in range(parts + 1) if parts is not None else (None,):
            key=f"part={part},facing={facing}" if part is not None else f"facing={facing}"
            assert key in variants, f"{name}: missing {key}"
            entry=variants[key]
            assert entry.get("y") in (0,90,180,270), f"{name}: invalid yaw for {key}"
            assert entry["model"] == f"caszual_additions:block/{name}"
    assert len(variants)==len(directions)*(parts+1 if parts is not None else 1)

for name in ("ptp_wrap_unit","ptp_hopper_adapter","pallet_transfer_block"):
    assert_oriented(name,parts=9)
for name in ("rack_computer","storage_golem_shelf","rack_connector"):
    assert_oriented(name)

for name in ("rack_computer","storage_golem_shelf","rack_connector",
             "ptp_wrap_unit","ptp_hopper_adapter"):
    m=model(name)
    assert m.get("elements"),f"{name}: missing physical geometry"
    for element in m["elements"]:
        frm=element["from"]
        to=element["to"]
        assert all(a<b for a,b in zip(frm,to)),f"{name}: degenerate cuboid"

assert len(model("rack_computer")["elements"])>=6, "Kiosk has no monitor/console detail"
assert len(model("rack_connector")["elements"])>=2, "Connector has no crossbeams"
assert "poses.scale(.875f,.875f,.875f)" in read(
    "src/client/java/com/caszgamermd/caszualadditions/racks/RackBlockEntityRenderer.java")
assert "private static final double SCALE=.875" in read(
    "src/main/java/com/caszgamermd/caszualadditions/racks/RackCargoCollision.java")
network=read("src/main/java/com/caszgamermd/caszualadditions/racks/RackNetwork.java")
assert "MAX_CONNECTOR_GAP=4" in network and "backToBack" in network
net=read("src/main/java/com/caszgamermd/caszualadditions/racks/RackComputerNet.java")
assert 'root.add("ptps",transferPoints)' in net
assert "PtpControl" in net
assert "ParticleTypes.END_ROD" not in net
assert "highlights.add(position)" in net
print("Warehouse static models, orientation, fitted collision, links and control contracts: PASS")
