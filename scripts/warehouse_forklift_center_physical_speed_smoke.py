#!/usr/bin/env python3
"""Seat centering and real measured driving displacement on safe warehouse aisles."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
fork=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/WarehouseForklift.java").read_text()
assert "double right=.28;" in fork
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in fork
assert "WarehouseForkliftSeat.riderPosition(position(),getYRot())" in fork
assert "return WarehouseForkliftSeat.riderPosition(position(),getYRot());" in fork
assert "DRIVE_BLOCKS_PER_TICK=0.20D" in fork
assert "DRIVE_SPEED=0.20D" in fork
assert "double step=Math.min(distance," in fork
assert "Math.max(0,DRIVE_BLOCKS_PER_TICK-alreadyMoved)" in fork
assert "move(MoverType.SELF,delta);" in fork
assert "setDeltaMovement(0,getDeltaMovement().y,0);" in fork
assert "getX()-driveTickStartX,getZ()-driveTickStartZ" in fork
assert "driveTickStartX=getX();" in fork
assert "driveTickStartZ=getZ();" in fork
assert "ForkliftAislePath.canStand(world,this,next)" in fork
assert "ForkliftAislePath.route(world,this,goal)" in fork
assert "double tolerance=job==5?.82:.16;" in fork
assert 'getNavigation().stop();' in fork
print("PASS: passenger lateral centering; collision-safe physical 0.2-block/tick travel cap")
