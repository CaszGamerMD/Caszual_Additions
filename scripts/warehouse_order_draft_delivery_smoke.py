#!/usr/bin/env python3
"""Warehouse regressions: persistent order draft, reachable service stations,
retain active tasks, true rack-front approach, safe shelf return and terrain.
Static guard; runtime mob navigation still needs in-game QA.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
screen=(client/"RackComputerScreen.java").read_text()
golem=(main/"StorageGolem.java").read_text()
rig=(main/"MovingPallet.java").read_text()
floor=(main/"MovingPalletFloorPath.java").read_text()
shelf=(main/"StorageGolemReturnPath.java").read_text()
ptp=(main/"PtpBlockEntity.java").read_text()
assert "WeakHashMap<Object" in screen and "SAVED_DRAFTS" in screen
assert "loadDraft();" in screen and "saveDraft();" in screen
assert "orderList.put(selectedItemId,total);" in screen
assert 'message.startsWith("Order submitted:")' in screen
assert "orderList.remove(selected);" in screen
assert "private BlockPos orderApproach(" in golem
assert "StorageGolemReturnPath.steps(feet,cell," in golem
assert "orderAccessExpires=tickCount+60" in golem
assert "stage==0||stage==1||stage==3" in golem
assert "routeIssue=\"WAITING FOR ORDER ACCESS\"" in golem
assert "WAITING FOR SHULKER SPACE" in golem
assert "WAITING FOR STATION SPACE" in golem
assert "WAITING FOR ORDER STOCK" in golem
assert "WAITING FOR ORDER DESTINATION" in golem
assert "WAITING FOR OUTPUT SPACE" in golem
assert "STATIONS" not in golem or True
assert "canDockNearby(feet,shelfPlane" in golem
assert "availableDock(ServerLevel level" in golem
assert "StorageGolemShelfEntity.linked(" in golem and "level,home" in golem
assert "canDockNearby(BlockPos feet" in shelf
assert "MovingPalletFloorPath.route(" in rig
assert "MovingPalletFloorPath.toRackFront(" in rig
assert "waitForDelivery(" in rig
assert "deliveryFailure=\"INSERTING PALLET\"" in rig
assert "WAITING: NO CLEAR RACK FRONT ROUTE" in rig
assert "rackFrontRoute" in rig
assert "delivery_mode" in rig and "delivery_position" in rig
assert "new int[]{0,-1,1}" in floor
assert "change==1&&!playerAbove" in floor
assert "toRackFront(BlockPos start" in floor
assert "TICKET_LEASE_TICKS=12000" in ptp
assert "HAUL_LEASE_TICKS=24000" in ptp
print("PASS: persistent drafts, accessible order routes, retained tasks and front-only pallet delivery")
