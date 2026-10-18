#!/usr/bin/env python3
"""Industrial PVC curtains and remote pickup deadlock regression.
This is a source/asset smoke check; movement and sight still require gameplay.
"""
from pathlib import Path
import json,struct,zlib
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions"
resources=root/"src/main/resources"
curtain=(src/"curtains/PlasticCurtainsBlock.java").read_text()
registration=(src/"curtains/PlasticCurtainsContent.java").read_text()
courier=(src/"racks/StorageGolem.java").read_text()
rig=(src/"racks/MovingPallet.java").read_text()
main=(src/"CaszualAdditions.java").read_text()
creative=(src/"CaszualItemGroups.java").read_text()
assert "extends DoorBlock" in curtain
assert "EntityCollisionContext" in curtain
assert "entity!=null&&!hostile(entity)" in curtain
assert "Shapes.empty()" in curtain
assert "entity instanceof Enemy" in curtain
assert "MobCategory.MONSTER" in curtain
assert "return sheet(state);" in curtain
assert "getVisualShape" in curtain
assert "InteractionResult.PASS;" in curtain
assert "PlasticCurtainsContent.initialize();" in main
assert "PlasticCurtainsContent.ITEM" in creative
assert 'CaszualAdditions.id("plastic_curtains")' in registration
state=json.loads((resources/"assets/caszual_additions/blockstates/plastic_curtains.json").read_text())
parts=state["multipart"]
assert len(parts)==8
assert {(x["when"]["facing"],x["when"]["half"]) for x in parts}=={
 (side,half) for side in ("north","south","east","west") for half in ("lower","upper")
}
for half in ("upper","lower"):
    model=json.loads((resources/f"assets/caszual_additions/models/block/plastic_curtains_{half}.json").read_text())
    assert model["render_type"]=="minecraft:translucent"
    strips=[e for e in model["elements"] if e["faces"]["north"]["texture"]=="#strip"]
    assert len(strips)==8
    assert strips[0]["from"][0]==0 and strips[-1]["to"][0]==16
    assert all(e["from"][1]==0 for e in strips)
assert len(json.loads((resources/"assets/caszual_additions/models/block/plastic_curtains_upper.json").read_text())["elements"])>8
png=(resources/"assets/caszual_additions/textures/block/plastic_curtain_strips.png").read_bytes()
assert png[:8]==bytes.fromhex("89504e470d0a1a0a")
assert struct.unpack(">II",png[16:24])==(16,16)
chunks={};i=8
while i<len(png):
    length=int.from_bytes(png[i:i+4],"big")
    name=png[i+4:i+8];payload=png[i+8:i+8+length]
    assert zlib.crc32(name+payload)==int.from_bytes(png[i+8+length:i+12+length],"big")
    chunks.setdefault(name,[]).append(payload)
    i+=length+12
assert i==len(png)
assert b"tRNS" in chunks
assert any(0<a<255 for a in chunks[b"tRNS"][0])
assert len(zlib.decompress(b"".join(chunks[b"IDAT"])))==16*17
recipe=json.loads((resources/"data/caszual_additions/recipe/plastic_curtains.json").read_text())
assert recipe["result"]["id"]=="caszual_additions:plastic_curtains"
assert "caszual_additions:plastic_curtains" in json.loads((resources/"data/caszual_additions/tags/item/caszual_content.json").read_text())["values"]
assert "caszual_additions:plastic_curtains" in json.loads((resources/"data/minecraft/tags/block/doors.json").read_text())["values"]
assert "block.caszual_additions.plastic_curtains" in json.loads((resources/"assets/caszual_additions/lang/en_us.json").read_text())
# Previously marked done from an adjacent tile, then rejected placement
# indefinitely because the actual Golem never reached the reserved aisle.
assert "blockPosition().getX()==goal.getX()" in courier
assert "blockPosition().getZ()==goal.getZ()" in courier
assert "finalDistance<.85" in courier
assert "WarehouseTerrain.standingY(level,goal)" in courier
assert "remoteMoveAside=remoteClearParking(level);" in rig
assert "AABB moved=getBoundingBox().move(" in rig
assert "if(moved.intersects(occupied))continue;" in rig
assert "route.get(route.size()-1).equals(cell)" in rig
assert "remoteMoveAside=BlockPos.ZERO;" in rig
assert "if(remoteFootprintClear()){" in rig
assert "readyForCourierPlacement()" in rig
print("PASS: seamless translucent two-high hostile-blocking curtains, real-route pallet parking and exact courier handoff")
