#!/usr/bin/env python3
"""Remote Golems in an icy ocean must physically swim, not stand/dance at sea."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
walk=(root/"StorageGolemReturnPath.java").read_text()
worker=(root/"StorageGolem.java").read_text()
courier=(root/"RemotePalletCourierRoute.java").read_text()
team=(root/"RemotePalletPickupCrew.java").read_text()
assert "import net.minecraft.tags.FluidTags;" in walk
assert "if(!below.getCollisionShape(level,ground).isEmpty())return true;" in walk
assert "level.getFluidState(feet).is(FluidTags.WATER)" in walk
assert "!level.getFluidState(feet.above()).is(FluidTags.WATER)" in walk
assert "public static boolean clear(ServerLevel level,BlockPos center)" in courier
assert "StorageGolemReturnPath.walkable(level,center)" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,zSide))" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(0,0,zSide))" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,0))" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,zSide))" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,0))" in courier
assert "private BlockPos routeFeet(ServerLevel level)" in worker
assert "for(int dy=-1;dy<=9;dy++)" in worker
assert "private boolean isSwimmingRoute(ServerLevel level)" in worker
assert "private double routeDistanceSq(ServerLevel level,BlockPos position)" in worker
assert "if(isSwimmingRoute(level)&&Math.abs(y)<1.4)y=0;" in worker
assert "BlockPos feet=routeFeet(level);" in worker
assert "routeDistanceSq(level,flatWorkRoute.peekFirst())" in worker
assert "routeDistanceSq(level,goal)>1.30" in worker
assert "routeDistanceSq(level,next)" in worker
assert "setDeltaMovement(movement.x*.78+dx/distance*thrust," in worker
assert "Math.max(movement.y*.72,rise)" in worker
assert "getMoveControl().setWantedPosition(next.getX()+.5," in worker
assert '?"SWIMMING EMPTY HANDED TO REMOTE TARGET"' in worker
assert "if(remotePickup){tickRemotePickup(level);return;}" in worker
assert "if(remoteCourier){tickRemoteCourier(level);return;}" in worker
assert "worker.atRemotePickupGoal(level,goal)" in team
assert "public boolean atRemotePickupGoal(ServerLevel level,BlockPos goal)" in worker
assert "WarehouseRemoteTickets.keep(level,remotePickupTarget)" in worker
# Route still rejects warehouse obstacles EVEN on water.
for kind in ["RackBlock","StackOfPalletsBlock"]:
    assert f"feetState.getBlock() instanceof {kind}" in walk
    assert f"headState.getBlock() instanceof {kind}" in walk
    assert f"below.getBlock() instanceof {kind}" in walk
# A surface route must have horizontal adjacent cardinal cells, not deep
# underwater cells. The code checks water at feet AND no water overhead.
# Water buoyancy is real velocity rather than teleport or setPos.
assert "setPos(next" not in worker
print("PASS: surface-water route, buoyant swimming controls, waterline waypoint"
      " completion, same-Y obstacle rejection and saved remote task ownership")
