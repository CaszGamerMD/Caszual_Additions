package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.Cosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.BlockOutfitTextures;
import com.caszgamermd.caszualadditions.utils.cosmetics.EndRodRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.WearableAquariumArmRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The cosmetic chest slot replaces both player arms in third person; apply
 * the same replacement to the actual first-person hand model, too. Do not
 * intercept item submissions: tools and held items stay in vanilla position.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonCosmeticArmsMixin {
    @Unique private static final Identifier BONE_TEXTURE =
            Identifier.withDefaultNamespace("textures/block/bone_block_side.png");
    @Unique private static final Identifier STICK_TEXTURE =
            Identifier.withDefaultNamespace("textures/block/oak_log.png");
    @Unique private static final ModelPart THIN_ARM = caszual_additions$makeThinArm();
    @Unique private static final ModelPart BLOCK_ARM = caszual_additions$makeBlockArm();

    @Unique
    private static ModelPart caszual_additions$makeBlockArm() {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("arm");
    }

    @Unique
    private static ModelPart caszual_additions$makeThinArm() {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, -1, 2, 12, 2),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("arm");
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void caszual_additions$firstPersonCosmeticArm(
            PoseStack pose, SubmitNodeCollector collector, int light,
            float equipProgress, float attackProgress, HumanoidArm arm,
            CallbackInfo callback
    ) {
        if (caszual_additions$renderArm(pose, collector, light,
                equipProgress, attackProgress, arm)) {
            callback.cancel();
        }
    }

    // On 26.2, ordinary held items bypass renderPlayerArm. Render the
    // cosmetic limb behind that item's normal submission as well.
    @Inject(method = "submitArmWithItem", at = @At("TAIL"), require = 0)
    private void caszual_additions$heldItemArm(
            AbstractClientPlayer player, float tickDelta, float pitch,
            InteractionHand hand, float swing, ItemStack item, float equip,
            PoseStack pose, SubmitNodeCollector collector, int light,
            CallbackInfo callback
    ) {
        if (item.isEmpty() || item.is(net.minecraft.world.item.Items.FILLED_MAP)) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        caszual_additions$renderArm(pose, collector, light, equip, swing, arm);
    }

    @Unique
    private static boolean caszual_additions$renderArm(
            PoseStack pose, SubmitNodeCollector collector, int light,
            float equipProgress, float attackProgress, HumanoidArm arm
    ) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) return false;

        ItemStack chest = Cosmetics.get(player, EquipmentSlot.CHEST);
        boolean aquarium = SpecialCosmetics.isWearableAquarium(chest);
        boolean bone = SpecialCosmetics.isBone(chest);
        boolean rod = SpecialCosmetics.isEndRod(chest);
        boolean snow = !bone && !rod && SpecialCosmetics.isSnowGolem(
                Cosmetics.get(player, EquipmentSlot.HEAD), chest,
                Cosmetics.get(player, EquipmentSlot.LEGS),
                Cosmetics.get(player, EquipmentSlot.FEET));
        boolean blockDisguise = !aquarium && !rod && !snow && !bone
                && chest.getItem() instanceof BlockItem;
        if (!bone && !rod && !snow && !aquarium && !blockDisguise) return false;

        // Only replace the arm: the held item's render pipeline is untouched.
        float sign = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        pose.pushPose();
        pose.translate(sign * (0.67f - 0.08f * attackProgress),
                -0.64f - equipProgress * 0.5f,
                -0.82f + 0.04f * attackProgress);
        pose.mulPose(Axis.YP.rotationDegrees(sign * -31f));
        pose.mulPose(Axis.ZP.rotationDegrees(sign * -21f));
        pose.mulPose(Axis.XP.rotationDegrees(-34f - 22f * attackProgress));

        if (aquarium) {
            // Both bare hands and the arm behind held items use the aquarium,
            // with no dependency on the item being held in either hand.
            boolean slim = player.getSkin().model().toString().equalsIgnoreCase("slim");
            WearableAquariumArmRender.firstPerson(pose, collector, light, slim);
        } else if (blockDisguise) {
            pose.scale(1f / 16f, -1f / 16f, 1f / 16f);
            collector.order(1).submitModelPart(
                    BLOCK_ARM, pose, RenderTypes.entityTranslucent(
                            BlockOutfitTextures.texture(((BlockItem)chest.getItem()).getBlock())),
                    light, OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        } else if (rod) {
            // Exactly the same colored/RGB End Rod block model as third person.
            pose.scale(0.23f, -0.78f, 0.23f);
            EndRodRender.block(chest, pose, collector, 0);
        } else {
            // Thin skeleton/stick geometry, instead of the ordinary skin arm.
            pose.scale(1f / 16f, -1f / 16f, 1f / 16f);
            collector.order(1).submitModelPart(
                    THIN_ARM, pose, RenderTypes.entityCutout(snow ? STICK_TEXTURE : BONE_TEXTURE),
                    light, OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        }
        pose.popPose();
        return true;
    }
}
