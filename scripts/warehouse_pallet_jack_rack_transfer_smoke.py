#!/usr/bin/env python3
"""Pallet Jack rack pickup/drop-off must be lossless, exclusive and routed."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
folder=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
jack=(folder/"PalletJackItem.java").read_text()
rack=(folder/"RackBlockEntity.java").read_text()
block=(folder/"RackBlock.java").read_text()

# A rack's normal GUI MUST NOT intercept the jack in either hand.
assert "||held.is(RackContent.PALLET_JACK)" in block
assert "||held.is(RackContent.FORKLIFT_ITEM)" in block
assert "RackBlockEntity rack=RackBlock.controller(server,clicked);" in jack
assert jack.index("RackBlockEntity rack=RackBlock.controller(server,clicked)") < jack.index("if(loaded(jack))\n            return unload(")
assert "if(rack.installFromJack(jack))" in jack
assert "if(rack.liftToJack(jack))" in jack
assert "if(rack.hasPallet()||!rack.isEmpty())" in jack
assert "!rack.hasPallet()||!rack.glassWrapped()" in jack
assert "server.mayInteract(player,root)" in jack
assert "player.mayUseItemAt(root,context.getClickedFace(),jack)" in jack

# Pickup: wrapped pallet, rack filters, and exact 216 cargo slots move
# together; the jack takes ownership BEFORE the rack is cleared.
pickup=rack[rack.index("public boolean liftToJack("):rack.index("public boolean installFromJack(")]
assert "PalletJackItem.loaded(jack)" in pickup
assert "!glassWrapped()" in pickup
assert "forklift?RackPickupPlan.forklift(world,this):RackPickupPlan.find(world,this)" in pickup
assert "pullOwner.isBlank()" in pickup
assert "PalletData.applyFilterSettings(pallet,this)" in pickup
assert "GlassPalletWrap.withWrap(" in pickup
assert "NonNullList.withSize(217,ItemStack.EMPTY)" in pickup
assert "packed.set(0,deck.copyWithCount(1));" in pickup
assert "for(int i=0;i<CAPACITY;i++)packed.set(i+1,items.get(i).copy());" in pickup
assert "jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed));" in pickup
assert pickup.index("jack.set(DataComponents.CONTAINER") < pickup.index("items=NonNullList.withSize(CAPACITY")
assert pickup.index("jack.set(DataComponents.CONTAINER") < pickup.index("pallet=ItemStack.EMPTY")
assert 'filterUpgrade=ItemStack.EMPTY' in pickup
assert 'filterMode=0;' in pickup and 'filterScope=0;' in pickup
assert "WarehouseRackAccess.release(world,this);" in pickup

# Unloading into a different empty rack must preserve original slot indices,
# including filtered/previously stored contents. No loss on failure.
install=rack[rack.index("public boolean installFromJack("):rack.index("public ItemStack takePallet(")]
assert "PalletJackItem.loaded(jack)" in install
assert "hasPallet()||!isEmpty()" in install
assert "NonNullList<ItemStack> packed=PalletJackItem.unpack(jack);" in install
assert "GlassPalletWrap.wrapped(deck)" in install
assert "WarehouseRackAccess.clearOfStairs(world,this)" in install
assert "WarehouseRackAccess.prepare(world,this)" in install
assert "install(deck.copyWithCount(1));" in install
assert "for(int i=0;i<CAPACITY;i++)items.set(i,packed.get(i+1).copy());" in install
assert install.index("setChanged();") < install.index("jack.remove(DataComponents.CONTAINER);")
assert "WarehouseRackAccess.release(world,this);" in install
assert "static NonNullList<ItemStack> unpack(ItemStack jack)" in jack

# The already-shipped ground lift and ground placement pathways remain.
assert "StorageGroundPalletTransfer.wrappedController(world,selected)" in jack
assert "PalletCargoCollisionBlock.clear(world,root,state)" in jack
assert "controller.restoreJackCargo(contents);" in jack
assert 'SLOW,-0.40' in jack
print("PASS: rack pickup and unload forward to Pallet Jack and escrow exact 216 inventory slots with rack safety")
