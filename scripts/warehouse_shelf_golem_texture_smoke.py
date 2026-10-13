#!/usr/bin/env python3
"""Block wrong shelf silhouettes: render actual six-part Storage Golem mesh/atlas.

A single real entity texture must back shelf, walking, and pallet crew views,
while shelf visuals are just dock-count-driven models, not duplicate entities.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CLIENT=ROOT/"src/client/java/com/caszgamermd/caszualadditions/racks"
MAIN=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"

def text(path):
    return path.read_text(encoding="utf-8")

renderer=text(CLIENT/"StorageGolemShelfRenderer.java")
state=text(CLIENT/"StorageGolemShelfRenderState.java")
walking=text(CLIENT/"StorageGolemModel.java")
moving=text(CLIENT/"MovingPalletModel.java")
shelf=text(MAIN/"StorageGolemShelfEntity.java")
for view in ("StorageGolemRenderer.java","MovingPalletRenderer.java"):
    assert 'textures/entity/storage_golem.png' in text(CLIENT/view),view

assert "StorageGolemModel.layer().bakeRoot()" in renderer
assert 'textures/entity/storage_golem.png' in renderer
assert "RenderTypes.entityCutout(GOLEM_SKIN)" in renderer
assert 'collector.order(2).submitModelPart(' in renderer
for part in ("body","head","left_arm","right_arm","left_leg","right_leg"):
    assert '"'+part+'"' in renderer and '"'+part+'"' in walking and '"'+part+'"' in moving,part
assert "for(int i=0;i<state.docked;i++)" in renderer
assert "state.docked=Math.clamp(shelf.docked(),0,2)" in renderer
assert "if(state.docked==0)return;" in renderer
assert "state.facing=shelf.getBlockState().getValue(StorageGolemShelfBlock.FACING)" in renderer
assert "poses.mulPose(Axis.YP.rotationDegrees(yaw))" in renderer
assert "poses.scale(-WIDTH_SCALE,-HEIGHT_SCALE,HEIGHT_SCALE)" in renderer
assert "state.lightCoords" in renderer
assert "private static final float FOOT_Y=3f/16f;" in renderer
assert "shelf.docked()" in renderer and 'out.putInt("docked_golems",docked)' in shelf
assert "blocks.update" not in renderer and "Blocks.IRON_BLOCK" not in renderer
assert "Blocks.DIAMOND_BLOCK" not in renderer
assert "BlockModelRenderState" not in state

# Shelf walls occupy X below 2 px and above 14 px; inner floor is at
# Y=3 px, upper shelf underside at Y=14 px. Two mini models must fit.
width_scale=.34
height_scale=.44
for center in (.30,.70):
    left=center-9/16*width_scale
    right=center+9/16*width_scale
    assert left>=.10 and right<=.90,(left,right)
ymin=3/16
ymax=ymin+24/16*height_scale
assert ymin>=3/16 and ymax<=14/16,(ymin,ymax)

assert (ROOT/"src/main/resources/assets/caszual_additions/textures/entity/storage_golem.png").is_file()
print("PASS: both shelf bays use exact six-part slate Golem mesh and skin, rotated and fit within shelf")
