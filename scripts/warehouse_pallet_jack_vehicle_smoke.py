#!/usr/bin/env python3
"""Pallet Jack is a level-only ridden vehicle with exactly one packed payload.

Source-level regression complements Fabric's Minecraft integration tests.
"""
from pathlib import Path
R=Path(__file__).resolve().parents[1]
rack=R/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=R/"src/client/java/com/caszgamermd/caszualadditions/racks"
jack=(rack/"PalletJackItem.java").read_text()
vehicle=(rack/"PalletJackVehicle.java").read_text()
forklift=(rack/"WarehouseForklift.java").read_text()
registry=(rack/"RackContent.java").read_text()
renderer=(client/"PalletJackRenderer.java").read_text()
registration=(R/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java").read_text()
assert "extends PathfinderMob" in vehicle
assert "player.startRiding(vehicle)" in jack
assert "player.startRiding(this)" in vehicle
assert "player.getVehicle() instanceof PalletJackVehicle" in jack
assert "UseBlockCallback.EVENT.register" in jack
assert "if(player.isShiftKeyDown())return InteractionResult.PASS;" in vehicle
assert "input.z" in vehicle and "input.x" in vehicle
# Don't discard the actual 26.2 ridden input by reading idle passenger
# xxa/zza fields. This regression caught a jack that could never move.
assert "return new Vec3(input.x,0,input.z);" in vehicle
assert "server.getLastClientMoveIntent()" in vehicle
assert "if(level().isClientSide()&&hasClientMovement)" in vehicle
client_controls=(R/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletJackClientMovement.java").read_text()
client_initializer=(R/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java").read_text()
assert "ClientTickEvents.START_CLIENT_TICK.register" in client_controls
assert "jack.setClientMovement(new Vec3(x,0,z));" in client_controls
assert "PalletJackClientMovement.initialize();" in client_initializer
assert "new Vec3(player.xxa,0,player.zza)" not in vehicle
assert "public void travel(Vec3 input)" in vehicle
assert "floorY" in vehicle
assert "getY()-floorY" in vehicle
assert "foot.below()" in vehicle
assert "isCollisionShapeFullBlock" in vehicle
assert "isFaceSturdy" in vehicle
assert "!level.getFluidState(foot).isEmpty()" in vehicle
assert "safeFloor(level(),getX()+dx,getZ()+dz,heading)" in vehicle
assert "new Vec3(dx,0,dz)" in vehicle
assert 'jack_floor_y' in vehicle and 'jack_contents' in vehicle
assert "public ItemStack storedJack()" in vehicle
assert "PalletJackItem.loaded(jack)" in vehicle
assert "entityData.set(DISPLAY_DECK" in vehicle
assert "entityData.set(DISPLAY_CRATES" in vehicle
assert "ejectPassengers();" in vehicle and "if(recovered" in vehicle
assert "jack.copyWithCount(1)" in vehicle
assert "Containers.dropItemStack" in vehicle
assert "vehicle.deckY()" in jack
assert "operatePayload(server,player,jack,clicked,face)" in jack
assert "rack.installFromJack(jack)" in jack
assert "rack.liftToJack(jack)" in jack
assert "jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed))" in jack
assert "controller.restoreJackCargo(contents)" in jack
assert "jack.remove(DataComponents.CONTAINER)" in jack
assert "PALLET_JACK_VEHICLE_TYPE" in registry
assert "PalletJackVehicle.attributes()" in registry
assert "PalletJackRenderer::new" in registration
assert "public final class PalletJackRenderer" in renderer
assert "vehicle.displayPallet()" in renderer
assert "PalletData.wood(pallet).get(i)" in renderer
assert "state.cargo" in renderer
assert "new ItemStack(RackContent.FORKLIFT_ITEM)" in forklift
assert ".add(Attributes.MAX_HEALTH,5)" in forklift
assert ".add(Attributes.MAX_HEALTH,5)" in vehicle
# A dead/broken transport retains the packed Jack, rather than dropping
# 216 independent item entities.
assert vehicle.count("jack.copyWithCount(1)")>=1
assert "Containers.dropItemStack" in forklift
assert "jack.copy()" in forklift

# Standing operator + trailing load is both rendered and collision-tested.
assert "public static final double OPERATOR_LEAD=.93D;" in vehicle
assert "position().add(-Math.sin(angle)*OPERATOR_LEAD,0," in vehicle
assert "Math.cos(angle)*OPERATOR_LEAD" in vehicle
assert "new double[]{-2.12,-1.34,-.57,.25,OPERATOR_LEAD}" in vehicle
assert "new double[]{-.86,-.40,.40,.86}" in vehicle
assert "if(cargo||f>=OPERATOR_LEAD){" in vehicle
assert 'pose.mulPose(Axis.YP.rotationDegrees(-state.heading));' in renderer
assert 'pose.mulPose(Axis.YP.rotationDegrees(180-state.heading));' not in renderer
avatar=(client/"utils/mixin/AvatarCosmeticsMixin.java").read_text()
pose=(client/"utils/mixin/PlayerPlushieCuddleMixin.java").read_text()
state=(client/"utils/mixin/PalletJackAvatarStateMixin.java").read_text()
contract=(client/"racks/PalletJackAvatarPose.java").read_text()
mixins=(R/"src/main/resources/caszual_additions.client.mixins.json").read_text()
assert "state.isPassenger=false;" in avatar
assert "state.isCrouching=false;" in avatar
assert "player.getVehicle() instanceof PalletJackVehicle" in avatar
assert "caszual_additions$setPullingJack" in avatar
assert "caszual_additions$isPullingJack()" in pose
assert "model.rightArm.xRot=1.06F;" in pose
assert "model.leftArm.xRot=1.06F;" in pose
assert "model.rightLeg.xRot=stride;" in pose
assert "model.leftLeg.xRot=-stride;" in pose
assert "caszual_additions$jackWalking" in state
assert "interface PalletJackAvatarPose" in contract
assert '"PalletJackAvatarStateMixin"' in mixins
print("PASS: rider movement, edge/stair protection, 216-slot cargo persistence, " 
      "5-hit jack/forklift recovery, rendered transport pallet")
