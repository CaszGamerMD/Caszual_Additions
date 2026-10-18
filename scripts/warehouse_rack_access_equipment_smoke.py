#!/usr/bin/env python3
"""Rack access equipment: physical stair escort, ladder and guardrail scaffold.

This protects key logic and assets; collision and animation in a running
Minecraft world still require manual verification.
"""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions"
res=root/"src/main/resources"
racks=src/"racks"
read=lambda n:(racks/n).read_text()
stairs=read("RackStairsBlock.java")
stairbe=read("RackStairsBlockEntity.java")
ladder=read("RackLadderBlock.java")
scaff=read("RackScaffoldingBlock.java")
access=read("WarehouseRackAccess.java")
golem=read("StorageGolem.java")
rack=read("RackBlockEntity.java")
plan=read("RackPickupPlan.java")
registration=read("RackContent.java")
creative=(src/"CaszualItemGroups.java").read_text()
assert "RackStairsBlock RACK_STAIRS" in registration
assert "RackLadderBlock RACK_LADDER" in registration
assert "RackScaffoldingBlock RACK_SCAFFOLDING" in registration
assert "WarehouseRackAccess.initialize();" in registration
for name in ["RACK_STAIRS_ITEM","RACK_LADDER_ITEM","RACK_SCAFFOLDING_ITEM"]:
    assert name in creative

# Mobile multi-block staircase, half-steps, guardrail, remembered home,
# controlled player push, and occupancy-safe atomic movement.
assert "IntegerProperty.create(\"part\",0,8)" in stairs
assert "for(int i=1;i<9;i++)" in stairs
assert "box(0,0,8,16,8,16)" in stairs
assert "box(0,0,0,16,16,8)" in stairs
assert "box(0,0,0,1,16,16)" in stairs
assert "public static boolean occupied(" in stairs
assert "if(occupied(world,oldRoot,face))return false;" in stairs
assert "getWorldBorder().isWithinBounds(cell)" in stairs
assert "moved.restore(snapshot,byPlayer?to:snapshot.home());" in stairs
assert "current.relative(player.getDirection())" in stairs
assert "be.setHome(be.getBlockPos())" in stairs
assert "out.store(\"rack_stairs_home\"" in stairbe
assert "out.store(\"rack_stairs_target\"" in stairbe
assert "be.mover" in stairbe and "escort.isAlive()" in stairbe
assert "nextMove=level.getGameTime()+16" in stairbe
assert "be.returning" in stairbe and "be.requested=false" in stairbe
assert "RackStairsBlock.occupied(level,root," in stairbe
assert "WarehouseRackAccess.findAssignedStairs(" in golem
assert "worker.assignRackStairMover(" in access
assert "stairs.assign(desired,worker.getUUID())" in access
assert "shelf.takeNamed()" in access
assert "WarehouseRackAccess.requestStairs(level,elevated)" in golem
assert "WarehouseRackAccess.finishStairAccess" in golem

# Side climbing and dismount are vanilla ladder rules.
assert "extends LadderBlock" in ladder
tag=json.loads((res/"data/minecraft/tags/block/climbable.json").read_text())
assert "caszual_additions:rack_ladder" in tag["values"]

# Platform with exposed-side guardrails, connected smooth walkway,
# pallet-facing access, and safe folding/re-extension.
assert "public static final BooleanProperty FOLDED" in scaff
assert "Shapes.or(result,NORTH)" in scaff
assert "Shapes.or(result,SOUTH)" in scaff
assert "Shapes.or(result,WEST)" in scaff
assert "Shapes.or(result,EAST)" in scaff
assert "facing!=Direction." in scaff
assert "instanceof RackScaffoldingBlock" in scaff
assert "if(occupied(world,pos))return false;" in scaff
assert "state.getValue(FOLDED)?Shapes.empty()" in scaff
assert "RackScaffoldingBlock.setFolded(level,at,true)" in access
assert "RackScaffoldingBlock.setFolded(level,at,false)" in access
assert "WarehouseRackAccess.prepare(level,rack)" in plan
assert "WarehouseRackAccess.prepare(" in rack
assert "WarehouseRackAccess.release(world,this)" in rack
assert "WarehouseRackAccess.clearOfStairs" in plan
assert "WarehouseRackAccess.clearOfStairs" in rack

for name in ("rack_stairs","rack_ladder","rack_scaffolding"):
    for path in (
        f"assets/caszual_additions/blockstates/{name}.json",
        f"assets/caszual_additions/models/item/{name}.json",
        f"assets/caszual_additions/items/{name}.json",
        f"data/caszual_additions/recipe/{name}.json"):
        assert (res/path).exists(),path
assert all((res/f"assets/caszual_additions/models/block/rack_stairs_{i}.json").exists()
           for i in range(9))
openmodel=json.loads((res/"assets/caszual_additions/models/block/rack_scaffolding_open.json").read_text())
folded=json.loads((res/"assets/caszual_additions/models/block/rack_scaffolding_folded.json").read_text())
assert len(openmodel["elements"])>len(folded["elements"])
assert len(openmodel["elements"])>=10
state=json.loads((res/"assets/caszual_additions/blockstates/rack_scaffolding.json").read_text())
assert any("folded=true" in key for key in state["variants"])
assert any("folded=false" in key for key in state["variants"])
name=json.loads((res/"assets/caszual_additions/lang/en_us.json").read_text())
assert all("block.caszual_additions."+s in name for s in
           ("rack_stairs","rack_ladder","rack_scaffolding"))
print("PASS: guardrailed retractable walkway; climbable steel ladder; movable saved-home stairs with assigned escort")
