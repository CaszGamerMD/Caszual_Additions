#!/usr/bin/env python3
"""Forklift pallet must be entirely ahead of cab and face transfers first."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks/WarehouseForklift.java"
render=root/"src/client/java/com/caszgamermd/caszualadditions/racks/WarehouseForkliftRenderer.java"
model=root/"src/client/java/com/caszgamermd/caszualadditions/racks/WarehouseForkliftModel.java"
truck=main.read_text()
renderer=render.read_text()
geometry=model.read_text()
# Transform used by the actual model: front = local -Z.
assert 'pose.mulPose(Axis.YP.rotationDegrees(180-state.bodyRot));' in renderer
assert 'pose.translate(-.90f,.32f+state.mastHeight,-3.72f);' in renderer
assert 'state.hasPallet' in renderer
# 1.8-deep pallet spans -3.72 to -1.92 (in FRONT of -1.4 mast).
assert '-3.72' in renderer
assert 'addBox(-10,-2,-64,4,3,41)' in geometry
assert 'addBox(6,-2,-64,4,3,41)' in geometry
assert 'carriage.y=22f-height*16f;' in geometry
# Physical motor must not translate sideways through turns.
assert 'private boolean turnForksToward(double x,double z)' in truck
assert 'float wanted=(float)Math.toDegrees(Math.atan2(-dx,dz));' in truck
assert 'float error=Mth.wrapDegrees(wanted-getYRot());' in truck
assert 'float turn=Mth.clamp(error,-14f,14f);' in truck
assert 'setYBodyRot(yaw);' in truck
assert 'setYHeadRot(yaw);' in truck
assert 'if(!turnForksToward(x,z)){' in truck
assert 'private boolean alignForTransfer(BlockPos load)' in truck
assert 'if(!alignForTransfer(source)){' in truck
assert 'BlockPos unload=groundDestination?staging:target;' in truck
assert 'if(!alignForTransfer(unload)){' in truck
assert 'status="TURNING FORKS TOWARD SOURCE PALLET";' in truck
assert 'status="TURNING FORKS TOWARD DROP-OFF";' in truck
# Previous forklift speed and low-travel fork height remain in this build.
assert 'DRIVE_BLOCKS_PER_TICK=0.20D' in truck
assert 'boolean liftingAtDestination=job==3&&!groundDestination&&animation>0;' in truck
assert 'getY()+.62' in truck
print("PASS: full cargo forward on long forks; travel yaw and loading/unloading alignment")
