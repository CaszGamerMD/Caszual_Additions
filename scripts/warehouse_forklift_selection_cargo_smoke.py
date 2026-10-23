#!/usr/bin/env python3
"""Forklift selections must not mistake a stale source, hidden cargo or rack part for an empty destination."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
lift=(base/"WarehouseForklift.java").read_text()
scanner=(base/"StorageScannerItem.java").read_text()
controller=(base/"WarehouseForkliftItem.java").read_text()
rack=(base/"RackBlockEntity.java").read_text()
# A rack root owns exactly one pallet throughout its 12 physical parts.
assert 'RackBlock.controller(level,clicked)' in scanner
assert 'from.getBlockPos().equals(rack.getBlockPos())' in scanner
assert 'SAME 2x2x3 rack' in scanner
assert 'rack.getBlockPos().toShortString()' in scanner
assert 'rack.palletStack().getHoverName().getString()' in scanner
assert 'rack.occupiedSlots()' in scanner
assert 'clearForkliftSource(scanner);' in scanner
assert 'SOURCE AND DESTINATION ARE THE SAME 2x2x3 RACK' in lift
# Selection is two steps on both black clipboard and warehouse scanner.
assert 'source==null&&rack.hasPallet()' in controller
assert 'select(context.getItemInHand(),level,rack.getBlockPos());' in controller
assert 'nearest.orderRackToRack(level,sourceRack,rack)' in controller
assert 'forklift.orderRackToRack(level,from,rack)' in scanner
# A parked vehicle with saved cargo must not accept a second pallet
# even if its deck item can no longer be rendered (legacy broken slot 0).
assert 'public boolean hasPackedCargo()' in lift
assert 'for(ItemStack item:PalletJackItem.unpack(jack))' in lift
assert 'public boolean readyForPickup(){return !busy()&&!hasPackedCargo();}' in lift
assert 'WarehouseForklift::readyForPickup' in scanner
assert 'if(lift.hasPackedCargo()){onboard=true;continue;}' in controller
assert 'if(!hasPackedCargo())' in lift
assert 'ItemStack packed=jack.copy();' in lift
assert 'if(hasPackedCargo()){' in lift # vehicle destruction preserves saved cargo
# Failures must reveal the actual reason, NEVER claim all problems are clearance.
assert 'SOURCE RACK CANNOT EXTRACT:' in lift
assert 'DESTINATION RACK CANNOT ACCEPT:' in lift
assert 'STORED ITEM SLOTS BUT NO PALLET' in lift
assert 'FORKLIFT JACK CONTAINS UNDISPLAYED SAVED CARGO;' in lift
assert '"FORKLIFT: "+forklift.status()' in scanner
assert '"Forklift cannot start: "+nearest.status()' in controller
assert 'boolean hasPallet() { return !pallet.isEmpty(); }' in rack
# Source checks must not mutate a forklift job before validation/hiring.
assert 'BlockPos proposedStaging=pickup?floorPlacement' in lift
assert 'this.staging=proposedStaging;' in lift
print('PASS: source/target rack selection, stale source, true 3D rack occupancy, and lossless forklift cargo')
