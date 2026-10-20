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
assert "box(0,3,0,1,16,16)" in stairs
assert "public static boolean occupied(" in stairs
assert "occupied(world,oldRoot,face)||occupied(world,to,face)" in stairs
assert "stair.destination().equals(desired)" in access
assert "getWorldBorder().isWithinBounds(cell)" in stairs
assert "moved.restore(snapshot,byPlayer?to:snapshot.home());" in stairs
assert "current.relative(player.getDirection())" in stairs
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
assert "instanceof RackBlock" in ladder
assert "super.canSurvive(state,level,pos)" in ladder
tag=json.loads((res/"data/minecraft/tags/block/climbable.json").read_text())
assert "caszual_additions:rack_ladder" in tag["values"]

# Scaffold rails are real multipart models with matching collision; the ladder
# immediately below makes a walk-through hatch instead of a solid floor.
assert "public static final BooleanProperty FOLDED" in scaff
assert "BooleanProperty RAIL_NORTH" in scaff
assert "BooleanProperty RAIL_SOUTH" in scaff
assert "BooleanProperty RAIL_WEST" in scaff
assert "BooleanProperty RAIL_EAST" in scaff
assert "BooleanProperty LADDER_HATCH" in scaff
assert "instanceof RackScaffoldingBlock" in scaff
assert "instanceof RackBlock" in scaff
assert "instanceof LadderBlock" in scaff
assert "updateShape(" in scaff
assert "Shapes.or(result, NORTH)" in scaff
assert "Shapes.or(result, SOUTH)" in scaff
assert "Shapes.or(result, WEST)" in scaff
assert "Shapes.or(result, EAST)" in scaff
assert "if (occupied(world, pos)) return false;" in scaff
assert "Shapes.empty() : shape(state)" in scaff
# Rail connections dynamically open on connected sides: no invisible rail collisions
# or clipping when a staircase, another scaffold, or a rack meets the walkway.
assert "instanceof RackStairsBlock" in scaff
assert "getValue(RackStairsBlock.PART) % 3" in scaff
assert "hasSideConnection(" in stairs
assert "instanceof RackScaffoldingBlock" in stairs
assert "instanceof RackBlock" in stairs
assert "BooleanProperty RAIL_LEFT" in stairs
assert "BooleanProperty RAIL_RIGHT" in stairs
assert "if(state.getValue(RAIL_LEFT))" in stairs
assert "if(state.getValue(RAIL_RIGHT))" in stairs
assert "withRails(original.setValue(FACING,turned)" in stairs
assert "withRails(first.setValue(PART,i)" in stairs
assert "updateShape(BlockState state" in stairs
stair_parts=json.loads((res/"assets/caszual_additions/blockstates/rack_stairs.json").read_text())["multipart"]
assert len(stair_parts)==64, len(stair_parts)
for i in range(9):
    for face in ("north","south","east","west"):
        assert any(p.get("when",{})=={"part":str(i),"facing":face}
                   for p in stair_parts)
for part in (0,4,8):
    for side,edge in (("rail_left","west"),("rail_right","east")):
        assert any(p.get("when",{}).get("part")==str(part)
                   and p.get("when",{}).get(side)=="true"
                   and f"rack_stairs_slope_{edge}" in p["apply"]["model"]
                   for p in stair_parts)
for i in (0,4,8):
    base=json.loads((res/f"assets/caszual_additions/models/block/rack_stairs_{i}.json").read_text())
    assert all(e["from"][0]>0 and e["to"][0]<16 for e in base["elements"])
for edge in ("west","east"):
    assert (res/f"assets/caszual_additions/models/block/rack_stairs_support_{edge}.json").exists()
# Stair-top docking looks one block UP in all four cardinal directions.
assert "at.above().relative(side)" in stairs
assert "pos.relative(side).below()" in scaff
assert "getValue(RackStairsBlock.PART)==8" in scaff
assert "scheduleElevatedStairRefresh(level,pos)" in scaff
assert "notifyTopDockChange(world,cell(oldRoot,face,8))" in stairs
assert "notifyTopDockChange(world,cell(to,face,8))" in stairs
assert "notifyTopDockChange(level,cell(pos,state.getValue(FACING),8))" in stairs
for edge in ("north","south","east","west"):
    assert f'DOCK_{edge.upper()}=BooleanProperty.create("dock_{edge}")' in stairs
    assert any(p.get("when",{}).get("part")=="8"
               and p.get("when",{}).get("dock_"+edge)=="true"
               and p["apply"]["model"].endswith(f"rack_stairs_dock_{edge}")
               for p in stair_parts)
    dock=json.loads((res/f"assets/caszual_additions/models/block/rack_stairs_dock_{edge}.json").read_text())
    assert dock["elements"]

