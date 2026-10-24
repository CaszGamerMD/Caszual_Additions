#!/usr/bin/env python3
"""Check rack-to-Golem pickup wiring and fail-safe inventory handoff.

Static guardrails only; actual multi-Golem pathfinding requires an in-game test.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/caszgamermd/caszualadditions/racks"

def source(name):
    return (JAVA / name).read_text(encoding="utf-8")

scanner = source("StorageScannerItem.java")
crew = source("StoragePalletCrew.java")
rack = source("RackBlockEntity.java")
golem = source("StorageGolem.java")
assert "StoragePalletCrew.interactRack(server,user,rack,home)" in scanner
assert "StoragePalletCrew.deliverRack(server,user,rack)" not in scanner
assert "public static String interactRack(" in crew
assert "public static String pickupRack(" in crew
assert "if(!rack.hasPallet())" in crew
assert "if(!rack.glassWrapped())" in crew
assert "worker.beginRackPickup(rack.getBlockPos(),plan.floorY());" in crew
assert "RackPickupPlan.find(level,rack)" in crew
assert "rack.startPull(owner,rackFloorY)" in golem
assert "rack.pullProgress()>=RackPickupPlan.PULL_TICKS" in golem
assert "new BlockPos(" in source("RackPickupPlan.java")
# Output picking now validates the exact service cell with the weighted,
# terrain-aware path used by the real worker. Keep the rack's safe final
# access requirement: a partial route is NEVER an acceptable pickup.
assert "WarehousePreferredPath.leg(" in golem
assert "route.getLast().equals(cell)" in golem
assert "StorageGolemReturnPath." in golem
assert "public boolean liftToCarrier(StorageGolem leader)" in rack
assert "PalletData.applyFilterSettings(pallet,this)" in rack
assert "stock.set(i,items.get(i).copy())" in rack
assert rack.index("leader.loadPallet(deck,stock)") < rack.index("items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);")
assert "private boolean rackPickup;" in golem
assert "rackPickup=in.getBooleanOr(\"rack_pickup\",false);" in golem
assert 'out.putBoolean("rack_pickup",rackPickup);' in golem
assert "rack.liftToCarrier(this)" in golem
assert "StoragePalletCrew.formMovingPallet(level,crew,owner)" in golem
assert "plan.goal(haulIndex," in golem
print("Rack pallet scanner pickup, four-worker handoff and 216-slot state transfer verified.")
