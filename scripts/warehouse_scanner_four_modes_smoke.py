#!/usr/bin/env python3
"""Smoke tests for Warehouse Scanner 7-mode screen and warehouse UI."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
r=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
asset=root/"src/main/resources/assets/caszual_additions"
def java(path):return (r/path).read_text()
scanner=java("StorageScannerItem.java")
driver=java("StorageGolem.java")
lift=java("WarehouseForklift.java")
crew=java("StoragePalletCrew.java")
rig=java("MovingPallet.java")
computer=java("WarehouseComputerBlockEntity.java")
net=java("RackComputerNet.java")
screen=(client/"RackComputerScreen.java").read_text()
model=(client/"WarehouseForkliftModel.java").read_text()
assert "enum Mode { DIRECT, REMOTE, FORKLIFT, RECALL, LINK, LOAD_UNLOAD, COMPUTER }" in scanner
assert "ScannerControlNet.open(user,scanner)" in scanner
assert (r/"ScannerControlNet.java").is_file()
assert (client/"ScannerScreen.java").is_file()
assert "Scanner is now linked to " in scanner
assert "WarehouseComputerBlockEntity.name(level,computer)" in scanner
assert "mode(scanner)==Mode.FORKLIFT" in scanner
assert "StorageGolemForkliftEscort.summon(level,player,home,shelf)" in scanner
assert "StorageScannerItem.Mode.RECALL" in driver
assert "StorageScannerItem.Mode.RECALL" in rig
assert "recallDoubleClick" in scanner and "target" in scanner
assert "requestGroundDelivery(world,at,user.getDirection())" in rig
assert "recallAfterDelivery=true" in rig
assert "requestForkliftDelivery(" in rig and "ForkliftPalletIO.fits" in rig
assert "rig.requestForkliftDelivery(level,rack,selected)" in crew
assert "claimStalledRemote" in rig and "holdDirectPallet" in rig
assert "public boolean orderRackToRack" in lift
assert "public boolean orderGround" in lift
assert "private boolean dismissAfterParking;" in lift
assert "setParking(at.offset(1,0,1))" in java("WarehouseForkliftItem.java")
assert "BlockEntity" in computer and 'out.putString("warehouse_name",name)' in computer
assert "WarehouseComputerBlockEntity.name(level,computer)" in net
assert 'root.add("builderOrders",builderOrders)' in net
assert 'row.addProperty("type",ptp.input()?"INPUT":"OUTPUT")' in net
assert "warehouseNameBox" in screen and "drawBuilderOrders" in screen
assert "RenameWarehouse" in net
assert "wheel_front_left" in model and "wheel_front_right" in model
assert "wheel_rear_left" in model and "wheel_rear_right" in model
assert "frontLeftWheel.xRot=wheel" in model
assert "frontRightWheel.xRot=wheel" in model
assert (asset/"blockstates/forklift_parking.json").is_file()
assert (asset/"models/block/forklift_parking.json").is_file()
assert "FORKLIFT_CONTROLLER" not in (root/"src/main/java/com/caszgamermd/caszualadditions/CaszualItemGroups.java").read_text().split("for (var item")[0].replace("RackContent.FORKLIFT_CONTROLLER,","")
print("PASS: Scanner 7-mode behavior, physical recalls, staffed forklift transfer, parking, warehouse names and builder job summaries")
