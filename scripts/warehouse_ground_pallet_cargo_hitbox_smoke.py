#!/usr/bin/env python3
"""Ground pallet cargo: outline, collision and interaction consistency."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
base=root/"src/main/java/com/caszgamermd/caszualadditions"
pallet=(base/"pallet/PalletBlock.java").read_text()
cargo=(base/"pallet/PalletCargoCollisionBlock.java").read_text()
entity=(base/"pallet/PalletBlockEntity.java").read_text()
render=(root/"src/client/java/com/caszgamermd/caszualadditions/pallet/PalletBlockEntityRenderer.java").read_text()
wrapper=(base/"racks/PortablePalletWrapperItem.java").read_text()
scanner=(base/"racks/StorageScannerItem.java").read_text()
stack=(base/"racks/StackOfPalletsItem.java").read_text()

# Existing 2x2 outline grows up only by the real packed 3x3x3 cargo cells.
assert "PalletCargoCollisionBlock.selectionShape(level,pos,root,state)" in pallet
assert "Shapes.or(deck," in pallet
assert "PalletCargoCollisionBlock.shapeFor(level, pos)" in pallet
assert "return shapeFor(level,pos);" in cargo
assert "for(int part=0;part<4;part++)" in cargo
assert "for(int height=0;height<=2;height++)" in cargo
assert "Voxe" in cargo and "section.move(" in cargo
assert "return shapeFor(level, pos);" in cargo
assert "count < CAPACITY" in cargo and "private static final int CAPACITY = 27" in cargo
assert "private static final double FLOOR = 0.3125" in cargo
assert "private static final double CELL = 2.0 / 3.0" in cargo
assert "DISPLAY_LAYERS = 3" in (root/"src/client/java/com/caszgamermd/caszualadditions/pallet/PalletBlockEntityRenderState.java").read_text()
assert "PalletCargoCollisionBlock.visibleCount(" in render
assert "PalletCargoCollisionBlock.refresh(server, worldPosition)" in entity
assert "PalletCargoCollisionBlock.clear(server, root, state)" in pallet
# The invisible upper sections must not intercept the inventory or tooling.
assert "PalletBlock.openPallet(level,root,level.getBlockState(root),player)" in cargo
assert "PalletCargoCollisionBlock.owningPallet(level,clicked)" in wrapper
assert "PalletCargoCollisionBlock.owningPallet(level,clicked)" in scanner
assert "PalletCargoCollisionBlock.owningPallet(level,clicked)" in stack
for name in ("PORTABLE_WRAPPER","STORAGE_SCANNER_ITEM","STACK_OF_PALLETS"):
    assert f"RackContent.{name}" in cargo
assert "pallet.playerWillDestroy(level,root,home,player)" in cargo
print("PASS: 2x2 floor pallet selection grows to real cargo; proxy clicks, tool use and drops delegate to root")
