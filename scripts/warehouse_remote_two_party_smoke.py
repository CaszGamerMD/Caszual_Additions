#!/usr/bin/env python3
"""Remote pallet exchange must retain every physical pallet and shelf worker."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
def read(p):return (main/p).read_text()
dispatch=read("RemotePalletDispatch.java")
carrier=read("MovingPallet.java")
courier=read("StorageGolem.java")
swap=read("StorageGroundPalletTransfer.java")
model=(client/"MovingPalletModel.java").read_text()
rigview=(client/"MovingPalletRenderer.java").read_text()
golemview=(client/"StorageGolemRenderer.java").read_text()
assert "carrier.splitRemoteCourier(courier)" in dispatch
assert "if(accepted&&level.addFreshEntity(courier))" in dispatch
assert "for(int i=0;i<3;i++)" in dispatch
assert "level.addFreshEntity(worker)" in dispatch
assert "level.addFreshEntity(carrier)" not in dispatch
assert "worker.beginRemotePickup(carrier.getUUID(),courier.getUUID()," in dispatch
assert "RemotePalletPickupCrew.rally(" in dispatch
assert "for(StorageGolem worker:spawned)worker.discard();" in dispatch
assert "sourceMagazine.put(fresh)" in dispatch
assert "for(int i=0;i<consumed;i++)shelves.get(i).add();" in dispatch
assert "remoteCourierId=worker.getUUID()" in carrier
assert "entityData.set(CREW_SIZE,3)" in carrier
assert "pallet=ItemStack.EMPTY;syncDisplay();" in carrier
assert "remoteCourierId=null;" in carrier
assert "entityData.set(CREW_SIZE,4)" in carrier
assert "deliveryMode=4;" in carrier
assert "deliveryMode=5;" in carrier
assert "deliveryMode=6;" in carrier
assert "deliveryMode=3;" in carrier
assert "remoteReplacementPlaced=true;" in carrier
assert "remoteAisle=remoteAisle(level,target,3,5)" in carrier
assert "remoteMoveAside=remoteAisle(level,deliveryPosition,5,7)" in carrier
assert "liftForRemoteExchange(level,target,this)" in carrier
assert "RemotePalletPickupCrew.splitLegacyRig(" in carrier
assert "public boolean prepareRecoveredRemote(" in carrier
assert "remotePickupActive()" in courier
assert "acceptRemoteLift(root,previous,oldDeck,stock)" in swap
assert "source.clearContent();" in swap
assert "PalletCargoCollisionBlock.clear(level,root,previous)" in swap
assert "getBlockState(cells[i]).canBeReplaced()" in swap
assert "carrier.confirmCourierPlacement(courier.getUUID())" in swap
assert "remoteFresh=ItemStack.EMPTY;" in courier
assert "remoteCourierPhase=1;" in courier
assert "rig.joinRemoteCourier(getUUID())" in courier
assert "discard(); // The rig already owns all FOUR shelf tokens." in courier
assert "int workerDrops=remoteCourierId==null?4:3;" in carrier
assert "courier.remoteCarrierDestroyed(level)" in carrier
assert "public void remoteCarrierDestroyed(ServerLevel level)" in courier
assert "if(!remoteFresh.isEmpty())" in courier
assert "remote_courier_rig" in courier and "remote_empty_pallet" in courier
assert "remote_courier_uuid" in carrier and "remote_replacement_placed" in carrier
assert "walkTo(level,remoteCourierAisle,1.12)" in courier
assert "WarehouseRemoteTickets.keep(level,remoteCourierTarget)" in courier
assert "corner[i].visible=i<state.crewSize;" in model
assert "state.crewSize=entity.displayedCrewSize()" in rigview
assert "state.pushingEmptyPallet" in golemview
assert "if(remoteCourier){tickRemoteCourier(level);return;}" in courier
assert "if(remoteCourier||stage==2||holdsPallet())return false;" in courier
print("PASS: three-Golem remote lift, upright one-Golem courier, saved phases, physical placement, regroup and one cargo owner")
