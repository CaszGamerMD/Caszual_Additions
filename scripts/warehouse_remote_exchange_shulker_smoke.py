#!/usr/bin/env python3
"""Fail CI if remote pallet lifecycle / Shulker Station wiring regresses."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions"
def read(path):return path.read_text()
rig=read(main/"MovingPallet.java")
scanner=read(main/"StorageScannerItem.java")
dispatch=read(main/"RemotePalletDispatch.java")
swap=read(main/"StorageGroundPalletTransfer.java")
magazine=read(main/"StackOfPalletsItem.java")
golem=read(main/"StorageGolem.java")
sorting=read(main/"StorageGolemDispatch.java")
station=read(main/"ShulkerTankEntity.java")
stationMenu=read(main/"ShulkerStationMenu.java")
stationScreen=read(client/"racks/ShulkerStationScreen.java")
stationBlock=read(main/"ShulkerTankBlock.java")
registration=read(main/"RackContent.java")
clientReg=read(client/"CaszualAdditionsClient.java")
assert 'tag.putBoolean("remote_pickup_pending",true)' in scanner
# Linking must NOT call enlist: that would drain the four shelf Golems
# which the remote dispatch transaction needs for its own pallet rig.
assert "if(sameWarehouse){" in scanner
assert scanner.index("if(sameWarehouse){") < scanner.index("StoragePalletCrew.enlist(level,player,computer)")
assert 'Golems remain docked for remote pickup' in scanner
assert 'Wrap it with the portable wrapper before requesting pickup.' in scanner
# PTP dropPallet applies live filters; compare that exact canonical stack.
assert '.applyFilterSettings(ptp.palletStack(),ptp)' in dispatch
assert "RemotePalletDispatch.request(world,player,computer,target)" in scanner
# Real long-distance dispatch uses temporary ticking tickets, not /forceload
# or a hidden teleport. Both the pending request and travelling entity renew.
tickets=read(main/"WarehouseRemoteTickets.java")
assert "Registry.register(BuiltInRegistries.TICKET_TYPE" in tickets
assert "new TicketType(240," in tickets
assert "TicketType.FLAG_SIMULATION" in tickets
assert "addTicketWithRadius(type,new ChunkPos(pos),RADIUS)" in tickets
assert "WarehouseRemoteTickets.initialize();" in registration
assert "WarehouseRemoteTickets.keep(world,computer);" in scanner
assert "WarehouseRemoteTickets.keep(server,blockPosition());" in rig
assert "WarehouseRemoteTickets.keep(server,deliveryPosition);" in rig
assert "ServerTickEvents.END_SERVER_TICK" in scanner
assert "StorageScannerItem.initialize();" in registration
assert "beginRemoteExchange(old.getBlockPos(),computer)" in dispatch
assert "StackOfPalletsItem.take(change)" in dispatch
assert "sourcePtp.removePallet()" in dispatch
# Withdrawal validation must roll back before consuming any shelf tokens.
assert 'sourceRack.setItem(sourceSlot,previous);\n                return "WAITING: pallet magazine' in dispatch
assert "if(!removed.isEmpty()&&!sourcePtp.installPallet(removed))" in dispatch
assert "drop.getX()+.5,drop.getY()+.5,drop.getZ()+.5,removed" in dispatch
assert "if(consumed==4&&level.addFreshEntity(carrier))" in dispatch
assert "for(int i=0;i<consumed;i++)shelves.get(i).add();" in dispatch
assert "exchange(ServerLevel level,BlockPos root,MovingPallet carrier)" in swap
assert "NonNullList.withSize(216,ItemStack.EMPTY)" in swap
assert "PalletCargoCollisionBlock.clear(level,root,previous)" in swap
assert "carrier.acceptRecoveredPallet(oldDeck,stock)" in swap
assert "PalletCargoCollisionBlock.refresh(level,root)" in swap
assert "deliveryMode=3;" in rig and "deliveryMode=4;" in rig
assert "returnToWarehouse(ServerLevel level,BlockPos computer)" in rig
assert "navigateRemoteFloor(ServerLevel level,BlockPos target)" in rig
assert "StorageGroundPalletTransfer.exchange(level,target,this)" in rig
assert "out.store(\"remote_warehouse\",BlockPos.CODEC,remoteWarehouse)" in rig
assert 'in.read("remote_warehouse",BlockPos.CODEC)' in rig
assert "for(PtpBlockEntity candidate:warehouse.points())" in rig
assert "candidate.input()&&!point.input()" in rig
assert "chosen.setInput(true)" in rig or "point.setInput(true)" in rig
assert "withWrap(pallet.copy(),false)" in rig
assert "done=deliverToRack(rack)" in rig
assert "tickPalletStackJob(ServerLevel level)" in golem
assert "palletStackJob=in.getBooleanOr" in golem
assert "StackOfPalletsItem.put(updated,cargo)" in golem
assert "dispatchPalletMagazine(ServerLevel level" in sorting
assert "ptp.input()||" in sorting or "!ptp.input()||" in sorting
assert "public static final int CAPACITY=12;" in magazine
assert "ItemContainerContents.fromItems(safe)" in magazine
assert "SHULKER_STATION_MENU=Registry.register" in registration
assert "ShulkerStationScreen::new" in clientReg
assert "ExtendedMenuProvider<BlockPos>" in station
assert "serverPlayer.openMenu(tank)" in stationBlock
assert "public static final int COLS=8,ROWS=8,STOCK=64;" in stationMenu
assert "outputConnected()" in stationScreen
print("PASS: remote 216-slot escrow, scanner requests, PTP return, Golem pallet magazine and Shulker Station GUI")
