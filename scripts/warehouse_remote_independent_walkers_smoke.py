#!/usr/bin/env python3
"""Remote trio must be three actual independent mobs until the moment of lift."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
def read(f):return (src/f).read_text()
dispatch=read("RemotePalletDispatch.java")
worker=read("StorageGolem.java")
rally=read("RemotePalletPickupCrew.java")
rig=read("MovingPallet.java")
transfer=read("StorageGroundPalletTransfer.java")
# At DISPATCH, absolutely NO shared 3-model carrier entity is added.
assert "level.addFreshEntity(carrier)" not in dispatch
assert "List<StorageGolem> identities=new ArrayList<>(4)" in dispatch
assert "for(int i=0;i<3;i++)" in dispatch
assert "worker.beginRemotePickup(carrier.getUUID(),courier.getUUID()" in dispatch
assert "level.addFreshEntity(worker)" in dispatch
assert "if(accepted&&level.addFreshEntity(courier))" in dispatch
assert "for(StorageGolem worker:spawned)worker.discard()" in dispatch
assert "sourceMagazine.put(fresh)" in dispatch
assert "for(int i=0;i<consumed;i++)shelves.get(i).add()" in dispatch
assert "worker.remotePickupActive()" in dispatch
assert "RemotePalletPickupCrew.rally(" in dispatch
# Each real worker has persistent UUID, target, shelf, approach and phase.
assert "private boolean remotePickup;" in worker
assert "private UUID remotePickupMission,remotePickupCourier;" in worker
assert "void beginRemotePickup(" in worker
assert "remotePickupWarehouse" in worker and "remotePickupRally" in worker
assert 'out.putBoolean("remote_pickup_worker",remotePickup);' in worker
assert 'remotePickup=in.getBooleanOr("remote_pickup_worker",false)' in worker
assert "if(remotePickup){tickRemotePickup(level);return;}" in worker
assert "walkTo(level,goal,1.12)" in worker
assert "leader.remotePickupIndex()!=0" in rally
assert "threeWideAisle(level,aisle)" in rally
assert "worker.distanceToSqr(goal.getX()+.5" in rally
assert "StorageGroundPalletTransfer.wrappedController" in rally
# Shared rig first exists AT the wrapped target; only then 216 slots move.
assert "rig.setUUID(id);" in rally
assert "level.addFreshEntity(rig)" in rally
assert "StorageGroundPalletTransfer.liftForRemoteExchange(level," in rally
assert "for(StorageGolem worker:trio)worker.discard();" in rally
assert "public boolean prepareRecoveredRemote(" in rig
assert "remoteCourierId=courierId" in rig
assert "deliveryMode=4" in rig
assert "source.clearContent();" in transfer
assert "PalletCargoCollisionBlock.clear(level,root,previous)" in transfer
assert "if(!StorageGroundPalletTransfer.liftForRemoteExchange(" in rally
# Courier MOVES during the trio's outbound walk, even if rig isn't born.
assert "MovingPallet rig=other instanceof MovingPallet carrier?carrier:null;" in worker
assert "if(rig==null||!rig.readyForCourierPlacement())" in worker
assert "walkTo(level,remoteCourierAisle,1.12)" in worker
# Saved older rigid pickup rigs are split into three individual workers.
assert "RemotePalletPickupCrew.splitLegacyRig(" in rig
assert "golem.beginRemotePickup(rig.getUUID(),courier" in rally
assert "rig.discard();" in rally
print("PASS: independent outbound 3 Golems; saved missions, physical rendezvous,"
      " single ownership at lift, autonomous courier and legacy migration")
