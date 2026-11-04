#!/usr/bin/env python3
"""Pallet Balloon initial vehicle and speed/seat persistence contract."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
balloon=(main/"PalletBalloon.java").read_text()
item=(main/"PalletBalloonItem.java").read_text()
reg=(main/"RackContent.java").read_text()
client=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletBalloonClientMovement.java").read_text()
model=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletBalloonModel.java").read_text()
pallet=(root/"src/main/java/com/caszgamermd/caszualadditions/pallet/PalletBlock.java").read_text()
renderer=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletBalloonRenderer.java").read_text()
render_state=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletBalloonRenderState.java").read_text()
network=(main/"PalletBalloonNetworking.java").read_text()
payload=(main/"PalletBalloonPilotPayload.java").read_text()
renderer_texture=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/PalletBalloonRenderer.java").read_text()
item_model=(root/"src/main/resources/assets/caszual_additions/models/item/pallet_balloon.json").read_text()
assert "getPassengers().size()<4" in balloon
assert "pallets.add(ItemStack.EMPTY)" in balloon
assert "double[][] seats={{-1.72,1.35,-1.72}" in balloon
assert "for(int i=0;i<4;i++)" in balloon
assert "EntityDataSerializers.ITEM_STACK" in balloon
assert "public ItemStack palletAt(int slot)" in balloon
assert "Float.isFinite(forward)" in balloon and ".yRot((float)-Math.toRadians(getYRot()))" in balloon
assert "setServerPilotInput(payload.forward(),payload.turn(),payload.lift())" in network
assert "PayloadTypeRegistry.serverboundPlay()" in network
assert "StreamCodec.composite(" in payload and "PalletBalloonPilotPayload::new" in payload
assert "ClientPlayNetworking.send(new PalletBalloonPilotPayload(forward,turn,lift))" in client
assert "textures/entity/pallet_balloon.png" in renderer_texture
assert "caszual_additions:item/pallet_balloon" in item_model
assert "tryBalloonPickup(level, pos, state, player)" in pallet
assert "createCreativeCopyStack()" in pallet
assert "PalletCargoCollisionBlock.clear(server, root, rootState)" in pallet
assert "items.updateForLiving(state.cargo[i],stack" in renderer
assert "state.cargo[i].submit" in renderer
assert "ItemStackRenderState[] cargo" in render_state
assert 'out.store("pallet_"+i,ItemStack.CODEC,pallets.get(i))' in balloon
assert 'in.read("pallet_"+i,ItemStack.CODEC)' in balloon
assert "return (2.0+18.0*t)/20.0;" in balloon
assert "Math.clamp((getY()-64.0)/236.0,0.0,1.0)" in balloon
assert "PALLET_BALLOON_TYPE" in reg and "PALLET_BALLOON_ITEM" in reg
assert ".sized(5.2f,7.8f).build(balloonKey)" in reg
assert "EntityType.Builder.<PalletBalloon>of" in reg
assert "player.startRiding(balloon)" in item
assert "keyJump.isDown()?1:0" in client
assert "keySprint.isDown()?1:0" in client
assert '"basket"' in model and '"ropes"' in model and '"envelope"' in model
print("PASS: balloon seats, networked pilot controls, cargo persistence/sync/rendering, placed-pallet pickup and height-scaled 2-20 m/s speed")
