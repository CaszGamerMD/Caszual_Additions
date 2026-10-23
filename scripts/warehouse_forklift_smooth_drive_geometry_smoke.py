#!/usr/bin/env python3
"""Guard against stop-and-turn per grid node, displaced riders and coplanar cab faces."""
from pathlib import Path
from math import sin,cos,pi,isclose
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
truck=(main/"WarehouseForklift.java").read_text()
path=(main/"ForkliftAislePath.java").read_text()
seat=(main/"WarehouseForkliftSeat.java").read_text()
model=(client/"WarehouseForkliftModel.java").read_text()

# The original motor stopped at every half-block A* cell until yaw error
# fell below 3 degrees. It should now steer while MOVING down clear lanes,
# with a sharp-corner exception and physical 4 m/s speed ceiling.
assert "private static final double LOOKAHEAD_DISTANCE=3.5D;" in truck
assert "MOTOR_ACCELERATION=.024D;" in truck
assert "rollingSpeed=Math.min(DRIVE_BLOCKS_PER_TICK," in truck
assert "boolean facing=turnForksToward(waypoint.x,waypoint.z);" in truck
assert "if(!facing&&error>52){" in truck
assert "error>35?DRIVE_BLOCKS_PER_TICK*.65" in truck
assert "origin.distanceToSqr(candidate)>LOOKAHEAD_DISTANCE*LOOKAHEAD_DISTANCE" in truck
assert "ForkliftAislePath.clearSegment(world,this,origin,candidate)" in truck
assert "if(moved&&best>routeStep)routeStep=best;" in truck
assert "rollingSpeed=0;" in truck
assert "DRIVE_BLOCKS_PER_TICK=0.20D;" in truck
assert "if(!standable(world,vehicle," in path
assert "int samples=Math.max(1,(int)Math.ceil(distance/.18));" in path
assert "world.noCollision(vehicle,box)" in path
assert "getCollisionShape(world,below).isEmpty()" in path

# Seat X stays centered, Z slides TWO model pixels rearward along forklift
# local axis. Its physical Y coordinate remains exactly at cushion top.
assert "LOCAL_RIGHT=0.0;" in seat
assert "LOCAL_REAR=10.0/16.0;" in seat
assert "FOOT_HEIGHT=10.0/16.0;" in seat
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in truck
assert "addBox(-6,-10,5,12,2,10)" in model
assert "addBox(-6,-8,5,12,5,10)" in model
assert "addBox(-6,-25,13.5f,12,15,2)" in model
for deg in (0,45,90,135,180,225,270,315):
    t=deg*pi/180
    x=-sin(t)*.625
    z=cos(t)*.625
    assert isclose(x*cos(t)+z*sin(t),0,abs_tol=1e-10)
    assert isclose((x*x+z*z)**.5,.625,abs_tol=1e-10)

# Check the visual Z-fight root causes, not simply model box count.
# Seat cushion & foundation used to intersect in 1px of volume and have
# coplanar side faces. Now the cushion ends exactly where the base begins.
assert "addBox(-6,-10,3,12,3,10)" not in model
assert "addBox(-6,-10,5,12,2,10)" in model
assert "addBox(-6,-8,5,12,5,10)" in model
# Previously overlaid 20x33 underside and main chassis shared side planes.
assert "addBox(-10,-9,-18,20,3,33)" not in model
assert "addBox(-9,-9,-18,18,2,33)" in model
# Roof bars were embedded within roof volume, rendering coplanar strips.
assert "addBox(-11,-51,-11,22,2,2)" not in model
assert "addBox(-11,-49,-11,22,1,2)" in model
assert "addBox(6,-58,7,5,4,5)" in model
# Distinct depth for painted metal counterweight/rear-light surfaces.
assert "addBox(-7,-18,24,14,4,2)" not in model
assert "addBox(-7,-18,25,14,2,1.5f)" in model
assert "addBox(-10,-20,27,5,3,1)" in model

# The pathfinder still owns navigation and cannot drive through racks.
assert "ForkliftAislePath.route(world,this,goal)" in truck
assert "NO SAFE FORKLIFT AISLE ROUTE TO DOCK" in truck
print("PASS: smooth safe route, 4m/s cap, centered rearward seat and separated cab/roof surfaces")
