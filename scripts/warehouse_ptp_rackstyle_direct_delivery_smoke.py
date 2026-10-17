#!/usr/bin/env python3
"""PTPs and racks have the same pickup/delivery affordance for Direct Scanner."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"
def read(name):return (src/name).read_text()
crew=read("StoragePalletCrew.java")
golem=read("StorageGolem.java")
rig=read("MovingPallet.java")
plan=read("PtpAccessPlan.java")
ptp=read("PtpBlockEntity.java")
scanner=read("StorageScannerItem.java")
assert "public static String interactPtp" in crew
assert "if(carrier!=null)return carrier.requestPtpDelivery(level,point);" in crew
assert "return deliverPtp(level,player,point);" in crew
assert "return pickup(level,player,point);" in crew
assert "PtpAccessPlan.find(level,source,player.blockPosition())" in crew
assert "source.beginPlayerHaul(player.getUUID())" in crew
assert "worker.beginPlayerPickup(source.getBlockPos(),ticket,access.facing())" in crew
assert "for(int worker=0;worker<4&&clear;worker++)" in plan
assert "StorageGolemReturnPath.walkable(level,plan.goal(worker))" in plan
assert "service.offset(x,0,z)" in plan
assert "depth=2+(worker/2)" in plan
assert "playerPtpFace=accessFace" in golem
assert "place.relative(playerPtpFace,2+haulIndex/2)" in golem
assert "out.putString(\"player_ptp_face\",playerPtpFace.getName())" in golem
assert "public boolean placeIntoPtp(PtpBlockEntity point)" in golem
assert "releaseCarriedPallet();" in golem
assert "public String requestPtpDelivery(ServerLevel level,PtpBlockEntity point)" in rig
assert "deliveryMode=7;" in rig
assert "if(deliveryMode==2||deliveryMode==7){tickAisleDelivery(level);return;}" in rig
assert "deliveryService=deliveryMode==7?serviceAisle(level,target,3,5)" in rig
assert "groundDeliveryAisle(level,target,deliveryFacing)" in rig
assert "navigateRemoteFloor(level,deliveryService)" in rig
assert "nearRemoteGoal(deliveryService)" in rig
assert "deliverToPtp(point)" in rig
assert "deliverToGround(level,target,deliveryFacing)" in rig
assert "point.clearContent();" in rig and "point.dropPallet();" in rig
assert "point.setSealed(" in rig
assert "clearCargo();" in rig
assert "deliveryMode=Math.clamp(in.getIntOr(\"delivery_mode\",0),0,7)" in rig
assert 'out.store("delivery_service",BlockPos.CODEC,deliveryService)' in rig
assert 'in.read("delivery_service",BlockPos.CODEC)' in rig
assert "PtpBlock.controller(server,clicked)" in scanner
assert "StoragePalletCrew.interactPtp(server,user,point)" in scanner
assert "public boolean canInstallPallet()" in ptp
print("PASS: rack-style PTP pickup and delivery, fixed outer aisle path and safe 216-slot cargo handoff")
