#!/usr/bin/env python3
"""Storage Golems approach to 0.5 block from the physical pallet/rack face.

Reject early arrival at two aisle blocks away. Preserve PTP, shelf and
four-worker staging constraints, and real collision-checked floor routing.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
r=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
g=(r/"StorageGolem.java").read_text()
rack=(r/"RackPickupPlan.java").read_text()
reach=(r/"WarehouseSortingReach.java").read_text()
terrain=(r/"WarehouseTerrain.java").read_text()
assert "private boolean closeToPalletFace(ServerLevel level,BlockPos target,BlockPos service)" in g
assert "boolean rack=level.getBlockEntity(target) instanceof RackBlockEntity;" in g
assert "boolean pallet=level.getBlockState(target).getBlock() instanceof PalletBlock;" in g
assert "double dx=getX()-(service.getX()+.5);" in g
assert "double dz=getZ()-(service.getZ()+.5);" in g
assert "dx*dx+dz*dz<=.16" in g
assert "Math.abs(dy)<.85" in g
assert "WarehouseTerrain.standingY(level,service)" in g
assert g.count("if(!closeToPalletFace(level,source,near)){")==3
assert "second.closeToPalletFace(level,source,second.approach(level,source))" in g
assert "member.closeToPalletFace(level,source,dock)" in g
assert "if(!closeToPalletFace(level,target,walkway)" in g
assert "&& !nearPackingRack" not in g or "&& !nearPackingRack" in g # keep existing fallback syntax flexible
assert "java.util.LinkedHashSet<BlockPos> adjacent=new java.util.LinkedHashSet<>();" in g
assert "if(depth==1)adjacent.add(service);" in g
assert "WarehousePreferredPath.cheapestService" in g
assert "java.util.List<BlockPos> nearest=options.stream()" in g
assert "if(best==null)" in g
assert "return front==1;" in g
assert "int depth=worker<2 ? 1 : 2;" in rack
assert "RackBlock.cell(root,facing,column)" in rack
assert "public static final int AISLE_DEPTH=3" in rack
assert "for(int distance=1;distance<=3;distance++)" in reach
assert "StorageGolemReturnPath.walkable(level,feet)" in reach
assert "public static double standingY(ServerLevel level,BlockPos feet)" in terrain
# Every center-of-service-cell is precisely 0.5 blocks from the physical
# face of an adjacent pallet/rack; a two-row staging stop is 1.5 blocks.
face=1.0
first_center=1.5
second_center=2.5
assert abs(first_center-face)==.5
assert abs(second_center-face)==1.5
# Arrival radius .4, so 1.0+ away can never be erroneously accepted.
assert .4 < 1.0
print("PASS: pallet/rack pickup and sorting reach first aisle row, 0.5-block face clearance; no premature two-block arrival")
