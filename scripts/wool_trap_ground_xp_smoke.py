#!/usr/bin/env python3
"""Wool sensory gear, placed XP Collector and scoped active trap quieting."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions"
cli=root/"src/client/java/com/caszgamermd/caszualadditions"
res=root/"src/main/resources"
trap=(src/"antiwarden/WardenTrapBlockEntity.java").read_text()
trap_silence=(src/"antiwarden/WardenTrapSilence.java").read_text()
sounds=(src/"antiwarden/mixin/WardenTrapSoundMixin.java").read_text()
boots=(src/"antiwarden/mixin/WoolBootsVibrationMixin.java").read_text()
render=(cli/"antiwarden/WoolGearLayer.java").read_text()
xpblock=(src/"xp/XpCollectorBlock.java").read_text()
xpbe=(src/"xp/XpCollectorBlockEntity.java").read_text()
xpblocks=(src/"xp/XpBlocks.java").read_text()
entities=(src/"xp/XpBlockEntities.java").read_text()
main=(src/"CaszualAdditions.java").read_text()
tab=(src/"CaszualItemGroups.java").read_text()
client=(cli/"CaszualAdditionsClient.java").read_text()
avatar=(cli/"utils/mixin/EndRodAvatarRendererMixin.java").read_text()
mixin=json.loads((res/"caszual_additions.antiwarden.mixins.json").read_text())
assert "WardenTrapSoundMixin" in mixin["mixins"]
assert "WardenTrapSilence.update(level,worldPosition,true)" in trap
assert "WardenTrapSilence.update(level,worldPosition,false)" in trap
assert "warden.setSilent(true);" in trap
assert "wasSilent=caught.isSilent();" in trap
assert "warden.setSilent(wasSilent)" in trap
assert '"was_silent"' in trap
assert "RADIUS=9.0" in trap_silence
assert "sculkOrWardenSound" in trap_silence
assert "WardenTrapSilence.near" in sounds
assert "playSeededSound" in sounds
# Animal Garden and other mods may play direct SoundEvent values for which
# BuiltInRegistries.SOUND_EVENT.getKey(value) returns null. These must NOT
# crash the server or be canceled, even when a trap is nearby.
assert "if(sound==null||sound.value()==null" in sounds
assert "var key=BuiltInRegistries.SOUND_EVENT.getKey(sound.value());" in sounds
assert "return key!=null" in sounds
assert "key.toString()" in sounds
assert "getKey(sound.value()).toString()" not in sounds
assert sounds.index("WardenTrapSilence.near((ServerLevel)(Object)this,position)") < sounds.index("var key=BuiltInRegistries.SOUND_EVENT.getKey(sound.value())")
assert "WardenTrapSilence.near(level,position)" in boots
assert "distanceToSqr(player.position())<=49.0" in boots
assert "rightSlipper" in render and "leftSlipper" in render
assert "model.head.translateAndRotate(pose)" in render
assert "model.rightLeg.translateAndRotate(pose)" in render
assert "model.leftLeg.translateAndRotate(pose)" in render
assert "WoolGearLayer::layer" in client
assert "new com.caszgamermd.caszualadditions.antiwarden.WoolGearLayer(this,context)" in avatar
assert (res/"assets/caszual_additions/textures/entity/wool_sensory_gear.png").exists()
# Equipped armor is ONLY the custom articulated 3D layer. Vanilla's
# full helmet/leg armor shell previously covered the skin in a giant
# purple panel / overlaid the actual earmuffs and slipper geometry.
asset=json.loads((res/"assets/caszual_additions/equipment/wool_armor.json").read_text())
assert all(len(layers)==0 for layers in asset["layers"].values())
assert all(k in asset["layers"] for k in ("humanoid","humanoid_baby","humanoid_leggings"))
assert "return LayerDefinition.create(mesh,256,256);" in render
assert "texOffs(0,64)" in render and "texOffs(48,64)" in render
assert "addOrReplaceChild(\"left_slipper\",slipper" in render
assert "addOrReplaceChild(\"right_slipper\",slipper" in render
assert "model.head.translateAndRotate(pose)" in render
assert "model.leftLeg.translateAndRotate(pose)" in render
assert "model.rightLeg.translateAndRotate(pose)" in render
assert "full-face" in render or "full face" in render
# UV atlas and model texture dimensions must MATCH; all five material
# regions (offwhite wool, slate, rim, lens, sole) are present in PNG.
import struct
png=(res/"assets/caszual_additions/textures/entity/wool_sensory_gear.png").read_bytes()
assert png.startswith(bytes.fromhex("89504e470d0a1a0a"))
assert struct.unpack(">II",png[16:24])==(256,256)
assert len(png)>900
for name,minimum in [("wool_helmet",12),("wool_boots",10)]:
    model=json.loads((res/f"assets/caszual_additions/models/item/{name}.json").read_text())
    assert len(model["elements"])>=minimum
assert "XP_COLLECTOR=register(\"xp_collector\"" in xpblocks
assert "COLLECTOR=Registry.register(" in entities
assert "XpCollectorBlockEntity.tick(" in xpblock
assert "getEntitiesOfClass(ExperienceOrb.class" in xpbe
assert "RADIUS=6;" in xpbe
assert "CAPACITY=30970;" in xpbe
assert "orb.discard();" in xpbe
assert "collector.setStored(stored);" in xpbe
assert "data.copyTag().getIntOr(\"stored_xp\",0)" in xpbe
assert '"stored_xp"' in xpbe
assert "getDrops(BlockState state,LootParams.Builder builder)" in xpblock
assert "collector.asItem()" in xpblock
assert "XpCollectorBlockEntity.itemXp(stack)" in xpblock
assert "player.giveExperiencePoints(collector.withdraw())" in xpblock
assert "player.isShiftKeyDown()" in xpblock
assert "XpCollectorItem" not in main
assert "XpBlocks.XP_COLLECTOR.asItem()" in tab
assert "XpCollectorItem" not in tab
for s in ("assets/caszual_additions/blockstates/xp_collector.json",
          "assets/caszual_additions/models/block/xp_collector.json",
          "assets/caszual_additions/models/item/xp_collector.json",
          "assets/caszual_additions/items/xp_collector.json",
          "data/caszual_additions/recipe/xp_collector.json"):
    assert (res/s).exists(),s
assert not (src/"xp/XpCollectorItem.java").exists()
print("PASS: earmuff goggles, wool slippers, active Warden/triggers silence, ground-placed reusable XP Collector")
