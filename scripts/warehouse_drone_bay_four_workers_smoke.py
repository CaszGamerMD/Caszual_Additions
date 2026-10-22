#!/usr/bin/env python3
"""Parking Bay pickup = four empty-handed Golems; REMOTE = three + fresh-pallet courier."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
load=lambda name:(src/name).read_text(encoding="utf-8")
bay=load("DroneBayNoReplacementDispatch.java")
remote=load("RemotePalletDispatch.java")
crew=load("RemotePalletPickupCrew.java")
rig=load("MovingPallet.java")
worker=load("StorageGolem.java")
# Parking Bay never consults, withdraws or creates a replacement pallet.
assert "List<StorageGolemShelfEntity> shelves=new ArrayList<>(4)" in bay
assert "shelves.size()<4" in bay and "shelves.size()==4" in bay
assert "starts.size()!=4" in bay and "for(int i=0;i<4;i++)" in bay
assert "shelves.get(3).getBlockPos()" in bay
assert "NO_COURIER" in bay and "worker.beginRemotePickup(mission,NO_COURIER" in bay
assert "empty pallet" not in bay.lower()
assert "fresh" not in bay.lower()
assert "beginRemoteCourier(" not in bay
# Fourth worker must walk to a distinct, safe rally cell: normal trio unchanged.
assert "if(workerIndex==3)" in crew
assert "int workerCount=bayPickup?4:3;" in crew
assert "if(team.size()!=workerCount)return;" in crew
assert "index<workerCount" in crew
assert "rig.setCrewName(" in crew and "for(StorageGolem member:team)" in crew
assert "DroneBayNoReplacementDispatch.NO_COURIER.equals(leader.remotePickupCourier())" in crew
assert "rig.finishNoReplacementRemotePickup()" in crew
assert "for(StorageGolem worker:team)worker.discard();" in crew
# Once loaded, the four-worker carrier returns directly without a courier.
assert 'entityData.set(CREW_SIZE,4);\n        deliveryFailure="FOUR GOLEMS RETURNING' in rig
assert "for(int i=0;i<4;i++){" in rig[rig.index("private boolean restoreWorkers"):rig.index("private boolean safePlatformFloor")]
assert 'remoteNoReplacement=in.getBooleanOr("remote_no_replacement",false);' in rig
assert "bayOnly?\"/4\":\"/3\"" in worker
# Vanilla REMOTE mode stays four Golems, one pushing the fresh deck.
assert "List<StorageGolem> identities=new ArrayList<>(4)" in remote
assert "for(int i=0;i<3;i++)" in remote
assert "worker.beginRemotePickup(carrier.getUUID(),courier.getUUID()" in remote
assert "if(accepted&&level.addFreshEntity(courier))" in remote
assert "fresh empty pallet needed" in remote
assert "carrier.splitRemoteCourier(courier)" in remote
assert "RemotePalletDispatch.request(" in load("StorageScannerItem.java")
assert "level.getEntity(leader.remotePickupCourier()) instanceof StorageGolem courier" in crew
print("PASS: BAY four independent Golems/no replacement; REMOTE 3 walkers + 1 replacement courier")
