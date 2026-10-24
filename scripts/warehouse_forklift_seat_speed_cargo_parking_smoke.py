#!/usr/bin/env python3
"""Ensure forklift operators, pallets and parking follow physical task order."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
racks=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
lift=(racks/"WarehouseForklift.java").read_text()
view=(client/"WarehouseForkliftRenderer.java").read_text()
state=(client/"WarehouseForkliftRenderState.java").read_text()
golem=(racks/"StorageGolem.java").read_text()
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in lift
assert "DRIVE_SPEED=0.20D;" in lift # 4 blocks per 20t second
assert ".add(Attributes.MOVEMENT_SPEED,DRIVE_SPEED)" in lift
assert "List<Vec3> raw=ForkliftAislePath.route(world,this,goal);" in lift
assert "aisleRoute=ForkliftAislePath.smooth(world,this,raw," in lift
assert "ForkliftAislePath.canStand(world,this,next)" in lift
assert "boolean liftingAtSource=retrieving&&sourceRackStage>=1" in lift
assert "boolean liftingAtDestination=!groundDestination" in lift
assert 'status="LOWERING PALLET AFTER COMPLETE RACK WITHDRAWAL"' in lift
assert 'status=groundDestination?"LOWERING PALLET ONTO GROUND"' in lift
assert ':"PLACING PALLET INTO RACK AT PRESET MAST HEIGHT"' in lift
assert "if(!enterRackSafely(world,rack,false))return;" in lift
assert "if(!exitRackSafely(world,rack,false))return;" in lift
assert "entityData.set(CARGO_CRATES,Math.min(27,visible));" in lift
assert "state.visibleCargo=deck.isEmpty()?0:vehicle.displayedCargoCrates();" in view
assert "state.hasPallet=!deck.isEmpty();" in view
assert "pose.translate(-.90f,.32f+state.mastHeight,-3.72f);" in view
assert "0,.10f,.03f+board*.43f,1.80f,.12f,.42f" in view
assert "state.crate" in view and "state.glass" in view
assert "state.boards[board]" in view and "PalletData.plasticColor(deck)" in view
assert "public boolean hasPallet;" in state
assert "dismissAfterParking=true;" in lift
assert "job=5;animation=0;stallTicks=0;" in lift
assert "if(job!=0||!atParkingBay()||driver==null)return false;" in lift
assert "public String emergencyDriverRelease(ServerLevel world)" in lift
assert "public String recoverStuckCargo(Player player)" in lift
assert 'status="WAITING: FORKLIFT PARKING BAY NOT SET"' in lift
# A BlockPos parking marker denotes the forklift's FLOOR, not the
# geometric center of a cube. Y+0.5 failed the 0.25 vertical tolerance
# in ForkliftAislePath.clearSegment, silently immobilizing home routing.
assert "return drive(world,new Vec3(goal.getX()+.5,goal.getY()," in lift
assert "goal.getZ()+.5));" in lift
assert "drive(world,Vec3.atCenterOf(goal))" not in lift
path=(racks/"ForkliftAislePath.java").read_text()
assert "if(Math.abs(from.y-to.y)>.25)return false;" in path
floor_y=68
assert abs((floor_y+.5)-floor_y)>.25
assert abs(floor_y-floor_y)<=.25
assert '!status.startsWith("WAITING:")' in lift
assert '!status.startsWith("REPLANNING")' in lift
assert "if(drive(world,parking)){" in lift
assert "golem.finishForkliftDriving()" in lift
assert "getVehicle()==lift" in golem
print("PASS: lower cab seat, 4m/s, full-sized visible low pallet, rise only at racks, driver parks first")
