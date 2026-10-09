package com.caszgamermd.caszualadditions.utils.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CosmeticSkullScaleMixin {
    @Inject(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HeadedModel;translateToHead(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER))
    private void caszual_additions$keepBlockSize(
            PoseStack pose, SubmitNodeCollector collector, int light,
            LivingEntityRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar) {
            if (SpecialCosmetics.hasBlockHead(avatar.headEquipment))
                pose.scale(1f / .99f, 1f / .99f, 1f / .99f);
            if (SpecialCosmetics.isMobHead(avatar.headEquipment)) {
                // Keep the existing facing correction. Sizing happens below,
                // consistently for both player skulls and mob skulls.
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            }
        }
    }

    @ModifyArgs(method = "submit", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1))
    private void caszual_additions$fullSizeHead(
            Args args, PoseStack pose, SubmitNodeCollector collector, int light,
            LivingEntityRenderState state, float yaw, float pitch) {
        if (state instanceof AvatarRenderState avatar
                && (SpecialCosmetics.isPlayerHead(avatar.headEquipment)
                || SpecialCosmetics.isMobHead(avatar.headEquipment))) {
            // Vanilla renders skulls smaller than a skin's head. Player-head
            // cosmetics already bypass this; now mob heads do as well.
            args.set(0, 1f);
            args.set(1, 1f);
            args.set(2, 1f);
        }
    }
}
