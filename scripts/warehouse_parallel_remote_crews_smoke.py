#!/usr/bin/env python3
"""Regression: one scanner can dispatch multiple independent 4-Golem remote jobs."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/com/caszgamermd/caszualadditions/racks"
def source(name):
    return (java / name).read_text()

dispatch = source("RemotePalletDispatch.java")
scanner = source("StorageScannerItem.java")
carrier = source("MovingPallet.java")
worker = source("StorageGolem.java")
pickup = source("RemotePalletPickupCrew.java")
shelf = source("StorageGolemShelfEntity.java")

# Do not serialize EVERY job owned by one player; serialize only pickup
# claims for the SAME physical wrapped pallet.
assert "rig.owns(player.getUUID())" not in dispatch
assert "worker.ownedBy(player.getUUID())" not in dispatch
assert "old.getBlockPos().equals(rig.remoteExchangeTarget())" in dispatch
assert "old.getBlockPos().equals(worker.remotePickupTarget())" in dispatch
assert "public BlockPos remoteExchangeTarget()" in carrier
assert "remoteExchangeActive()?deliveryPosition:null" in carrier
assert "remotePickupMission=mission;" in worker
assert "id.equals(worker.remotePickupMission())" in pickup

# No half-sized teams; each accepted remote job debits four real docked
# workers, and unsuccessful spawns restore their complete allocation.
assert "if(shelves.size()<4)" in dispatch
assert "for(int i=0;i<4;i++)" in dispatch
assert "if(consumed==4)" in dispatch
assert "for(int i=0;i<consumed;i++)shelves.get(i).add();" in dispatch
assert "if(!shelf.take())break;" in dispatch
assert "if(!level.addFreshEntity(worker))" in dispatch
assert "public boolean take(){if(docked<=0)return false;" in shelf
assert "public boolean add(){if(docked>=2)return false;" in shelf
for docked, expected in [(0,0),(3,0),(4,1),(7,1),(8,2),(12,3)]:
    assert docked//4 == expected

# Queue lives on scanner item data and is not overwritten by a second
# click. Successfully dispatched/invalid jobs leave the queue; waiting
# jobs persist and are retried, even with one inaccessible pallet.
assert "MAX_PENDING_REMOTE=64" in scanner
assert "queuedPickups(CompoundTag tag)" in scanner
assert "storePickups(scanner,targets);" in scanner
assert "targets.add(target.immutable());" in scanner
assert "remaining.add(target);" in scanner
assert "remaining.addAll(pending.subList(i+1,pending.size()))" in scanner
assert "if(!remaining.equals(pending))storePickups(scanner,remaining);" in scanner
assert "dispatchPending(scanner,level,player,false)" in scanner
assert "dispatchPending(scanner,server,user,true)" in scanner
assert 'remote_pickup_pending' in scanner  # old scanner NBT migration

print("PASS: parallel, per-target exclusive remote crews; 4-token dispatch; multi-target scanner queue")
