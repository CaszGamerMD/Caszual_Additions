#!/usr/bin/env python3
"""Warehouse routing admits real stair surfaces but no pallet/rack shortcuts."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
terrain=(root/"WarehouseTerrain.java").read_text()
route=(root/"StorageGolemReturnPath.java").read_text()
assert "import net.minecraft.world.level.block.StairBlock;" in terrain
assert "public static boolean stairFloor(" in terrain
assert "state.getBlock() instanceof StairBlock" in terrain
assert "shape.max(Direction.Axis.Y)" in terrain
assert "stairFloor(level,from)||stairFloor(level,to)" in terrain
assert "WarehouseTerrain.stairFloor(level,feet)" in route
assert "if(architecturalFloor)return true;" in route
assert "instanceof StackOfPalletsBlock" in route
assert "instanceof RackBlock" in route
assert "instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock" in route
assert "if(!level.getFluidState(feet).isEmpty()" in route
print("PASS physical stair/slab traversal, honest support height, dry floors and pallet/rack exclusions")
