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
for name in ("rack_computer","storage_golem_shelf"):
    assert_oriented(name)
connector_variants=blockstate("rack_connector")["variants"]
connector_models={
    "rack_connector",
    "rack_connector_east_corner",
    "rack_connector_x",
    "rack_connector_x_east_corner"
}
for facing in ("north","south","east","west"):
    for positive in ("false","true"):
        key=f"facing={facing},corner_positive={positive}"
        assert key in connector_variants,f"missing connector corner variant {key}"
        assert connector_variants[key]["model"].split(":block/")[1] in connector_models
assert len(connector_variants)==8

for name in ("rack_computer","storage_golem_shelf","rack_connector",
             "ptp_wrap_unit","ptp_hopper_adapter"):
    m=model(name)
    assert m.get("elements"),f"{name}: missing physical geometry"
    for element in m["elements"]:
        frm=element["from"]
        to=element["to"]
        assert all(a<b for a,b in zip(frm,to)),f"{name}: degenerate cuboid"

assert len(model("rack_computer")["elements"])>=6, "Kiosk has no monitor/console detail"
# Each connector is ONE 2px upper corner rod, never a giant X or two
# middle rods; axis/corner models must match the BlockState variants.
for name in connector_models:
    rods=model(name)["elements"]
    assert len(rods)==1,f"{name}: expected a single rod"
    rod=rods[0]
    dimensions=[b-a for a,b in zip(rod["from"],rod["to"])]
    assert sorted(dimensions)==[2,2,16],f"{name}: not a 2x2x16 rod"
    assert rod["from"][1]==14 and rod["to"][1]==16,"Rod should align with rack top"
    assert "rotation" not in rod,"No diagonal X-brace rotation"
connector=read("src/main/java/com/caszgamermd/caszualadditions/racks/RackConnectorBlock.java")
connector_item=read("src/main/java/com/caszgamermd/caszualadditions/racks/RackConnectorItem.java")
assert "CORNER_POSITIVE" in connector and "MAX_CONNECTOR_GAP" in connector
assert "getClickLocation()" in connector_item and "root.getY()+2" in connector_item
assert "reservedForAccess" not in connector,"Face-to-face connectors must span rack approach lanes"
connections=read("src/main/java/com/caszgamermd/caszualadditions/racks/WarehouseConnections.java")
assert "WarehouseNetwork.fromComputer" in connections
assert "WarehouseNetwork.fromShelfGroup" in connections
computer_block=read("src/main/java/com/caszgamermd/caszualadditions/racks/RackComputerBlock.java")
assert "StorageScannerItem.onComputerClick" in computer_block
scanner=read("src/main/java/com/caszgamermd/caszualadditions/racks/StorageScannerItem.java")
assert scanner.index("bind(scanner,level,computer);") < scanner.index("StoragePalletCrew.enlist(level,player,computer)")
scanner=read("src/main/java/com/caszgamermd/caszualadditions/racks/StorageScannerItem.java")
assert "extends Item" in scanner and "StoragePalletCrew" in scanner
assert not (ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks/StorageScannerBlock.java").exists()
assert not (ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks/OrderHubBlock.java").exists()
assert not (ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks/OrderHubEntity.java").exists()
assert "Blocks.GLASS.defaultBlockState()" in read(
    "src/client/java/com/caszgamermd/caszualadditions/racks/PtpRenderer.java")
assert "GlassPalletWrap.withWrap" not in scanner # Scanner owns no pallet inventory

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
print("Warehouse thin rods, handheld scanner, removed hubs, glass wrap and network contracts: PASS")
