#!/usr/bin/env python3
"""Non-cube block cargo must use the cardboard path in every stored pallet."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
client=root/"src/client/java/com/caszgamermd/caszualadditions"
helper=(client/"pallet/PalletCargoDisplay.java").read_text()
ground=(client/"pallet/PalletBlockEntityRenderer.java").read_text()
rack=(client/"racks/RackBlockEntityRenderer.java").read_text()
fork=(client/"racks/WarehouseForkliftRenderer.java").read_text()
moving=(client/"racks/MovingPalletRenderer.java").read_text()

# Item type alone cannot classify shape: doors, signs and carpets are
# BlockItems despite being much smaller than a full cube.
assert "if(!(stack.getItem() instanceof BlockItem blockItem)||level==null)" in helper
assert "blockItem.getBlock().defaultBlockState()" in helper
assert "return !block.isCollisionShapeFullBlock(level,palletPos);" in helper
assert "PalletCargoDisplay.shouldBox(" in ground
assert "PalletCargoDisplay.shouldBox(" in rack
assert "state.boxed[displayIndex]=PalletCargoDisplay.shouldBox(" in ground
assert "state.boxed[i]=PalletCargoDisplay.shouldBox(" in rack
# Non-full cubes and ordinary non-block items use the same cardboard model,
# while true full cube items retain their existing exposed block render.
for renderer in (ground,rack):
    assert "ItemDisplayContext.GUI:ItemDisplayContext.NONE" in renderer or (
            "ItemDisplayContext.GUI" in renderer and "ItemDisplayContext.NONE" in renderer)
    assert "state.carton.submit(" in renderer
    assert "state.items[i].submit(" in renderer
assert "state.boxed[displayIndex] = false;" not in ground
assert "state.boxed[i]=!(stack.getItem() instanceof BlockItem)" not in rack
# Golem transport and forklift already display protective cartons, and
# must not be accidentally changed to display the problematic block models.
assert "state.crate" in moving
assert "state.crate" in fork
# Renderer-only change: both true slot count and the 3x3x3 visible
# display/collision grid are deliberately left untouched.
inventory=(root/"src/main/java/com/caszgamermd/caszualadditions/pallet/PalletBlockEntity.java").read_text()
assert "public static final int SLOTS = 216;" in inventory
render_state=(client/"pallet/PalletBlockEntityRenderState.java").read_text()
assert "public static final int DISPLAY_LAYERS = 3;" in render_state
print("PASS: non-full block items display as cardboard boxes on placed and racked pallets")
