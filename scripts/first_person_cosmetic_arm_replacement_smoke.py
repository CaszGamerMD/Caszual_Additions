#!/usr/bin/env python3
"""First-person cosmetic arms replace the actual avatar arm at its render hook.

Source regression guards the previous TAIL overlay bug and validates bare /
held-item paths share the same real vanilla arm renderer. Fabric compilation
checks the Minecraft 26.2 method descriptors and model APIs.
"""
import json
from pathlib import Path

base=Path(__file__).resolve().parents[1]
client=base/"src/client/java/com/caszgamermd/caszualadditions"
mixins=json.loads((base/"src/main/resources/caszual_additions.client.mixins.json").read_text())
avatar=(client/"utils/mixin/EndRodAvatarRendererMixin.java").read_text()
replacement=(client/"utils/cosmetics/FirstPersonCosmeticArmRender.java").read_text()
legacy=(client/"utils/mixin/FirstPersonCosmeticArmsMixin.java").read_text()
assert "EndRodAvatarRendererMixin" in mixins["client"]
assert "FirstPersonCosmeticArmsMixin" not in mixins["client"],"old TAIL hook still active"
assert '@Inject(method="renderHand",at=@At("HEAD"),cancellable=true)' in avatar
assert "ci.cancel();" in avatar
assert "if(player==null||player.isInvisible())return;" in avatar
for feature in ("isWearableAquarium(chest)","isSnowGolem(","isBone(chest)",
                "isEndRod(chest)","chest.getItem() instanceof BlockItem"):
    assert feature in avatar,feature
for feature in ("WearableAquariumArmRender.renderHand(","caszual_additions$snow.hand(",
                "FirstPersonCosmeticArmRender.drawBone(",
                "EndRodRender.limb(","FirstPersonCosmeticArmRender.drawBlock("):
    assert feature in avatar,feature
assert "vanillaArm.resetPose();" in replacement
assert "vanillaArm.translateAndRotate(pose);" in replacement
assert "collector.order(1).submitModelPart(replacement,pose," in replacement
assert "RenderTypes.entityTranslucent(texture)" in replacement
assert "BlockOutfitTextures.texture(item.getBlock())" in replacement
assert "BlockOutfitTextures.texture(Blocks.BONE_BLOCK)" in replacement
assert "slim?SLIM_BLOCK:WIDE_BLOCK" in replacement
assert "slim?SLIM_BONE:WIDE_BONE" in replacement
assert "vanillaArm" in replacement and "LayerDefinition.create(mesh,texW,texH)" in replacement
assert "renderPlayerArm" in legacy and "submitArmWithItem" in legacy
print("PASS: first-person block, bone, end rod, aquarium, snow golem replacement is single-hook and item-safe")
