#!/usr/bin/env python3
"""Remote pallet extraction: tight aisles, real motion, crew recall and exact stock."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/"src/main/java/com/caszgamermd/caszualadditions/racks"
crew=(src/"RemotePalletPickupCrew.java").read_text()
worker=(src/"StorageGolem.java").read_text()
rig=(src/"MovingPallet.java").read_text()
transfer=(src/"StorageGroundPalletTransfer.java").read_text()
shelves=(src/"StoragePalletCrew.java").read_text()
assert "for(int gap=1;gap<=5;gap++)" in crew
assert "compactAisle(level,anchor,target)" in crew
assert "WarehousePreferredPath.leg(from,anchor," in crew
assert "int width=workerIndex%2;" in crew
assert "int depth=workerIndex/2;" in crew
assert "Direction across=(outward==Direction.NORTH||outward==Direction.EAST)" in crew
assert "outward.getClockWise():outward.getCounterClockWise()" in crew
assert "individualGoal(level,aisle,leader.remotePickupTarget(),index)" in crew
assert "if(!compactAisle(level,aisle,leader.remotePickupTarget()))" in crew
assert "BlockPos[] footprint=bounds(leader.remotePickupTarget(),oldState)" in crew
assert "rig.setPos((footprint[0].getX()+footprint[1].getX()+1)*.5," in crew
assert "rig.setPos(aisle.getX()+.5" not in crew
assert "Direction pullingFace=face(level,aisle,leader.remotePickupTarget())" in crew
assert "rig.beginRemotePullout(pullingFace);" in crew
assert "if(!StorageGroundPalletTransfer.liftForRemoteExchange(" in crew
assert "source.clearContent();" in transfer
assert "PalletCargoCollisionBlock.clear(level,root,previous);" in transfer

# Actual loaded pallet slides in SMALL collision-checked increments; a
# rig never jumps from pallet footprint into a distant assembly area.
assert "private boolean canPullLoaded(ServerLevel level,Vec3 candidate)" in rig
assert "new double[]{-.89,.89}" in rig
assert "for(int dy=1;dy<=2;dy++)" in rig
assert "!level.getFluidState(overhead).isEmpty()" in rig
assert "StorageGolemReturnPath.walkable(level,feet)" in rig
assert "level.noCollision(this,getBoundingBox().move(delta))" in rig
assert "move(MoverType.SELF,delta);" in rig
assert "remotePullSide.getStepX()*.105" in rig
assert "remotePullSide.getStepZ()*.105" in rig
assert 'out.putString("remote_pull_side",remotePullSide.getName())' in rig
assert 'in.getStringOr("remote_pull_side","")' in rig
assert "if(remoteFootprintClear()){" in rig
assert "if(!finishNoReplacementRemotePickup())" in rig
assert "if(remotePullSide!=null){" in rig
assert "if(pullLoadedOneStep(level)){" in rig
assert "STRAIGHT EXTRACTION BLOCKED; FINDING SAFE DETOUR" in rig
assert "rig.finishNoReplacementRemotePickup()" not in crew

# Remote crew recall must break its mission assignment FIRST, not leave a
# worker stuck running tickRemotePickup ahead of return-home logic.
assert "if(remotePickup||remoteCourier)" in worker
assert "return RemotePalletPickupCrew.abortBeforeLift(level," in worker
assert "void finishAbortedRemotePickup(ServerLevel level)" in worker
assert "if(remoteCourier&&!remoteFresh.isEmpty()){" in worker
assert "remoteFresh.copy()" in worker
assert "remotePickup=false;" in worker and "remoteCourier=false;" in worker
assert 'routeIssue="REMOTE PICKUP CANCELLED; RETURNING TO SHELF";' in worker
assert "if(!nodePush&&!forkliftDriver&&!forkliftEscort&&!builderPalletJob)" in worker
assert "checkPendingRecall(level)" in worker
assert "if(level.getEntity(mission) instanceof MovingPallet)return false;" in crew
assert "worker.finishAbortedRemotePickup(level)" in crew or "member.finishAbortedRemotePickup(level)" in crew
assert "shelf.requestRecall()" in shelves
assert "if(worker.recallToShelf(level))recalled++;" in shelves
assert "&&!(remotePickup&&remotePickupMission!=null" in worker
assert "&&(groundPickup||rackPickup||remotePickup);" in worker
assert "routeDistanceSq(level,goal)<.16" in worker

# Geometric fixture: 2-wide pallet-facing corridor has FOUR safe staging
# positions but cannot provide the old 3x3 rally while pallet stays put.
pallet={(x,z) for x in (0,1) for z in (0,1)}
def goals(face):
    anchor,forward,across={
        "N":((0,-1),(0,-1),(1,0)),
        "E":((2,0),(1,0),(0,1)),
        "S":((0,2),(0,1),(1,0)),
        "W":((-1,0),(-1,0),(0,1)),
    }[face]
    return [(anchor[0]+(i//2)*forward[0]+(i%2)*across[0],
             anchor[1]+(i//2)*forward[1]+(i%2)*across[1])
            for i in range(4)]
for face in "NESW":
    cells=goals(face)
    assert len(set(cells))==4
    assert all(p not in pallet for p in cells)
    assert all(abs(p[0]-cells[0][0])+abs(p[1]-cells[0][1])<=2
               for p in cells)
    cx,cz=cells[0]
    assert any((cx+x,cz+z) in pallet
               for x in (-1,0,1) for z in (-1,0,1))
print("PASS: compact pallet-face pullers, physically swept cargo, cargo-safe whole-mission recall")
