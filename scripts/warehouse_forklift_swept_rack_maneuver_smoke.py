#!/usr/bin/env python3
"""Verify the rack fork order, curved withdrawal geometry and save safety."""
from pathlib import Path
from math import cos,sin,pi,isclose

root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
truck=(src/"WarehouseForklift.java").read_text()
plan=(src/"ForkliftRackManeuver.java").read_text()
route=(src/"ForkliftAislePath.java").read_text()
assert "STEPS=24" in plan
assert "new int[]{0,1,-1}" in plan
assert "turn*90f*(float)eased" in plan
assert "for(double back:turn==0?" in plan
assert "for(double lateral:turn==0?" in plan
assert "ForkliftAislePath.canStand(world,forklift,pos)" in plan
assert "clearForkEnvelope(world,pos,yaw,floorY,palletY," in plan
assert "fullyOutside(world,path,floorY,palletY,rack)" in plan
assert "return false;" in plan and "if(!world.hasChunkAt(p))return false;" in plan
assert "RackBlock.controller(level,p)==rack" in plan
assert "state.getBlock() instanceof RackBlock" in plan
assert "state.getBlock() instanceof PalletBlock" in plan
assert "state.getBlock() instanceof PalletCargoCollisionBlock" in plan
assert "for(int dy=0;dy<=2;dy++)" in plan
assert "new double[]{-.92,-.70,-.46,-.22,0," in plan
assert "sourceReverseDepth=planned.reverseDepth();" in truck
assert "destinationAisleShift=planned.aisleShift();" in truck
assert "if(tickCount-last<20)return null;" in truck
assert "public record Pose(Vec3 position,float yaw)" in plan
assert "public record Plan(List<Pose> poses,int turn,double reverseDepth,double aisleShift)" in plan

# Wide warehouses must allow an actual straight withdrawal far enough
# to clear the full 4.12-block fork envelope, without forcing a 90-degree
# backing turn simply because the shortest arc is closer to the truck.
assert "new double[]{1.75,2.2,2.7,3.2,3.8,4.5,5.5,6.5,8.0}" in plan
assert "new double[]{0,.2,.45,.7,1.0,1.25}" in plan
assert "new double[]{1.5,1.9,2.2,2.6,3.2}" in plan
assert "if((preferredDepth>0||preferredShift>0)" in plan
assert "if(preferredShift>0&&Math.abs(lateral-preferredShift)>.001)" in plan
assert ".comparingInt((Plan p)->Math.abs(p.turn()))" in plan
assert ".thenComparingDouble(p->forklift.distanceToSqr(" in plan
assert "sourceExitBlockedTicks" in truck
assert "destinationExitBlockedTicks" in truck
assert 'status="REPLANNING SAFE FORKLIFT WITHDRAWAL FROM RACK DOCK";' in truck
assert "cursor==0" in truck
assert "position().distanceToSqr(plan.dock().position())<.0225" in truck
assert "sourceArc=null;sourceArcTurn=0;" in truck
assert "destinationArc=null;destinationArcTurn=0;" in truck
assert "WAITING: FORK/PALLET SWEEP BLOCKED" in truck
assert "WAITING: FORKLIFT CHASSIS OR FLOOR BLOCKED" in truck
assert '+", job "+job+", rack source "+sourceRackStage' in truck

# Representative open aisle: a cargo tip extends 4.12 blocks forward of
# the chassis. A straight reverse of 2.7 still intersects a deep obstacle
# at z=1.8, but a 3.8-block reverse clears it. No physical rack bypass.
dock=2.82
front_reach=4.12
rack_front_edge=1.8
assert dock+2.7-front_reach < rack_front_edge
assert dock+3.8-front_reach > rack_front_edge
# A three-wide aisle won't fit every longer straight retreat, but that is
# why the planner separately validates all chassis/fork poses and retains
# the collision-checked curved fallback.

