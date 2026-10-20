#!/usr/bin/env python3
"""Placement and owner-death golem follow regression for warehouse equipment."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
RACKS=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"
ASSETS=ROOT/"src/main/resources/assets/caszual_additions"
read=lambda name:(RACKS/name).read_text()
sc=read("RackScaffoldingBlock.java")
golem=read("StorageGolem.java")
rig=read("MovingPallet.java")
filter_item=json.loads((ASSETS/"models/item/pallet_filter.json").read_text())
sc_state=json.loads((ASSETS/"blockstates/rack_scaffolding.json").read_text())
assert 'context.getClickedFace()' in sc
assert 'clicked.getOpposite()' in sc
assert 'clicked.getAxis().isHorizontal()' in sc
assert 'context.getHorizontalDirection()' in sc
assert 'STOWED_NORTH' in sc and 'STOWED_SOUTH' in sc
assert 'STOWED_EAST' in sc and 'STOWED_WEST' in sc
assert 'case NORTH -> STOWED_NORTH' in sc
assert 'case SOUTH -> STOWED_SOUTH' in sc
assert 'case WEST -> STOWED_WEST' in sc
assert 'case EAST -> STOWED_EAST' in sc
assert {(p.get("when",{}).get("facing")) for p in sc_state["multipart"]
        if p.get("when",{}).get("folded")=="true"} == {"north","east","south","west"}
display=filter_item["display"]
assert display["firstperson_righthand"]["rotation"][1] == 140
assert display["thirdperson_righthand"]["rotation"][1] == 120
assert "firstperson_lefthand" in display
assert "thirdperson_lefthand" in display
assert len(filter_item["elements"])>=8
for src in (golem,rig):
    assert "waitingForPlayer" in src
    assert 'out.putBoolean("waiting_for_player",waitingForPlayer);' in src
    assert 'waitingForPlayer=in.getBooleanOr("waiting_for_player",false);' in src
    assert 'player==null||!player.isAlive()||player.isSpectator()' in src
    assert 'waitingForPlayer=true;' in src
    assert 'player.distanceToSqr(getX(),getY(),getZ())>16.0*16.0' in src
    assert 'getNavigation().stop();' in src
    assert 'setDeltaMovement(0,getDeltaMovement().y,0);' in src
    assert 'waitingForPlayer=false;' in src
assert 'floorFollowRoute.clear();' in rig
assert 'nextFloorFollowPlan=0;' in rig
assert 'if(isDelivering()){tickDelivery(server);return;}' in rig
assert 'StoragePalletCrew.formMovingPallet(' in golem
print("PASS: clipboard reading orientation, clicked-face scaffold cassette, owner-death 16-block reunion for both crew modes")
