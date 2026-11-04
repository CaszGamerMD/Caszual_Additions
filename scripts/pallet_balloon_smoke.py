#!/usr/bin/env python3
"""Pallet Balloon initial vehicle and speed/seat persistence contract."""
from pathlib import Path
import struct
import zlib
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
entity_texture=root/"src/main/resources/assets/caszual_additions/textures/entity/pallet_balloon.png"
item_texture=root/"src/main/resources/assets/caszual_additions/textures/item/pallet_balloon.png"
initializer=(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualAdditions.java").read_text()
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
def validate_png(path, expected_size):
    """Validate PNG chunk CRCs and the complete compressed image stream."""
    data=path.read_bytes()
    signature=bytes.fromhex("89504e470d0a1a0a")
    assert data.startswith(signature), f"{path}: invalid PNG signature"
    offset=8
    width=height=None
    compressed=bytearray()
    saw_end=False
    while offset < len(data):
        assert offset+12 <= len(data), f"{path}: truncated PNG chunk"
        length=struct.unpack_from(">I",data,offset)[0]
        chunk_type=data[offset+4:offset+8]
        chunk_end=offset+8+length
        assert chunk_end+4 <= len(data), f"{path}: truncated {chunk_type!r} chunk"
        chunk_data=data[offset+8:chunk_end]
        expected_crc=struct.unpack_from(">I",data,chunk_end)[0]
        actual_crc=zlib.crc32(chunk_type+chunk_data)&0xffffffff
        assert actual_crc==expected_crc, f"{path}: {chunk_type!r} CRC mismatch"
        if chunk_type==b"IHDR":
            assert length==13, f"{path}: invalid IHDR length"
            width,height=struct.unpack_from(">II",chunk_data,0)
        elif chunk_type==b"IDAT":
            compressed.extend(chunk_data)
        elif chunk_type==b"IEND":
            assert length==0, f"{path}: invalid IEND"
            saw_end=True
            offset=chunk_end+4
            break
        offset=chunk_end+4
    assert saw_end and offset==len(data), f"{path}: missing IEND or trailing bytes"
    assert (width,height)==expected_size, f"{path}: expected {expected_size}, got {(width,height)}"
    assert compressed, f"{path}: missing image data"
    zlib.decompress(bytes(compressed))

validate_png(entity_texture,(360,360))
validate_png(item_texture,(64,64))
assert "PalletBalloonNetworking.initialize()" in initializer
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