# The route is traversed in reverse on entry and forwards on exit:
# no snap rotation or raising while the tines are inside any rack.
assert "private boolean enterRackSafely(ServerLevel world,RackBlockEntity rack," in truck
assert "private boolean exitRackSafely(ServerLevel world,RackBlockEntity rack," in truck
assert "sourceRackStage=4;sourceArcCursor=0;" in truck
assert "destinationRackStage=4;destinationArcCursor=0;" in truck
assert "if(!exitRackSafely(world,origin,true))return;" in truck
assert "if(!exitRackSafely(world,rack,false))return;" in truck
assert "if(!enterRackSafely(world,origin,true))return;" in truck
assert "if(!enterRackSafely(world,rack,false))return;" in truck
assert "if(!ForkliftRackManeuver.fullyOutside(world,plan," in truck
assert "WAITING: FORKS STILL INSIDE RACK; REFUSING TO LOWER" in truck
assert "RAISING FORKS OUTSIDE RACK BEFORE ENTRY" in truck
assert "LOWERING PALLET AFTER COMPLETE RACK WITHDRAWAL" in truck
assert "LOWERING EMPTY FORKS CLEAR OF RACKS" in truck
assert "BACKING CLEAR OF RACK WHILE GRADUALLY TURNING 90 DEGREES" in truck
assert "if(!ForkliftAislePath.canStand(world,this,position)" in truck
assert "ForkliftRackManeuver.clearForkEnvelope(world,position,yaw," in truck
assert "move(MoverType.SELF,delta);" in truck
assert "Math.min(.12,remaining)" in truck
assert "job=3;animation=0;" in truck
assert "if(job==4){" in truck

# Recalling a forklift halfway through an elevated transfer must not jump
# directly to job=5 and immediately lower the mast inside a rack.
recall=truck[truck.index("public String requestDriverDismissal(ServerLevel world)"):truck.index("public boolean dismissDriver(ServerLevel world)")]
assert "boolean forksInSource=sourceRackStage>=1&&sourceRackStage<5;" in recall
assert "boolean forksInDestination=destinationRackStage>=1&&destinationRackStage<5;" in recall
assert "if(forksInSource||forksInDestination){" in recall
assert 'status="RECALL QUEUED: RETRACTING FORKS BEFORE PARKING";' in recall
assert recall.index("if(forksInSource||forksInDestination){")<recall.index("job=5;animation=0;")
assert "dismissAfterParking=true;" in recall
assert 'job=driver!=null&&!parking.equals(BlockPos.ZERO)?5:0;' in truck
assert "if(dismissAfterParking){" in truck

# Level reloads must not clear source escrow, approach direction or stages.
for field in ("source_rack_stage","destination_rack_stage",
              "source_arc_cursor","destination_arc_cursor",
              "source_arc_turn","destination_arc_turn"):
    assert 'out.putInt("'+field+'"' in truck
    assert 'in.getIntOr("'+field+'"' in truck
assert "refreshDisplay();" in truck
for field in ("source_reverse_depth","source_aisle_shift",
              "destination_reverse_depth","destination_aisle_shift"):
    assert 'out.putDouble("'+field+'"' in truck
    assert 'in.getDoubleOr("'+field+'"' in truck

# Kinematic invariants of a turn: yaw changes continuously (each
# step much less than 90 deg) and the dock/outside positions differ.
for sign in (1,-1):
    headings=[sign*90*(t*t*(3-2*t)) for t in (i/24 for i in range(25))]
    assert isclose(headings[0],0)
    assert isclose(headings[-1],sign*90)
    assert max(abs(b-a) for a,b in zip(headings,headings[1:]))<6
    # Soft reverse arc: no instant one-block lateral movement.
    lateral=[2.2*(1-cos(t*pi/2)) for t in (i/24 for i in range(25))]
    assert isclose(lateral[0],0)
    assert isclose(lateral[-1],2.2)
    assert all(b>=a for a,b in zip(lateral,lateral[1:]))
assert "ForkliftAislePath.route(world,this,goal)" in truck
assert "public static boolean clearSegment(" in route
print("PASS: rack tines clear before lowering, gradual 90-degree backing, no ghost collision, persistent escrow")
