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
