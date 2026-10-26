#!/usr/bin/env python3
"""Verify unified forward-facing cargo and four-corner slope gait.

Pure source checks guard safety invariants; Java compilation and actual game
terrain handling are validated separately.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
rig=(main/"MovingPallet.java").read_text()
ground=(main/"MovingPalletTerrain.java").read_text()
render=(client/"MovingPalletRenderer.java").read_text()
state=(client/"MovingPalletRenderState.java").read_text()
model=(client/"MovingPalletModel.java").read_text()
assert "private void updateCrewFacingAndFeet(ServerLevel level)" in rig
assert "updateCrewFacingAndFeet(server);" in rig
assert "Math.atan2(-dx,dz)" in rig
assert "setYRot(yaw+Math.clamp(diff,-12f,12f))" in rig
assert "setYBodyRot(getYRot())" in rig
for key in ("TERRAIN_0","TERRAIN_1","TERRAIN_2","TERRAIN_3","TERRAIN_SUPPORT"):
    assert key in rig
assert "entityData.set(TERRAIN_SUPPORT,ground.mask())" in rig
assert "level.hasChunkAt(blockPosition())" in rig
assert "TERRAIN_KEYS[index]" in rig
assert "CORNER" not in ground or "FOOT_OFFSET=.875" in ground
assert "ground=walkableSurface" in ground
assert "getCollisionShape(level,cell)" in ground
assert "shape.toAabbs()" in ground
assert "localX>=box.minX" in ground
assert "state.platformPitch=-slope[0]" in render
assert "Integer.bitCount(mask)==3" in ground
assert "offsets[i]=waiting" in ground
assert "Integer.bitCount(mask)<3" in ground
assert "Math.atan2(dz,1.75)" in ground
assert "Math.atan2(dx,1.75)" in ground
assert "getXsize()" not in ground
assert "state.bodyRot=180.0f" not in render
assert "180.0f-state.bodyRot" in render
assert "state.footHeight[i]=entity.cornerTerrainHeight(i)" in render
assert "MovingPalletTerrain.tilt(state.footHeight,state.groundedMask)" in render
assert "Axis.XP.rotation(state.platformPitch*levelOff)" in render
assert "Axis.ZP.rotation(-state.platformRoll*levelOff)" in render
assert "state.groundedMask&(1<<i)" in model
assert "float footGait=supported?amount:0f" in model
assert "corner[i].y=Mth.clamp(-elevation*16f,-14f,21f)" in model
assert "state.crewSize" in model
assert "public final float[] footHeight=new float[4]" in state
assert "updateCarrySurfaceSpeed(server);" in rig
assert "if(isDelivering()){tickDelivery(server);return;}" in rig
assert "waitingForPlayer" in rig
print("PASS: unified forward gait, bounded roll/pitch, four ground probes, idle unsupported foot; cargo/follow path intact")

