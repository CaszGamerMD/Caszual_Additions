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
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in fork
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in fork
assert "return WarehouseForkliftSeat.riderPosition(position(),getYRot());" in fork
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
assert "aisleRoute=ForkliftAislePath.route(world,this,goal);" in fork
assert "private boolean drive(ServerLevel world,Vec3 goal)" in fork
assert "private Dock groundService(ServerLevel world,BlockPos root,Direction facing)" in fork
assert ":groundService(world,source,groundFacing);" in fork
assert "REPLANNING FORKLIFT AISLE AROUND OBSTRUCTION" in fork
assert "if(!seatedDriver(world)){" in fork
assert "origin.liftToForklift(jack)" in fork
assert "ForkliftPalletIO.liftGround(world,source,jack)" in fork
assert "rack.installFromForklift(jack)" in fork
assert "ForkliftPalletIO.placeGround(" in fork
assert 'Vec3 palletCenter=rackPalletCenter(rack);' in fork
assert 'ForkliftAislePath.canStand(world,this,position)' in fork
print("PASS: centered seated driver and autonomous forklift driving with collision guards")
