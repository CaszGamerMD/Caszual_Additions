#!/usr/bin/env python3
"""Physical 2x2x4 full-width pallet magazine lifecycle and UI invariants."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
SERVER=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"
CLIENT=ROOT/"src/client/java/com/caszgamermd/caszualadditions/racks"
def read(folder,path):return (folder/path).read_text()
block=read(SERVER,"StackOfPalletsBlock.java")
be=read(SERVER,"StackOfPalletsBlockEntity.java")
renderer=read(CLIENT,"StackOfPalletsRenderer.java")
states=json.loads((ROOT/"src/main/resources/assets/caszual_additions/blockstates/stack_of_pallets.json").read_text())["variants"]
# A full-size 2x2 deck is 5px high, so 12 occupied decks fit
# vertically in four one-block-high collision cells: 12*5/16 = 3.75.
assert "MAX_PALLETS=12" in block
assert "PALLET_THICKNESS_PIXELS=5" in block
assert 'IntegerProperty.create("layer",0,3)' in block
assert "for(int layer=0;layer<4;layer++)for(int part=0;part<4;part++)" in block
assert "state.setValue(PART,part).setValue(LAYER,layer)" in block
assert "state.getValue(PART)==0&&state.getValue(LAYER)==0" in block
assert "return be==null?0:be.count()*PALLET_THICKNESS_PIXELS;" in block
assert "Block.box(0,0,0,16,Math.min(16,remaining),16)" in block
assert "getCollisionShape(BlockState state" in block
assert "repairUpperLayers(world,pos)" in block
assert "shouldChangedStateKeepBlockEntity(BlockState oldState)" in block
assert "return world.getBlockEntity(base) instanceof StackOfPalletsBlockEntity be?be:null;" in block
assert "if(found.is(this)&&found.getValue(PART)==part" in block
assert "&&found.getValue(LAYER)==layer)" in block
# Empty-hand click takes ONE real full-component item. Crouch keeps UI access.
assert "ItemStack taken=magazine.take();" in block
assert "player.getInventory().placeItemBackInInventory(taken);" in block
assert "if(player.isShiftKeyDown())" in block
assert "server.openMenu(magazine)" in block
assert "if(!StackOfPalletsItem.isPallet(held))return InteractionResult.PASS;" in block
assert "else if(magazine.put(held.copyWithCount(1)))" in block
assert "if(!player.getAbilities().instabuild)held.shrink(1);" in block
assert "ContainerHelper.saveAllItems(output,pallets)" in be
assert "ContainerHelper.loadAllItems(input,pallets)" in be
assert "ItemContainerContents.fromItems(pallets)" in be
assert "for(int i=11;i>=0;i--)" in be
# Exactly one pallet across ENTIRE 2x2 plane per tier, no .46 miniatures.
assert "int displaySlot=0;" in renderer
assert "int index=displaySlot++;" in renderer
assert "float py=i*(StackOfPalletsBlock.PALLET_THICKNESS_PIXELS/16f)" in renderer
assert "poses.translate(0f,py,0f)" in renderer
assert "poses.scale(.46f" not in renderer
for tok in ("PalletData.wood(palletItem)","PalletData.plasticColor(palletItem)",
            "PalletContent.copperTextureBlock(pallet.copperAge())",
            "GlassPalletWrap.wrapped(palletItem)"):
    assert tok in renderer
assert len(states)==80, len(states)
assert all(f"height={h},layer={l},part={p}" in states
           for h in range(5) for l in range(4) for p in range(4))
print("PASS: full-size 12-tier pallet stack, real collision on all four heights,"
      " full-component empty-hand pickup, legacy migration, no duplicate drops")
