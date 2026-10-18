#!/usr/bin/env python3
"""Regression checks for reverse rack insertion, rigid bots and 3D wrapper."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
assets=root/"src/main/resources/assets/caszual_additions"
def read(path):return path.read_text(encoding="utf-8")
moving=read(src/"MovingPallet.java")
rack=read(src/"RackBlock.java")
route=read(src/"StorageGolemReturnPath.java")
renderer=read(client/"MovingPalletRenderer.java")
model=read(client/"MovingPalletModel.java")
wrap=read(src/"PortablePalletWrapperItem.java")
assert "entityContext.getEntity() instanceof StorageGolem" in rack
assert "entityContext.getEntity() instanceof MovingPallet" in rack
assert "Shapes.block()" in rack
assert "feetState.getBlock() instanceof RackBlock" in route
assert "headState.getBlock() instanceof RackBlock" in route
assert "below.getBlock() instanceof RackBlock" in route
assert "RACK_INSERT_ACTIVE" in moving and "RACK_INSERT_X" in moving
assert "RACK_INSERT_Y" in moving and "RACK_INSERT_Z" in moving
assert "releaseDuration=deliveryMode==1?RackPickupPlan.PULL_TICKS:20" in moving
assert moving.index("if(releaseTicks<releaseDuration)") < moving.index("boolean ok=deliveryMode==1")
assert 'entityData.set(RACK_INSERT_ACTIVE,false);' in moving
assert 'entityData.set(RACK_INSERT_ACTIVE,deliveryMode==1);' in moving
# The original fixed-facing carrier now rotates pallet and Golems together.
assert "state.bodyRot=180.0f" not in renderer
assert "180.0f-state.bodyRot" in renderer
assert "pose.mulPose(Axis.YP.rotationDegrees(" in renderer
assert "state.rackInsertX*slide" in renderer
assert "state.rackInsertY*lift" in renderer
assert "state.rackInsertZ*slide" in renderer
assert "float lift=Math.min(1f,stage*40f/22f)" in renderer
assert "float slide=Math.max(0f,Math.min(1f,(stage*40f-22f)/18f))" in renderer
assert "corners[i].yRot" not in model
assert "leftLeg[i].xRot" in model and "rightLeg[i].xRot" in model
assert "plastic stretch wrap" in wrap
j=json.loads(read(assets/"models/item/portable_pallet_wrapper.json"))
assert len(j["elements"])>=20
assert all(e.get("faces") for e in j["elements"])
assert {"film","grip","steel","red"} <= set(j["textures"])
p=json.loads(read(assets/"items/portable_pallet_wrapper.json"))
assert p["model"]["model"]=="caszual_additions:item/portable_pallet_wrapper"
print("PASS: rack robot-solid, reverse lift+slide, rigid corner models, 3D plastic wrapper")
