#!/usr/bin/env python3
"""Warehouse UI and transfer invariants; Java compilation runs separately."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def file(part):
    return (ROOT/"src"/part).read_text()
main="main/java/com/caszgamermd/caszualadditions/"
client="client/java/com/caszgamermd/caszualadditions/"
pallet_menu=file(main+"pallet/PalletMenu.java")
pallet_ui=file(client+"pallet/PalletScreen.java")
computer=file(main+"racks/RackComputerNet.java")
computer_ui=file(client+"racks/RackComputerScreen.java")
highlight=file(client+"racks/RackHighlightClient.java")
client_net=file(client+"racks/RackComputerClient.java")
worker=file(main+"racks/StorageGolem.java")
crew=file(main+"racks/StoragePalletCrew.java")
shelf=file(main+"racks/StorageGolemShelfBlock.java")
door=file(main+"racks/StorageGolemShelfDoor.java")
station=file(main+"racks/ShulkerTankEntity.java")
station_menu=file(main+"racks/ShulkerStationMenu.java")
station_ui=file(client+"racks/ShulkerStationScreen.java")
hopper=file(main+"racks/PtpHopperPort.java")

# The last inventory column ends at x347. Filter module x356, editor
# x382+, so neither ghosts nor buttons can cover any server-backed slots.
assert "new Slot(moduleSlot,0,356,53)" in pallet_menu
assert "IMAGE_W = 554" in pallet_ui
assert "x+STORAGE_X,y+STORAGE_Y" in pallet_ui
assert "x+382,y+39,x+543,y+329" in pallet_ui
assert "filterOpen=menu.hasFilter();" in pallet_ui
assert "x>=398&&x<398+4*22" in pallet_ui
assert "drawSlots(graphics,x+PLAYER_X,y+PLAYER_Y" in pallet_ui

# One pallet per global 10s glow. Search does NOT paint all results.
assert "HighlightRequest" in computer and "HighlightChanged" in computer
assert "WarehouseNetwork.fromComputer(level,computer).racks()" in computer
assert "for(ServerPlayer online:player.getServer().getPlayerList().getPlayers())" in computer
assert "int ticks=online.level()==level?200:0" in computer
assert "private record Glow(BlockPos rack,long expires)" in highlight
assert "return glow.rack().equals(position);" in highlight
assert "RackHighlightClient.select(payload.rack(),payload.ticks())" in client_net
assert "HIGHLIGHT" in computer_ui and "highlightButton.active=loaded;" in computer_ui

# RECALL ALL addresses the connected network, not just the UI's player.
assert "dismissWarehouse(level,computer)" in computer
assert "for(var entity:level.getAllEntities())" in crew
assert "homes.contains(worker.shelfHome())" in crew
assert "rig.returnToWarehouse(level,computer)" in crew
assert "recallToShelf(ServerLevel level)" in worker
assert "cancelPendingPickup(level)" in worker and "stage=2;" in worker
# Front door faces the placer and is the ONLY spawn path.
assert "context.getHorizontalDirection().getOpposite()" in shelf
assert "StorageGolemShelfBlock.FACING" in door
assert "root.relative(facing,depth)" in door

# All 64/3/3/27 slots are true live server-side containers.
assert "private NonNullList<ItemStack> working=" in station
assert "private NonNullList<ItemStack> finished=" in station
assert "ContainerHelper.saveAllItems(output,boxes)" in station
assert "public ItemStack workingItem(int lane,int slot)" in station
assert "content.copyInto(slots)" in station
assert "public boolean loadEmptyIntoLane(int lane)" in station
assert "public boolean sendSelected(ServerLevel level,int lane)" in station
assert "getContainerSize(){return BOX_SLOTS;}" in station_menu
assert "new SelectedShulkerContainer()" in station_menu
assert "clickMenuButton(Player player,int id)" in station_menu
assert "SEND TO PALLET" in station_ui
# Hopper face, not warehouse PTP mode, determines item flow.
assert "implements WorldlyContainer" in hopper
assert "face!=Direction.DOWN&&canPlaceItem(slot,stack)" in hopper
assert "face==Direction.DOWN&&accessible()" in hopper
assert "c.canPlaceItem(slot,stack)" in hopper
assert "!c.sealed()" in hopper
print("PASS: pallet UI layout, one global glow, warehouse recall, shelf front,"
      " Shulker lane contents and hopper-sided transfers")
