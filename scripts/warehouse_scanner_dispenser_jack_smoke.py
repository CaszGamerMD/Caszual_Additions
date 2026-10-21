#!/usr/bin/env python3
"""Scanner relinking, persistent Pallet Dispenser floor, Pallet Jack cargo safety."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions"
client=root/"src/client/java/com/caszgamermd/caszualadditions"
assets=root/"src/main/resources/assets/caszual_additions"
scanner=(main/"racks/StorageScannerItem.java").read_text()
computer=(main/"racks/RackComputerBlock.java").read_text()
rack=(main/"racks/RackContent.java").read_text()
jack=(main/"racks/PalletJackItem.java").read_text()
pallet=(main/"pallet/PalletBlock.java").read_text()
entity=(main/"pallet/PalletBlockEntity.java").read_text()
proxy=(main/"pallet/PalletCargoCollisionBlock.java").read_text()
state=(client/"racks/StackOfPalletsRenderState.java").read_text()
renderer=(client/"racks/StackOfPalletsRenderer.java").read_text()
dispenser=(main/"racks/StackOfPalletsBlockEntity.java").read_text()
lang=json.loads((assets/"lang/en_us.json").read_text())
recipe=json.loads((root/"src/main/resources/data/caszual_additions/recipe/pallet_jack.json").read_text())
model=json.loads((assets/"models/item/pallet_jack.json").read_text())

# Link/unlink preserves the mode, clears old warehouse coordinates and pending
# remote swap. Rebinding must not strand a loaded manual moving pallet.
for key in ('warehouse_dimension','warehouse_x','warehouse_y','warehouse_z',
            'remote_pickup_pending','remote_pickup_x','remote_pickup_y',
            'remote_pickup_z'):
    assert f'"{key}"' in scanner
assert 'private static void unlink(ItemStack scanner)' in scanner
assert 'boolean same=computer.equals(previous);' in scanner
assert 'if(same){' in scanner and 'bind(scanner,level,computer);' in scanner
assert 'clearPickup(scanner);' in scanner
assert 'StoragePalletCrew.recallFollowing(level,player);' in scanner
assert 'if(carrying)' in scanner
assert 'bind(scanner,level,computer);' in scanner
assert 'held.is(RackContent.STORAGE_SCANNER_ITEM)' in computer
assert 'StorageScannerItem.onComputerClick(server,user,held,pos)' in computer
assert 'Scanner is now linked to ' in scanner

# Preserve the historical item/block IDs and existing saved 12-slot contents.
assert 'CaszualAdditions.id("stack_of_pallets")' in rack
assert 'STACK_OF_PALLETS=' in rack
assert 'new StackOfPalletsItem(' in rack
assert 'new StackOfPalletsBlock(' in rack
assert lang["item.caszual_additions.stack_of_pallets"]=="Pallet Dispenser"
assert lang["block.caszual_additions.stack_of_pallets"]=="Pallet Dispenser"
assert 'Component.literal("Pallet Dispenser")' in dispenser
assert 'new BlockModelRenderState()' in state and 'floorGrill' in state
assert 'blocks.update(state.floorGrill,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY)' in renderer
assert 'for(int line=1;line<8;line++)' in renderer
assert renderer.index('var grille=state.floorGrill;') < renderer.index('if(!state.occupied[i])continue;',renderer.index("public void submit("))
assert 'state.boards[index][i].tintLayers()' in renderer

# Jack is a reusable ONE-load tool; exactly 1 deck + 216 distinct cargo
# slots in its CONTAINER, with NO nested filled pallet item.
assert 'public final class PalletJackItem extends Item' in jack
assert 'private static final int SIZE=217;' in jack
assert 'packed.set(0,deck.copyWithCount(1));' in jack
assert 'for(int i=0;i<216;i++)packed.set(i+1,source.getItem(i).copy())' in jack
assert 'StorageGroundPalletTransfer.wrappedController(world,selected)' in jack
assert 'jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed));' in jack
assert jack.index('jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed));') < jack.index('source.clearContent();')
assert 'PalletCargoCollisionBlock.clear(world,root,state);' in jack
assert 'controller.initialize(desired,0,deck);' in jack
assert 'controller.restoreJackCargo(contents);' in jack
assert jack.index('controller.restoreJackCargo(contents);') < jack.index('jack.remove(DataComponents.CONTAINER);')
assert 'root.sync();' in entity[entity.index('restoreJackCargo'):entity.index('packedCarrierDrop')]
assert 'if(!world.setBlock(at,facing.setValue(PalletBlock.PART,part),3))break;' in jack
assert 'BlockState test=block.defaultBlockState()' in jack
assert '!world.getBlockState(at).canBeReplaced()' in jack
assert '||RackBlock.reservedForAccess(world,at)' in jack
assert 'AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL' in jack
assert 'SLOW,-0.40' in jack
assert 'speed.addTransientModifier(LOAD_PENALTY)' in jack
assert 'speed.removeModifier(SLOW)' in jack
assert 'PalletJackItem.initialize()' in rack
assert 'RackContent.PALLET_JACK' in pallet
assert 'RackContent.PALLET_JACK' in proxy
assert 'add.accept(RackContent.PALLET_JACK);' in (main/"CaszualItemGroups.java").read_text()
assert lang["item.caszual_additions.pallet_jack"]=="Pallet Jack"
assert recipe["result"]["id"]=="caszual_additions:pallet_jack"
assert len(model["elements"])>=8
print("PASS: scanner relinks/unlinks safely; Pallet Dispenser always shows steel grill; Pallet Jack keeps 216 slots and applies loaded slowdown")
