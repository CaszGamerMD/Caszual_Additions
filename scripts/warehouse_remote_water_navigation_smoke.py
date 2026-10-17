#!/usr/bin/env python3
"""Regression: solid frozen lakes are floors, but liquid water is NEVER a path."""
from pathlib import Path
from collections import deque
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
walk=(root/"StorageGolemReturnPath.java").read_text()
worker=(root/"StorageGolem.java").read_text()
courier=(root/"RemotePalletCourierRoute.java").read_text()
dispatch=(root/"RemotePalletDispatch.java").read_text()
team=(root/"RemotePalletPickupCrew.java").read_text()
assert "return !below.getCollisionShape(level,ground).isEmpty();" in walk
assert "!level.getFluidState(feet).isEmpty()" in walk
assert "!level.getFluidState(feet.above()).isEmpty()" in walk
assert "return level.getFluidState(feet).is(FluidTags.WATER)" not in walk
assert "BlockState floor=level().getBlockState(blockPosition().below());" in worker
for name in ("ICE","PACKED_ICE","BLUE_ICE","FROSTED_ICE"):
    assert f"net.minecraft.world.level.block.Blocks.{name}" in worker
assert "setDeltaMovement(motion.x*.58,motion.y,motion.z*.58);" in worker
assert "boolean nearby=feet.getY()==target.getY()" in worker
assert "if(route==null&&!nearby)" in worker
assert "pauseRemotePath(\"NO SAFE DRY PATH (WATER OR OBSTRUCTION)\")" in worker
assert "pauseRemotePath(\"NO CLEAR COURIER AISLE AWAY FROM PICKUP CREW\")" in worker
assert "private boolean remotePathPaused;" in worker
assert 'out.putBoolean("remote_path_paused",remotePathPaused)' in worker
assert 'remotePathPaused=in.getBooleanOr("remote_path_paused",false)' in worker
assert "public boolean resumeRemotePath()" in worker
assert "player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)" in worker
assert "if(remotePathPaused){" in worker
assert "return !remotePathPaused&&routeDistanceSq(level,goal)<1.30;" in worker
assert "if(remotePickup){tickRemotePickup(level);return;}" in worker
assert "if(remoteCourier){tickRemoteCourier(level);return;}" in worker
assert "worker.atRemotePickupGoal(level,goal)" in team
assert "BlockPos start=workerStarts.get(i);" in dispatch
assert "if(courierStart==null)" in dispatch
assert "crewRally.equals(BlockPos.ZERO)||!pushing" in courier
assert "pos.distSqr(crewRally)>=9" in courier

# Synthetic frozen canal: same shape as liquid corridor, but dry ice
# must be traversable and an unfrozen gap must stop the trip.
def reachable(floors, start, goal):
    q=deque([start]);seen={start}
    while q:
        p=q.popleft()
        if p==goal:return True
        for dx,dz in ((0,1),(0,-1),(1,0),(-1,0)):
            n=(p[0]+dx,p[1]+dz)
            if n in floors and n not in seen:
                seen.add(n);q.append(n)
    return False
ice={(x,z) for x in range(8) for z in range(5)}
assert reachable(ice,(0,2),(7,2))
for x in range(5):
    ice.remove((3,x)) # Entire cross-section is liquid water
assert not reachable(ice,(0,2),(7,2))
print("PASS: ice and stone stay traversable; water pauses workers for scanner rescue")
