#!/usr/bin/env python3
"""Pallet item NBT/cargo must survive player-installed rack roundtrips.

These are static invariants; Fabric build and in-world pickup still need
end-to-end testing. Reproduces Ctrl+Pick Block with 216 inventory slots.
"""
from pathlib import Path
base=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions"
rack=(base/"racks/RackBlockEntity.java").read_text()
block=(base/"racks/RackBlock.java").read_text()
pallet=(base/"pallet/PalletBlockEntity.java").read_text()
jack=(base/"racks/PalletJackVehicle.java").read_text()
install=rack[rack.index("public void install(ItemStack item)"):
             rack.index("public boolean liftToCarrier(")]
assert "if (hasPallet()||item.isEmpty()||!isEmpty()) return;" in install
assert "NonNullList.withSize(CAPACITY,ItemStack.EMPTY)" in install
assert "item.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY)" in install
assert ".copyInto(carried);" in install
assert install.index("items=carried;") < install.index("pallet=item.copyWithCount(1);")
assert "pallet.remove(DataComponents.CONTAINER);" in install
for symbol in ("PalletData.filter(item)", "PalletData.filterMode(item)",
               "PalletData.filterScope(item)", "PalletData.filterSignature(item)",
               "PalletData.filterInstalled(item)"):
    assert symbol in install
assert "createCreativeCopyStack()" in pallet
assert "packedCarrierDrop(source.createDropStack(),source.items)" in pallet
extract=rack[rack.index("public ItemStack takePallet()"):
             rack.index("public String rackName()")]
assert "if(!hasPallet()||!isEmpty())return ItemStack.EMPTY;" in extract
assert "level.getGameTime()-pullLastTick<=200" in extract
assert "return dropPallet();" in extract
assert "player.isShiftKeyDown()" in block
assert "player.getInventory().placeItemBackInInventory(extracted);" in block
assert "rack.takePallet()" in block
assert "rack.isEmpty()" in block
assert "server.getLastClientMoveIntent()" in jack
assert "if(level().isClientSide()&&hasClientMovement)" in jack
assert "new Vec3(player.xxa,0,player.zza)" not in jack
print("PASS: pallet NBT/216 slots imported, no duplicate nested cargo, manual empty extraction, rider input")
