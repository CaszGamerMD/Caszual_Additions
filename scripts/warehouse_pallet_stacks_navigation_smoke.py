#!/usr/bin/env python3
"""Source-level safety invariants (in addition to Fabric compile and game QA)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
def read(p):return (main/p).read_text()
blocks=read("StackOfPalletsBlock.java")
be=read("StackOfPalletsBlockEntity.java")
item=read("StackOfPalletsItem.java")
registration=read("RackContent.java")
network=read("WarehouseNetwork.java")
sorting=read("StorageGolemDispatch.java")
worker=read("StorageGolem.java")
dispatch=read("RemotePalletDispatch.java")
carrier=read("MovingPallet.java")
route=read("WarehousePreferredPath.java")
assert "extends BlockItem" in item
assert "super.useOn(context)" in item
assert 'CaszualAdditions.id("stack_of_pallets")' in registration
assert "StackOfPalletsBlockEntity::new" in registration
assert "pallets=NonNullList.withSize(12,ItemStack.EMPTY)" in be
assert "ContainerHelper.saveAllItems(output,pallets)" in be
assert "ContainerHelper.loadAllItems(input,pallets)" in be
assert "magazine.loadMagazine(stack)" in blocks
assert "ExtendedMenuProvider<BlockPos>" in be
assert "server.openMenu(magazine);" in blocks
assert "PALLET_STACK_MENU=Registry.register" in registration
stack_menu=read("StackOfPalletsMenu.java")
assert "CAPACITY=12" in stack_menu
assert "addStandardInventorySlots(inventory,8,111);" in stack_menu
assert "StackOfPalletsScreen::new" in (
    root/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java").read_text()
assert "magazine.asItem()" in blocks
assert "magazine.put(cargo.copyWithCount(1))" in worker
assert "magazines.putIfAbsent(pos,stack)" in network
assert "network.magazines()" in dispatch
assert "sourceMagazine.take()" in dispatch
assert "sourceMagazine.put(fresh)" in dispatch
assert "Placed magazines" in dispatch
assert "WarehousePreferredPath.leg" in carrier and "WarehousePreferredPath.leg" in worker
assert "remoteFloorRoute.clear();" in carrier
assert "blockedRemoteCell=next.immutable();" in carrier
assert "nextRemotePlan=tickCount+10" in carrier
assert "getMoveControl().setWantedPosition" in carrier
assert "WarehouseRemoteTickets.keep(server,deliveryPosition);" in carrier
assert "WarehouseRemoteTickets.keep(server,new BlockPos(" in carrier
assert "out.putInt(\"delivery_mode\",deliveryMode);" in carrier
assert 'out.store("delivery_position",BlockPos.CODEC,deliveryPosition)' in carrier
assert "new int[]{0,-1,1}" in route
assert "if(flat!=null)return flat;" in route
assert "if(route==null)" in worker
print("PASS: placeable saved 12-pallet magazine, loss-safe physical supply,"
      " persistent remote assignments and flat-first Golem routes")
