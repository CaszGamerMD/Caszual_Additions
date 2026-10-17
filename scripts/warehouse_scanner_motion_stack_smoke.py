#!/usr/bin/env python3
"""Regression safety guards for Direct crew recall, remote lift visuals and stack pickup."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
racks=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=ROOT/"src/client/java/com/caszgamermd/caszualadditions/racks"
def src(p):return (racks/p).read_text()
def render(p):return (client/p).read_text()
golem=src("StorageGolem.java")
rig=src("MovingPallet.java")
stack=src("StackOfPalletsBlock.java")
stack_be=src("StackOfPalletsBlockEntity.java")
model=render("MovingPalletModel.java")
rig_render=render("MovingPalletRenderer.java")
courier=render("StorageGolemRenderer.java")

# New direct crews must consume the CURRENT recall epoch at the time
# their real shelf worker identity is assigned; an earlier RECALL ALL
# must never override an explicit Scanner summon.
crew=golem[golem.index("public void assignPlayerCrew("):
           golem.index("public boolean ownedBy(",golem.index("public void assignPlayerCrew("))]
assert "stage=4" in crew and "markCurrentRecall();" in crew
assert crew.index("home=shelf.immutable()")<crew.index("markCurrentRecall();")
assert "shelf.recallSerial()>observedRecallSerial" in golem

# Remote three-worker rig must be empty-handed, with natural animated arms
# until it physically acquires the 216-slot old pallet at the target.
assert "state.visible=!stack.isEmpty();" in rig_render
assert "state.liftProgress=state.visible?entity.liftProgress():0f;" in rig_render
assert "if(!state.visible)" in model
assert "Mth.cos(phase+offset)" in model
assert "leftArm[i].xRot=-3.02f*state.liftProgress" in model
assert "if(!carrier.acceptRemoteLift" not in rig
assert "liftTicks=0;" in rig
assert "entityData.set(LIFT_TICKS,0);" in rig

# Empty-pallet courier pushes a nearly full 2x2 deck ON EDGE in FRONT;
# it rotates with the worker and sways/bobs with its walking gait.
assert "state.pushingEmptyPallet" in courier
assert "state.walkAnimationPos" in courier
assert "state.walkAnimationSpeed" in courier
assert "Axis.ZP.rotationDegrees" in courier
assert "Axis.XP.rotationDegrees" in courier
assert "pose.translate(0,1.01f+bob,-.80f)" in courier
assert "piece(state.heldTray,state,pose,collector" in courier
assert "blocks.update(state.heldTray,Blocks.POLISHED_DEEPSLATE" in courier
assert "if(state.pushingEmptyPallet)" in render("StorageGolemModel.java")
assert "1.88f,.255f,.185f" in courier
assert "for(int board=0;board<4;board++)" in courier
assert "PalletData.wood(carried).get(i)" in courier
assert "PalletData.plasticColor(carried)" in courier
assert "PalletContent.copperTextureBlock" in courier

# Empty main-hand and secondary 26.2 block-use paths both withdraw exactly
# one top pallet. A same-tick duplicate click cannot withdraw two pallets.
assert "private static boolean removeTopPallet(" in stack
assert "if(held.isEmpty()&&!player.isShiftKeyDown())" in stack
assert "return removeTopPallet(level,pos,player)" in stack
assert "removeTopPallet(level,pos,player);" in stack
assert "lastPickupTick.get(player.getUUID())" in stack
assert "previous!=null&&previous==tick" in stack
assert "lastPickupTick.put(player.getUUID(),tick)" in stack
assert "ItemStack taken=magazine.take();" in stack
assert "player.getInventory().placeItemBackInInventory(taken)" in stack
assert "for(int i=11;i>=0;i--)" in stack_be
assert "ItemContainerContents.fromItems(pallets)" in stack_be
assert "getCollisionShape(" in stack
print("PASS: direct recall epoch, empty-handed pickup team, animated full-size courier,"
      " and one-at-a-time empty-hand pallet stack withdrawal")
