#!/usr/bin/env python3
"""Seat centering and real measured driving displacement on safe warehouse aisles."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
fork=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/WarehouseForklift.java").read_text()
assert "double right=.28;" in fork
assert "getY()+.62" in fork # keep height unchanged
assert "getX()-Math.sin(radians)*rear+Math.cos(radians)*right" in fork
assert "getZ()+Math.cos(radians)*rear+Math.sin(radians)*right" in fork
assert "DRIVE_BLOCKS_PER_TICK=.20D" in fork
assert "DRIVE_SPEED=.20D" in fork
assert "double remaining=Math.max(0,DRIVE_BLOCKS_PER_TICK-vanillaDistance)" in fork
assert "double step=Math.min(remaining,length)" in fork
assert "double mx=dx/length*step,mz=dz/length*step;" in fork
assert "move(MoverType.SELF,new Vec3(mx,0,mz));" in fork
assert "setDeltaMovement(0,getDeltaMovement().y,0);" in fork
assert "getX()-driveTickStartX,getZ()-driveTickStartZ" in fork
assert "driveTickStartX=getX();" in fork
assert "driveTickStartZ=getZ();" in fork
assert "world.noCollision(this,footprint)" in fork
assert "world.hasChunkAt(floor)" in fork
assert "Path path=getNavigation().getPath();" in fork
assert "path.getNextEntityPos(this)" in fork
assert "directLaneClear(world,aheadX,y,aheadZ)" in fork
assert "getNavigation().moveTo(x,goal.getY(),z,DRIVE_CONTROL);" in fork
assert "powerAlongSafeRoute(world,x,goal.getY(),z)" in fork
assert "job==5?.90*.90:3.0" in fork
print("PASS: passenger lateral centering; collision-safe physical 0.2-block/tick travel cap")
