#!/usr/bin/env python3
"""Regression guardrails for loaded/empty follow, two-worker dock, and delivery.

World collision and rendering still require in-game verification.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/"src/main/java/com/caszgamermd/caszualadditions/racks"
CLIENT=ROOT/"src/client/java/com/caszgamermd/caszualadditions/racks"
def main(file):return (MAIN/file).read_text()
def client(file):return (CLIENT/file).read_text()

golem=main("StorageGolem.java")
rig=main("MovingPallet.java")
crew=main("StoragePalletCrew.java")
router=main("StorageGolemReturnPath.java")
rig_model=client("MovingPalletModel.java")
golem_model=client("StorageGolemModel.java")
rig_renderer=client("MovingPalletRenderer.java")

assert ".add(Attributes.MOVEMENT_SPEED,0.2625)" in golem
assert ".add(Attributes.MOVEMENT_SPEED,.144)" in rig
player_follow=golem[golem.index("        // An empty crew stays"):golem.index("    private PtpBlockEntity haulSource")]
assert "player.getDirection()" not in player_follow
assert "getNavigation().stop()" in player_follow
rig_follow=rig[rig.index("    private void followOnFloor("):rig.index("    private void dropCargo(")]
assert "player.getDirection()" not in rig_follow
assert "MovingPalletFloorPath.route(" in rig_follow
assert "player.getY()>getY()+.8" in rig_follow
assert "getNavigation().stop()" in rig_follow

assert "rightArm[i].xRot=-1.85f*grip" in rig_model
assert "rightForearm[i].xRot=-.90f*grip" in rig_model
assert "leftArm[i].xRot=-1.85f*grip" in rig_model
assert "leftArm.xRot=-1.85f" in golem_model
assert "leftForearm.xRot=-.90f" in golem_model
assert "corners[i].yRot=" not in rig_model
assert "state.releaseProgress=entity.releaseProgress()" in rig_renderer
assert "rig.requestRackDelivery(level,rack)" in crew
assert "rig.requestGroundDelivery(" in crew
assert "if(isDelivering()){tickDelivery(server);return;}" in rig
assert "releaseDuration=deliveryMode==1?RackPickupPlan.PULL_TICKS:20" in rig
assert "if(releaseTicks<releaseDuration)" in rig
assert rig.index("if(releaseTicks<releaseDuration)") < rig.index("boolean ok=deliveryMode==1")
assert "delivery_mode" in rig and "delivery_position" in rig
assert "delivery_failure" in rig
assert "groundClear(level,target,deliveryFacing)" in rig
assert "requestGroundDelivery" in rig and "requestRackDelivery" in rig
assert "if(delivered)" in rig and "restoreWorkers(server)" in rig
assert "for(int depth=1;depth<=3;depth++)" in router
assert "if(feet.equals(corner)" in router
print("PASS: slower proximity follow; upright arms; queued arrival, release, and two-worker docking")
