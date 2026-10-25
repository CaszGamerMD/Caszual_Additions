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
assert "controller.zza" in vehicle and "controller.xxa" in vehicle
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
assert vehicle.count("jack.copyWithCount(1)")>=2
assert "Containers.dropItemStack" in forklift
assert "jack.copy()" in forklift
print("PASS: rider movement, edge/stair protection, 216-slot cargo persistence, " 
      "5-hit jack/forklift recovery, rendered transport pallet")
