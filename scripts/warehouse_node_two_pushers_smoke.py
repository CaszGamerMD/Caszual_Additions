#!/usr/bin/env python3
"""Two-node pusher dispatch stays separate from both four-Golem tasks."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
push=(base/"PalletNodePushCrew.java").read_text()
worker=(base/"StorageGolem.java").read_text()
remote=(base/"RemotePalletDispatch.java").read_text()
bay=(base/"DroneBayNoReplacementDispatch.java").read_text()
assert "new ArrayList<>(2)" in push and "shelves.size()!=2" in push
assert "for(int i=0;i<2;i++)" in push
assert "golem.beginNodePush(mission,node,vehicle,goals[i],loading)" in push
assert "StorageGolemReturnPath.walkable(level,a)" in push
assert 'Class.forName("com.caszgamermd.palletsplus.transport.NodePushBridge")' in push
assert "if(!transferred)return;" in push
assert "WarehouseAislePriority.stagedOutputRacks(level,computer)" in push
assert "RackPickupPlan.find(level,rack)" in push
assert "rack.startPull(first.nodePushMission(),first.nodePushSourceFloor())" in push
assert "rack.liftToCarrier(first)" in push
assert "first.nodePushDepartSource()" in push and "partner.nodePushDepartSource()" in push
assert 'bridge.getMethod("completeFromCrew"' in push
assert "first.finishNodePush()" in push and "partner.finishNodePush()" in push
assert "private boolean nodePush;" in worker
assert "nodePush=in.getBooleanOr(" in worker and 'out.putBoolean("node_push",nodePush)' in worker
assert "tickNodePush(level);return;" in worker
assert 'out.store("node_push_source",BlockPos.CODEC,nodePushSource)' in worker
assert 'nodePushSource=in.read("node_push_source",BlockPos.CODEC)' in worker
assert "leader.relinquishPalletToRig()" not in push  # only the transport bridge commits the transfer
assert 'label="TRANSFER PUSH "+(haulIndex+1)+"/2' in worker
assert "shelves.size()<4" in bay and "for(int i=0;i<4;i++)" in bay
assert "List<StorageGolem> identities=new ArrayList<>(4)" in remote
assert "worker.beginRemotePickup(carrier.getUUID(),courier.getUUID()" in remote
print("PASS: exactly two persistent pallet node pushers; four-Golem INPUT/REMOTE tasks unchanged")
