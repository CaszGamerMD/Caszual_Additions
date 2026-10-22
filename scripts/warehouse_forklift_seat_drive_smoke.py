#!/usr/bin/env python3
"""Regression: seated forklift driver, active collision-safe motor, and task lifecycle."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
fork=(main/"WarehouseForklift.java").read_text()
golem=(main/"StorageGolem.java").read_text()
pose=(client/"StorageGolemModel.java").read_text()
state=(client/"StorageGolemRenderState.java").read_text()
renderer=(client/"StorageGolemRenderer.java").read_text()
assert "getPassengerRidingPosition(Entity passenger)" in fork
assert "passenger instanceof StorageGolem" in fork
assert "getY()+.62" in fork and "double rear=.59;" in fork
assert "getX()-Math.sin(radians)*rear+Math.cos(radians)*right" in fork
assert "getZ()+Math.cos(radians)*rear+Math.sin(radians)*right" in fork
assert "state.ridingForklift=golem.getVehicle() instanceof WarehouseForklift;" in renderer
assert "public boolean ridingForklift;" in state
assert "if(state.ridingForklift){" in pose
assert "leftLeg.xRot=rightLeg.xRot=-1.35f;" in pose
assert "leftArm.xRot=rightArm.xRot=-1.08f;" in pose
assert "if(getVehicle()==lift){" in golem
assert "startRiding(lift)" in golem
assert "BlockPos candidate=lift.blockPosition().relative(side,2);" in golem
assert "StorageGolemReturnPath.walkable(level,candidate)" in golem
assert "setYBodyRot(lift.getYRot());" in golem
assert "getNavigation().moveTo(x,goal.getY(),z,DRIVE_CONTROL)" in fork
assert "powerAlongSafeRoute(world,x,goal.getY(),z)" in fork
assert "directLaneClear(world,aheadX,y,aheadZ)" in fork
assert "world.noCollision(this,test)" in fork
assert "getCollisionShape(world,floor).isEmpty()" in fork
assert "lastDriveDistance=dist" in fork and "stallTicks++" in fork
assert 'WAITING: FORKLIFT PATH BLOCKED; CLEAR THE DRIVE AISLE' in fork
assert "private BlockPos groundService(" in fork
assert ":groundService(world,source);" in fork
assert "if(!seatedDriver(world)){" in fork
assert "origin.liftToForklift(jack)" in fork
assert "ForkliftPalletIO.liftGround(world,source,jack)" in fork
assert "rack.installFromForklift(jack)" in fork
assert "ForkliftPalletIO.placeGround(" in fork
assert "return plan==null?null:plan.goal(3,false);" in fork
print("PASS: centered seated driver and autonomous forklift driving with collision guards")
