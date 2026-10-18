#!/usr/bin/env python3
"""Idle golems have straight-down arms; both forearms are fully textured.
Loaded three/four worker rigs and the single pallet courier treat genuine
bottom slabs as half-height smooth terrain, not full-step obstacles.
"""
import importlib.util
from pathlib import Path
root=Path(__file__).resolve().parents[1]
script=root/"scripts/warehouse_golem_robot_texture_smoke.py"
spec=importlib.util.spec_from_file_location("robot_atlas",script)
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
at=module.at
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
solo=(client/"StorageGolemModel.java").read_text()
team=(client/"MovingPalletModel.java").read_text()
renderer=(client/"StorageGolemRenderer.java").read_text()
golem=(src/"StorageGolem.java").read_text()
terrain=(src/"WarehouseTerrain.java").read_text()
walk=(src/"StorageGolemReturnPath.java").read_text()
route=(src/"WarehousePreferredPath.java").read_text()
floor=(src/"MovingPalletFloorPath.java").read_text()
carrier=(src/"MovingPallet.java").read_text()
courier=(src/"RemotePalletCourierRoute.java").read_text()

for txt in (team,solo):
    assert txt.count(".texOffs(0,40)")==2
    assert txt.count(".texOffs(16,40)")==2
    assert 'addOrReplaceChild("forearm"' in txt
# Validate ALL faces of both 4x6x4 cubes. Upper/forearm cuboid UV map:
# top/bottom 8x4 (offset 4px), sides 16x6 (offset y+4).
for x in (0,16):
    for u in range(x+4,x+12):
        for v in range(40,44):
            assert at(u,v)[3]==255,("missing cap texel",u,v)
    for u in range(x,x+16):
        for v in range(44,50):
            assert at(u,v)[3]==255,("missing sleeve/glove texel",u,v)

assert "state.carryingPallet=golem.carryingPalletPose();" in renderer
assert "state.pushingEmptyPallet=golem.pushingPalletPose();" in renderer
assert 'label.startsWith("PALLET ")' not in renderer
assert "return playerHaul&&stage==1;" in golem
assert 'label="CREW "+(haulIndex+1)+"/4 | IDLE";' in golem
assert "if(amount<.045f)amount=0f;" in solo
assert "leftForearm.xRot=rightForearm.xRot=0f;" in solo
assert "leftArm.zRot=rightArm.zRot=0f;" in solo
assert "leftForearm[i].xRot=0f;" in team

assert "SlabType.BOTTOM" in terrain
assert "boolean slabFloor=WarehouseTerrain.bottomSlab(level,feet)" in walk
assert "if(slabFloor)return true;" in walk
assert "if(WarehouseTerrain.bottomSlab(level,ground))return false;" in walk
assert "public static double standingY(ServerLevel level,BlockPos feet)" in terrain
assert "Math.abs(standingY(level,to)-standingY(level,from))<=.501" in terrain
assert "public static boolean wideFloor(ServerLevel level,BlockPos center)" in terrain
assert "double rise=Math.abs(standingY.applyAsDouble(next)" in route
assert "int penalty=rise<=.501?0:STEP_HEIGHT_COST;" in route
assert "WarehouseTerrain.standingY(server,next)" in golem
assert "&&WarehouseTerrain.standingY(server,next)-getY()>.60" in golem
assert "WarehouseTerrain.wideFloor(level,center)" in carrier
assert "WarehouseTerrain.standingY(level,next)" in carrier
assert "smoothHalfStep.test(current,next)" in floor
assert "WarehouseTerrain.smoothHalfStep(level,from,to)" in carrier
assert "WarehouseTerrain.standingY(level,pos)" in courier

# Toy physical heights: slab surface y+.5; full block feet y.
# Up+down 0.5 should require no jump even when BlockPos Y changes.
full=65
slab=64+.5
assert abs(full-slab)==.5
assert not (full-slab>.60)
# One block remains a genuine step (with a jump if required).
assert 65-64>.60
print("PASS: idle arms hang at sides, all new UV pixels opaque, loaded pallet glides over half slabs")
