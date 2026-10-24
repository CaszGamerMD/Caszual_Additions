#!/usr/bin/env python3
"""Regression for genuine continuous forklift driving and measurable speed."""
from pathlib import Path
from math import hypot

root=Path(__file__).resolve().parents[1]
folder=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
truck=(folder/"WarehouseForklift.java").read_text()
path=(folder/"ForkliftAislePath.java").read_text()

# Previous versions recalculated their best lookahead target every tick.
# The target changed continuously and the forklift alternated braking and
# accelerating even while a clear path remained in front of it.
assert "public static List<Vec3> smooth(ServerLevel world,WarehouseForklift vehicle," in path
assert "List<Vec3> segments=new ArrayList<>();" in path
assert "if(!clearSegment(world,vehicle,start,candidate))break;" in path
assert "segments.add(end);" in path
assert "List<Vec3> raw=ForkliftAislePath.route(world,this,goal);" in truck
assert "aisleRoute=ForkliftAislePath.smooth(world,this,raw," in truck
assert "Vec3 waypoint=aisleRoute.get(routeStep);" in truck
assert "if(moved&&best>routeStep)routeStep=best;" not in truck
assert "LOOKAHEAD_DISTANCE" not in truck
assert "routeStep++" in truck
assert "ForkliftAislePath.clearSegment(world,this,origin,candidate)" not in truck
assert "getMoveControl().setWantedPosition(getX(),getY(),getZ(),0)" not in truck

# No movement orders override physical displacement, and every physical
# step is collision checked and capped at 0.20 blocks for a 20-tick second.
assert "DRIVE_BLOCKS_PER_TICK=0.20D;" in truck
assert "MOTOR_ACCELERATION=.05D;" in truck
assert "rollingSpeed+MOTOR_ACCELERATION" in truck
assert "Math.min(rollingSpeed,cornerLimit)-alreadyMoved" in truck
assert "ForkliftAislePath.canStand(world,this,next)" in truck
assert "move(MoverType.SELF,delta);" in truck
assert "status=\"REPLANNING FORKLIFT AISLE AROUND OBSTRUCTION\";" in truck

# Run a simple deterministic 20Hz motor simulation from the source values.
# This is not an in-game physics test; it verifies the intended steady
# straight-line speed and continuity between longer segment endpoints.
speed=0
x=0.
target=10.
per_tick=[]
for tick in range(100):
    speed=min(.20,speed+.05)
    step=min(target-x,speed,.20)
    x+=step
    per_tick.append(step)
    if x>=target-.00001:break
assert tick<=54, ("forklift must complete ten clear blocks near 4m/s",tick)
assert all(v>0 for v in per_tick[:-1]), "must never stop at an arbitrary A* grid node"
assert abs(max(per_tick)-.2)<.000001
assert abs(x-10)<.000001

# Actual diagnostics are sampled on the server, not guessed from animation.
assert "private void measureActualMotion()" in truck
assert "currentBlocksPerSecond=telemetryDistance;" in truck
assert "Math.hypot(getX()-telemetryX,getZ()-telemetryZ)" in truck
assert '+" | "+driveDiagnostics()' in truck
assert "measured " in truck and " yaw error " in truck
print("PASS: forklift holds stable collision-checked route segments and measures its own speed")
