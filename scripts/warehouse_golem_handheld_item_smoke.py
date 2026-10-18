#!/usr/bin/env python3
"""Warehouse worker actually holds the picked up item, never a tray.

The picked item render state is resolved from its real synced ItemStack,
not a fake proxy block. Render as an entity feature layer so the saved
item model inherits world entity yaw and forearm/hand transforms.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
server=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
renderer=(client/"StorageGolemRenderer.java").read_text()
model=(client/"StorageGolemModel.java").read_text()
state=(client/"StorageGolemRenderState.java").read_text()
golem=(server/"StorageGolem.java").read_text()

assert "heldTray" not in renderer and "heldTray" not in state
assert "Block material=PalletContent.CARDBOARD_RENDER_PROXY;" not in renderer
assert "state.heldSlots=" in renderer
assert "displayedLoad(i)" in renderer
assert "ItemStack candidate=golem.displayedLoad(i);" in renderer
assert "if(!candidate.isEmpty()){held=candidate;break;}" in renderer
assert "items.updateForLiving(state.heldItem,held," in renderer
assert "ItemDisplayContext.FIXED,golem" in renderer
assert "else state.heldItem.clear();" in renderer
assert "getItemModelResolver()" in renderer
assert "public final ItemStackRenderState heldItem=new ItemStackRenderState();" in state
assert "getParentModel().translateToCargoHand(pose);" in renderer
assert "rightArm.translateAndRotate(pose);" in model
assert "rightForearm.translateAndRotate(pose);" in model
assert "addLayer(new HeldCargoLayer(this));" in renderer
assert "extends RenderLayer<StorageGolemRenderState,StorageGolemModel>" in renderer
assert "state.heldItem.submit(pose,collector,light," in renderer
assert "OverlayTexture.NO_OVERLAY,state.outlineColor" in renderer
assert 'else if(state.heldSlots>0&&!state.carryingPallet)' in model
assert "syncHeldVisual();" in golem

# Sorting workers can carry a block OR any non-block item (including
# modded items). Item model resolver must see the SAME sample; no
# replacement material map may be used.
for item in ("minecraft:andesite","minecraft:grass_block",
             "minecraft:diamond","examplemod:custom_item"):
    assert item and "ItemStack candidate=golem.displayedLoad(i);" in renderer
print("PASS: no tray; real ItemStack model submitted from articulated wrist layer with entity yaw")
