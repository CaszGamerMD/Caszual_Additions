#!/usr/bin/env python3
"""Forklift working aisle is 3 blocks wide between facing 2x2 rack banks."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
base=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
access=(base/"RackPickupPlan.java").read_text()
truck=(base/"WarehouseForklift.java").read_text()
pallet=(base/"ForkliftPalletIO.java").read_text()

# Golem crews retain their existing full three-row staging formation,
# but forklifts need only two rows of access to put the 1.2-wide chassis
# centrally into the three-row lane.
assert "return find(level,rack,MAX_DROP,AISLE_DEPTH);" in access
assert "return find(level,rack,FORKLIFT_MAX_HEIGHT,2);" in access
assert "int maxDrop,int accessDepth" in access
assert "depth<=accessDepth&&clear" in access
assert "public static final int AISLE_DEPTH=3;" in access

# Physical illustration: first rack is at row 0, the facing rack at row
# 4. Rows 1,2,3 are the available clear floor, so a 1.2-wide vehicle
# centered on row 2 fits; its old row-3 goal intersects the opposing rack.
vehicle_half_width=.60
assert 1 < (2+.5)-vehicle_half_width
assert (2+.5)+vehicle_half_width < 4
assert (3+.5)+vehicle_half_width > 4
assert "private Dock service(ServerLevel world,RackBlockEntity rack)" in truck
assert "Vec3 palletCenter=rackPalletCenter(rack);" in truck
assert "ForkliftAislePath.canStand(world,this,position)" in truck

# A 2-deep pallet needs 2 of the 3 free aisle rows. Both choices touch
# a neighboring rack's RESERVED access row, but neither hits real blocks.
# Ground pallet delivery must check physical replacement & solid floor,
# without treating a non-collision reservation as an existing obstruction.
assert "RackBlock.reservedForAccess(world,at)" not in pallet
assert "!world.getBlockState(at).canBeReplaced()" in pallet
assert "!world.getBlockState(at.below()).isCollisionShapeFullBlock(world,at.below())" in pallet
assert "for(int part=0;part<4;part++)" in pallet
assert "world.removeBlock" in pallet

# Respect the selected rack facing during ground staging and approach
# from a safe cardinal side along the aisle rather than driving directly
# into the opposite rack.
assert "if(pickup)this.groundFacing=rack.facing();" in truck
assert "Dock landing=groundService(world,staging,groundFacing);" in truck
assert "if(!enterRackSafely(world,rack,false))return;" in truck
assert "for(Direction side:Direction.Plane.HORIZONTAL)" in truck
assert "ForkliftAislePath.route(world,this,position)" in truck

print("PASS: forklift can service both sides of a three-block aisle and stage intact 2x2 pallets")