# Structural supports make the 3-level stair a unified steel frame.
for part in (1,2,5):
    support=json.loads((res/f"assets/caszual_additions/models/block/rack_stairs_{part}.json").read_text())
    assert len(support["elements"])>=10
for edge in ("west","east"):
    slope=json.loads((res/f"assets/caszual_additions/models/block/rack_stairs_slope_{edge}.json").read_text())
    assert any(e.get("rotation",{}).get("angle")==45 for e in slope["elements"])
for part in (0,4,8):
    tread=json.loads((res/f"assets/caszual_additions/models/block/rack_stairs_{part}.json").read_text())
    assert len(tread["elements"])>=10
assert "RackScaffoldingBlock.setFolded(level,at,true)" in access
assert "RackScaffoldingBlock.setFolded(level,at,false)" in access
assert "WarehouseRackAccess.prepare(level,rack)" in plan
assert "WarehouseRackAccess.release(level,rack)" in plan
assert "WarehouseRackAccess.prepare(" in rack
assert "WarehouseRackAccess.release(world,this)" in rack
assert "WarehouseRackAccess.clearOfStairs" in plan
assert "WarehouseRackAccess.clearOfStairs" in rack
assert "for(int h=-RackPickupPlan.FORKLIFT_MAX_HEIGHT;h<=2;h++)" in access

# A single Stair item, single 3x1x3 outline, and safely rotated 9-part rig.
assert "IntegerProperty.create(\"part\",0,8)" in stairs
assert "for(int i=1;i<9;i++)" in stairs
assert "box(0,0,8,16,8,16)" in stairs
assert "box(0,0,0,16,16,8)" in stairs
assert "box(0,3,0,1,16,16)" in stairs
assert "assemblyOutline(" in stairs
assert "return assemblyOutline(pos,state);" in stairs
assert "public static boolean rotate(" in stairs
assert "Direction turned=face.getClockWise();" in stairs
assert "before.reserved()" in stairs
assert "occupied(world,origin,turned)" in stairs
assert "validAssembly(world,origin,face)" in stairs
assert "moved.restore(snapshot,origin)" in stairs
assert "if(player.isShiftKeyDown())" in stairs
assert "if(!rotate(server,be.getBlockPos()))" in stairs
assert "current.relative(player.getDirection())" in stairs
assert "moved.restore(snapshot,byPlayer?to:snapshot.home());" in stairs

for name in ("rack_stairs","rack_ladder","rack_scaffolding"):
    for path in (
        f"assets/caszual_additions/blockstates/{name}.json",
        f"assets/caszual_additions/models/item/{name}.json",
        f"assets/caszual_additions/items/{name}.json",
        f"data/caszual_additions/recipe/{name}.json"):
        assert (res/path).exists(),path
assert all((res/f"assets/caszual_additions/models/block/rack_stairs_{i}.json").exists()
           for i in range(9))
folded=json.loads((res/"assets/caszual_additions/models/block/rack_scaffolding_folded.json").read_text())
assert folded["elements"]
state=json.loads((res/"assets/caszual_additions/blockstates/rack_scaffolding.json").read_text())
parts=state["multipart"]
assert any(p.get("when",{}).get("folded")=="true" for p in parts)
assert any(p.get("when",{}).get("ladder_hatch")=="true" for p in parts)
assert any(p.get("when",{}).get("ladder_hatch")=="false" for p in parts)
for edge in ("north","south","east","west"):
    assert any(p.get("when",{}).get("rail_"+edge)=="true" for p in parts)
    rail=json.loads((res/f"assets/caszual_additions/models/block/rack_scaffolding_rail_{edge}.json").read_text())
    assert len(rail["elements"])>=4
hatch=json.loads((res/"assets/caszual_additions/models/block/rack_scaffolding_hatch.json").read_text())
assert len(hatch["elements"])==4
assert (res/"assets/caszual_additions/models/block/rack_scaffolding_deck.json").exists()
name=json.loads((res/"assets/caszual_additions/lang/en_us.json").read_text())
assert all("block.caszual_additions."+s in name for s in
           ("rack_stairs","rack_ladder","rack_scaffolding"))
print("PASS: guardrailed retractable walkway; climbable steel ladder; movable saved-home stairs with assigned escort")
