#!/usr/bin/env python3
"""XP hardware regression: crouch intake, collector naming, charger/drain art.

Static/source checks guard the XP path while Fabric's compilation confirms API
compatibility. A real player in a live world is still needed for functional QA.
"""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
java=root/"src/main/java/com/caszgamermd/caszualadditions/xp"
assets=root/"src/main/resources/assets/caszual_additions"
read=lambda name:(java/name).read_text(encoding="utf-8")
block=read("XpDrainBlock.java")
entity=read("XpDrainBlockEntity.java")
network=read("XpNetwork.java")
collector=read("XpCollectorBlock.java")
charger=read("XpChargerBlock.java")
lang=json.loads((assets/"lang/en_us.json").read_text(encoding="utf-8"))
assert lang["block.caszual_additions.xp_collector"]=="XP Collector"
assert "XP_COLLECTOR=register(\"xp_collector\"" in read("XpBlocks.java")
assert "return InteractionResult.SUCCESS;" in block
assert "XP Drain: connect an XP Tank" in block
assert "XpDrainBlockEntity.transferPlayer(" in block
assert "getTicker(Level level" in block
assert "XpDrainBlockEntity.tick(" in block
assert "transferPlayer(ServerLevel level,BlockPos pos,Player player)" in entity
assert "player.isCrouching()" in entity
assert "player.getBoundingBox()" in entity
assert "XpNetwork.insert(level,pos,Math.min(available,40))" in entity
assert "XpPlayer.remove(player,moved)" in entity
assert "transferPlayer(level,pos,p)" in entity
assert "XpNetwork.space(level,pos)<=0" in entity
assert "ExperienceOrb.class" in entity
assert "Items.EXPERIENCE_BOTTLE" in entity
assert ">=7" in entity
assert "for (Direction direction : Direction.values())" in network
assert "instanceof XpDrainBlockEntity" in network
assert "XpNetwork.extract" in read("XpRepair.java")
assert "charger.item()" in charger
assert "collector.asItem()" in collector
for name,min_parts in (("xp_drain",14),("xp_charger",18)):
    data=json.loads((assets/f"models/block/{name}.json").read_text())
    assert len(data["elements"])>=min_parts,name
    assert data["parent"]=="minecraft:block/block"
    assert all("from" in e and "to" in e for e in data["elements"])
    model=json.loads((assets/f"models/item/{name}.json").read_text())
    assert model["parent"]==f"caszual_additions:block/{name}"
print("PASS: collector name, crouch-powered XP Drain, Mending dock, improved XP art")
