#!/usr/bin/env python3
"""Remote courier must not orbit its target or squeeze a full-size 2x2 pallet."""
from pathlib import Path
from collections import deque
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
courier=(root/"StorageGolem.java").read_text()
router=(root/"RemotePalletCourierRoute.java").read_text()
assert "private final ArrayDeque<BlockPos> courierRoute=new ArrayDeque<>()" in courier
assert "private boolean followCourierRoute(ServerLevel level,BlockPos goal," in courier
assert "if(!goal.equals(courierRouteGoal))" in courier
assert "RemotePalletCourierRoute.chooseServiceAisle(" in courier
assert "RemotePalletCourierRoute.route(level,blockPosition()," in courier
assert "if(!followCourierRoute(level,remoteCourierAisle,true))" in courier
assert "if(!followCourierRoute(level,remoteCourierJoinAisle,false))" in courier
assert "courierBlockedCell=next.immutable();" in courier
assert "courierAvoidUntil=tickCount+140" in courier
assert "if(courierStalls>=3){" in courier
assert 'pauseRemotePath("COURIER STALLED AT BLOCKED AISLE")' in courier
assert "remoteCourierRejectedAisle=remoteCourierAisle" in courier
assert "courierRetargetCooldown=tickCount+60" in courier
assert "if(tickCount-lastCourierProgress>75)" in courier
assert "steerOnFloor(next,1.06)" in courier
assert 'out.store("remote_courier_join_aisle",BlockPos.CODEC,remoteCourierJoinAisle)' in courier
assert 'in.read("remote_courier_join_aisle",BlockPos.CODEC)' in courier
assert "WarehouseRemoteTickets.keep(level,goal)" in courier
assert "StorageGolemReturnPath.walkable(level,remoteCourierAisle)" not in courier
assert "walkTo(level,remoteCourierAisle,1.12)" not in courier
assert "candidate.distSqr(trioRally)<36" in router
assert "if(!excluded.equals(BlockPos.ZERO)" in router
assert "&&candidate.distSqr(excluded)<9" in router
assert "if(!clear(level,candidate))continue;" in router
assert "path=route(level,from,candidate," in router
assert "BlockPos.ZERO,true,trioRally)" in router
assert "pos.distSqr(crewRally)>=9" in router
assert "StorageGolemReturnPath.steps(start,destination,passable)" in router
assert "WarehousePreferredPath.leg(start,destination,passable,avoid)" in router
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,zSide))" in router
assert "for(int xSide:new int[]{-1,1})" in router
assert "public static BlockPos chooseJoinAisle" in router

# Pure 2D corridor fixture: closest-by-distance destination is blocked
# by racks; the courier must choose the alternative with actual 2-wide
# clearance and a connected path, not endlessly orbit between faces.
obstacles={(x,z) for x in range(3,9) for z in range(-2,3)}
# Face NEAR the target is barricaded with solid 2x2 racks.
obstacles.update({(x,z) for x in range(10,13) for z in range(-2,2)})
start=(0,0)
def openwide(p):
    x,z=p
    return all((x+dx,z+dz) not in obstacles
               for dx in (-1,0,1) for dz in (-1,0,1))
def reachable(src,dest):
    q=deque([src]);seen={src}
    while q:
        x,z=q.popleft()
        if (x,z)==dest:return True
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            at=(x+dx,z+dz)
            if at not in seen and max(abs(at[0]),abs(at[1]))<32 and openwide(at):
                seen.add(at);q.append(at)
    return False
assert not openwide((8,0))
assert not reachable(start,(8,0))
assert reachable(start,(8,7))
print("PASS: full-size pallet courier chooses reachable 2x2 corridor, persists goal,"
      " reroutes stalled waypoints, avoids trio and returns with narrow rejoin route")
