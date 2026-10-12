#!/usr/bin/env python3
"""Static checks for free-standing pallet interaction and open-front rack frames.

The world/client interactions and true collision feel still need in-game QA.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"src/main/resources/assets/caszual_additions"
JAVA=ROOT/"src/main/java/com/caszgamermd/caszualadditions"

def read(path):
    return (JAVA/path).read_text(encoding="utf-8")
def model(part,tier):
    return json.loads((ASSETS/"models/block"/f"pallet_rack_segment_{part}_{tier}.json").read_text())
def front_rails(elements):
    return [e for e in elements
            if e["from"][2]==0 and e["to"][2]==1
            and e["to"][0]-e["from"][0]>=14
            and (e["from"][1] in (0,15))]
def grill(elements):
    return [e for e in elements
            if e["from"][1]==1 and e["to"][1]==2
            and e["to"][2]-e["from"][2]==16
            and e["to"][0]-e["from"][0]==1]
def crosses(elements):
    return [e for e in elements
            if e["from"][1]==1 and e["to"][1]==2
            and e["from"][0]==0 and e["to"][0]==16
            and e["to"][2]-e["from"][2]==1]
variants=json.loads((ASSETS/"blockstates/pallet_rack.json").read_text())["variants"]
for facing in ("north","east","south","west"):
    for part in range(12):
        key=f"facing={facing},part={part}"
        assert key in variants,key
        tier=("lower","middle","upper")[part//4]
        local=part%4
        expected=(f"pallet_rack_segment_{local}_{tier}"
                  if tier=="lower" or local<2 else f"pallet_rack_segment_{local}")
        assert variants[key]["model"]=="caszual_additions:block/"+expected,key

for local in (0,1):
    for tier,count in (("lower",1),("middle",0),("upper",1)):
        elements=model(local,tier)["elements"]
        assert len(front_rails(elements))==count,(local,tier)
        if tier=="lower":
            assert all(e["from"][1]==0 for e in front_rails(elements))
        if tier=="upper":
            assert all(e["from"][1]==15 for e in front_rails(elements))
        assert (len(grill(elements))==3)==(tier=="lower"),(local,tier)
        assert (len(crosses(elements))==2)==(tier=="lower"),(local,tier)
for local in (2,3):
    elements=model(local,"lower")["elements"]
    assert len(grill(elements))==3
    assert len(crosses(elements))==2

pallet=read("pallet/PalletBlock.java")
assert "protected InteractionResult useItemOn(" in pallet
assert "return openPallet(level,pos,state,player);" in pallet
assert "protected InteractionResult useWithoutItem(" in pallet
assert "serverPlayer.openMenu(controller);" in pallet
assert "rootPos(pos, state)" in pallet
entity=read("pallet/PalletBlockEntity.java")
assert "for(int part=0;part<4;part++)" in entity
assert "PalletBlock.partPos(root.getBlockPos(),part,state)" in entity
rack=read("racks/RackBlock.java")
assert "RackFrameShapes.cell(state)" in rack
assert "RackCargoCollision.shape(level,pos,state)" in rack
assert "getShape(" in rack
assert "getCollisionShape(" in rack
assert "for (int i = 1; i < 12; i++)" in rack
assert "Containers.dropContents(level, root, rack)" in rack
assert "return List.of();" in rack  # Only the root drops one rack item
frame=read("racks/RackFrameShapes.java")
assert "double[][][] ELEMENTS" in frame
assert "CELLS[orientation][part]=combined.optimize()" in frame
assert "Block.box(a,y1,b,c,y2,d)" in frame
print("PASS: single-rack selection; precise 12-part collision; continuous grill + cross beams")
