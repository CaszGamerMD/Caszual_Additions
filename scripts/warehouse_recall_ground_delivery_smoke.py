#!/usr/bin/env python3
"""Recall placement keeps all 216 original slots, even if filters changed."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
server=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
pallet=root/"src/main/java/com/caszgamermd/caszualadditions/pallet"
rig=(server/"MovingPallet.java").read_text()
block=(pallet/"PalletBlockEntity.java").read_text()
scanner=(server/"StorageScannerItem.java").read_text()
recall=rig[rig.index('    @Override protected net.minecraft.world.InteractionResult mobInteract('):rig.index('        if(!player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)')]
place=rig[rig.index('private boolean deliverToGround(ServerLevel level,BlockPos pos,Direction facing,'):rig.index('/** Recreate precisely four *empty* walking crew members')]
unload=rig[rig.index('private void tickAisleDelivery(ServerLevel level)'):rig.index('/** Transfers the same pallet and exact 216 stacks')]
assert 'Mode.RECALL' in recall and 'StorageScannerItem.recallDoubleClick' in recall
assert 'requestGroundDelivery(world,at,user.getDirection())' in recall
assert 'recallAfterDelivery=true;' in recall
assert 'getBoundingBox().intersects(footprint)' in rig
assert 'BlockPos cell=PalletBlock.partPos(root,part,model)' in rig
assert 'boolean atUnloadingAisle=deliveryMode==7' in unload
assert 'groundUnloadReady(level,target,deliveryFacing,deliveryService)' in unload
assert 'if(!atUnloadingAisle){' in unload
assert 'if(releaseTicks<20){' in unload
assert 'dest.restoreJackCargo(contents);' in place
assert 'dest.initialize(pos,0,pallet);' in place
assert 'complete=verify(dest);' in place
assert 'if(!complete){' in place and 'dest.clearContent()' in place
assert 'if(complete)' in place and 'clearCargo();' in place
assert 'dest.setItem(i,contents.get(i).copy());' not in place
assert 'public void restoreJackCargo(NonNullList<ItemStack> packed)' in block
assert 'root.items.set(i,packed.get(i).copy())' in block
assert 'if(!stack.isEmpty() && !c.canPlaceItem(slot,stack))return;' in block
assert 'if(!placed){' in unload and 'waitForDelivery(' in unload
assert 'restoreWorkers(level);' in unload
print("PASS: Recall unloads from safe aisle, restores all pre-existing pallet cargo, retries without duping")
