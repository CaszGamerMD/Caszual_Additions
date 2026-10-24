#!/usr/bin/env python3
"""Static warehouse regression checks; full Fabric 26.2 compile runs separately."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions"
assets=root/"src/main/resources/assets/caszual_additions"
def read(name):return (main/name).read_text()
ptp=read("PtpBlock.java")
storage=read("PtpBlockEntity.java")
stack=read("StackOfPalletsBlock.java")
inventory=read("StackOfPalletsBlockEntity.java")
renderer=(client/"racks/StackOfPalletsRenderer.java").read_text()
renderstate=(client/"racks/StackOfPalletsRenderState.java").read_text()
registry=(client/"CaszualAdditionsClient.java").read_text()
assert "public boolean canRemoveEmptyPallet()" in storage
for clause in ("hasPallet()&&!sealed&&emptyCargo()&&!hauling()",
               "!outputInFlight()&&pendingOutputOrders()==0",
               "return canRemoveEmptyPallet()?dropPallet():ItemStack.EMPTY;"):
    assert clause in storage
assert "retrieveEmptyPallet(ptp,player)" in ptp
assert "player.isShiftKeyDown()&&ptp.hasPallet()" in ptp
assert "player.getInventory().placeItemBackInInventory(recovered)" in ptp
assert 'Unwrap the pallet with shears before removing it.' in ptp
assert "ptp.changeModeFromComputer()" in ptp
assert "Cannot change PTP mode:" in ptp
assert "Cannot load pallet: "+\u0022\u0022 not in ptp  # diagnostic comes from controller state

assert "IntegerProperty.create(\"part\",0,3)" in stack
assert "level.getWorldBorder().isWithinBounds(pos)" in stack
assert "for(int part=0;part<4;part++)" in stack
assert "new StackOfPalletsBlockEntity(pos,state):null" in stack
assert "be.loadMagazine(stack)" in stack
assert "public static @Nullable StackOfPalletsBlockEntity controller" in stack
assert "Block.popResource(level,origin,magazine.asItem())" in stack
assert "level.removeBlock(other,false)" in stack
assert "getCollisionShape(BlockState state" in stack

assert "ItemContainerContents.fromItems(pallets)" in inventory
assert "ContainerHelper.saveAllItems(output,pallets)" in inventory
assert "ContainerHelper.loadAllItems(input,pallets)" in inventory
assert "BlockEntityRenderers.register(RackContent.PALLET_STACK_ENTITY,StackOfPalletsRenderer::new)" in registry
assert "new BlockModelRenderState[12][4]" in renderstate
assert "for(int slot=0;slot<12;slot++)" in renderer
assert "GlassPalletWrap.wrapped(palletItem)" in renderer
assert "PalletData.wood(palletItem)" in renderer
assert "PalletData.plasticColor(palletItem)" in renderer
assert "PalletContent.copperTextureBlock(pallet.copperAge())" in renderer
assert "float py=i*(StackOfPalletsBlock.PALLET_THICKNESS_PIXELS/16f)" in renderer
assert "if(!state.occupied[i])continue;" in renderer
variants=json.loads((assets/"blockstates/stack_of_pallets.json").read_text())["variants"]
assert len(variants)==80 and all(
    f"height={h},layer={layer},part={part}" in variants
    for layer in range(4) for part in range(4) for h in range(5))
print("PASS: PTP empty-pallet removal with escrow safety; real 2x2 pallet magazine, 12 per-slot dynamic renders and saved contents")
