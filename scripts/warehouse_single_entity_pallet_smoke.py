#!/usr/bin/env python3
"""Static regression checks for one moving pallet / four attached Golems.
Real-world collision/navigation and render quality still need in-game QA.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(path):return (root/path).read_text()
p="src/main/java/com/caszgamermd/caszualadditions/racks/"
c="src/client/java/com/caszgamermd/caszualadditions/racks/"
entity=src(p+"MovingPallet.java")
model=src(c+"MovingPalletModel.java")
renderer=src(c+"MovingPalletRenderer.java")
crew=src(p+"StoragePalletCrew.java")
golem=src(p+"StorageGolem.java")
register=src(p+"RackContent.java")
client=src("src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java")
for text in ("NonNullList.withSize(216","ContainerHelper.saveAllItems",
             "ContainerHelper.loadAllItems", "getNavigation().moveTo",
             "private final BlockPos[] shelves=", "for(int i=0;i<4;i++)",
             "deliverToRack(", "deliverToGround(", "restoreWorkers(",
             "EntityDataSerializers.ITEM_STACK","EntityDataSerializers.INT"):
    assert text in entity,text
assert entity.index("if(!verify(rack))") < entity.index("clearCargo();",entity.index("public boolean deliverToRack(")),"No early cargo deletion"
assert entity.index("complete=verify(dest);") < entity.index("clearCargo();",entity.index("public boolean deliverToGround("))
assert "layer()" in model and "PartPose.offset(x,0,z)" in model
assert "state.walkAnimationPos" in model and "leftLeg[i].xRot" in model
assert "leftArm[i].xRot=-3.02f*grip" in model
assert "leftArm[i].x=leftSide?6f:-2.5f" in model
assert "rightArm[i].x=leftSide?2.5f:-6f" in model
assert "rightArm[i].xRot=-3.02f*grip" in model
assert "corners[i].yRot=" not in model
assert "if(liftTicks<12)" in entity
assert "entityData.set(LIFT_TICKS,liftTicks)" in entity
assert 'out.putInt("lift_ticks",liftTicks)' in entity
assert "state.liftProgress=state.visible?entity.liftProgress():0f;" in renderer
assert "only" not in crew.lower() or "formMovingPallet(" in crew
assert "if(!level.addFreshEntity(carrier))return false;" in crew
assert crew.index("if(!level.addFreshEntity(carrier))return false;") < crew.index("leader.relinquishPalletToRig();")
assert "for(StorageGolem member:crew)member.discard();" in crew
assert "StoragePalletCrew.formMovingPallet" in golem
assert 'CaszualAdditions.id("moving_pallet")' in register
assert "MovingPalletRenderer::new" in client
assert "MovingPalletModel::layer" in client
assert "super.submit(state,pose,collector,camera)" in renderer
assert "for(int i=0;i<state.cargo;i++)" in renderer
print("PASS: one pallet inventory/navigation/body, four locked animated models, safe delivery/worker restoration")
