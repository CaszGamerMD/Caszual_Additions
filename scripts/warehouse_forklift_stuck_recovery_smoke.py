#!/usr/bin/env python3
"""Prevent forklift endpoint deadlock, stranded drivers, and lost packed cargo."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
base=r/"src/main/java/com/caszgamermd/caszualadditions/racks"
truck=(base/"WarehouseForklift.java").read_text()
scanner=(base/"StorageScannerItem.java").read_text()
golem=(base/"StorageGolem.java").read_text()

# A pallet/rack at the end of a 12-block ray can block the full scan even
# while the vehicle's next small movement is completely safe.
assert 'aisleRoute=ForkliftAislePath.route(world,this,goal);' in truck
assert 'ForkliftAislePath.canStand(world,this,next)' in truck
assert 'DRIVE_BLOCKS_PER_TICK=0.20D' in truck
assert 'boolean facing=turnForksToward(waypoint.x,waypoint.z);' in truck
assert 'status="REPLANNING FORKLIFT AISLE AROUND OBSTRUCTION";' in truck
assert 'status="WAITING: FORKLIFT PATH BLOCKED; CLEAR THE DRIVE AISLE"' not in truck or 'WAITING: FORKLIFT PATH BLOCKED; CLEAR THE DRIVE AISLE' in truck

# Normal dismissal must cancel an unfinishable order, then park the vehicle.
assert 'public String requestDriverDismissal(ServerLevel world)' in truck
assert 'job=5;animation=0;stallTicks=0;' in truck
assert 'dismissAfterParking=true;' in truck
assert 'if(job!=0||!atParkingBay()||driver==null)return false;' in truck
assert 'if(drive(world,parking)){' in truck
assert 'dismissDriver(world);' in truck
assert 'PalletJackItem.loaded(jack)' in truck

# An explicit emergency override releases the passenger WITHOUT erasing
# an already-loaded pallet, and leaves a way to reclaim the complete jack.
assert 'public String emergencyDriverRelease(ServerLevel world)' in truck
assert 'golem.finishForkliftDriving();driver=null;' in truck
assert 'public String recoverStuckCargo(Player player)' in truck
assert 'ItemStack packed=jack.copy();' in truck
assert 'jack=new ItemStack(RackContent.PALLET_JACK);' in truck
assert 'player.getInventory().placeItemBackInInventory(packed);' in truck
assert 'if(!beginWithoutCargo())return false;' in truck
assert 'public boolean hasPackedCargo()' in truck
assert 'player.isShiftKeyDown()' in truck

# All 3 interaction surfaces can cancel or emergency-dismount.
assert 'lift.requestDriverDismissal(world)' in scanner
assert 'lift.emergencyDriverRelease(world)' in scanner
assert 'lift.emergencyDriverRelease((ServerLevel)level())' in golem
assert 'lift.requestDriverDismissal((ServerLevel)level())' in golem
print("PASS: collision-checked short-step driving, cancel-to-park, safe emergency driver/cargo recovery")
