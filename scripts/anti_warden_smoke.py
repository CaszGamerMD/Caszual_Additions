#!/usr/bin/env python3
"""Anti-Warden suite: verify registration, resources and invariants."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=root/"src/main/java/com/caszgamermd/caszualadditions"
resources=root/"src/main/resources"
anti=main/"antiwarden"
content=(anti/"AntiWardenContent.java").read_text()
trap=(anti/"WardenTrapBlockEntity.java").read_text()
trapblock=(anti/"WardenTrapBlock.java").read_text()
helmet=(anti/"mixin/WoolHelmetEffectMixin.java").read_text()
boots=(anti/"mixin/WoolBootsVibrationMixin.java").read_text()
for item in ("wool_helmet","wool_boots","warden_trap"):
    assert f'\"{item}\"' in content
    assert (resources/f"assets/caszual_additions/items/{item}.json").exists()
    assert (resources/f"data/caszual_additions/recipe/{item}.json").exists()
assert "humanoidArmor(WOOL_MATERIAL,slot)" in content
assert "MobEffects.DARKNESS" in helmet and "MobEffects.BLINDNESS" in helmet
assert "EquipmentSlot.HEAD" in helmet and "cir.setReturnValue(false)" in helmet
assert "EquipmentSlot.FEET" in boots
assert "handleGameEvent" in boots and "forceScheduleVibration" in boots
assert "position.distanceToSqr(player.position())<=49.0" in boots
assert "context.sourceEntity() instanceof Player" in boots
assert "getEntitiesOfClass(Warden.class" in trap
assert "warden.getNavigation().stop()" in trap
assert "warden.setDeltaMovement(Vec3.ZERO)" in trap
assert "warden.hurtServer(level" in trap
assert "ExperienceOrb.award(level" in trap
assert trap.index("warden.hurtServer") < trap.index("ExperienceOrb.award")
assert "getGameTime()%2" in trap
assert 'out.putString("warden",captive.toString())' in trap
assert "TRIPPED" in trapblock
mix=json.loads((resources/"caszual_additions.antiwarden.mixins.json").read_text())
assert len(mix["mixins"])==2
assert mix["package"]=="com.caszgamermd.caszualadditions.antiwarden.mixin"
fabric=json.loads((resources/"fabric.mod.json").read_text())
assert "caszual_additions.antiwarden.mixins.json" in fabric["mixins"]
for variant in ("humanoid","humanoid_baby","humanoid_leggings"):
    assert (resources/f"assets/caszual_additions/textures/entity/equipment/{variant}/wool_armor.png").exists()
for recipe in ("wool_helmet","wool_boots","warden_trap"):
    recipe_path=resources/f"data/caszual_additions/recipe/{recipe}.json"
    r=json.loads(recipe_path.read_text())
    assert r["result"]["id"]=="caszual_additions:"+recipe
assert "AntiWardenContent.initialize();" in (main/"CaszualAdditions.java").read_text()
print("PASS: Anti-Warden wool equipment, vibration filters, Warden-only XP trap and survival recipes")
