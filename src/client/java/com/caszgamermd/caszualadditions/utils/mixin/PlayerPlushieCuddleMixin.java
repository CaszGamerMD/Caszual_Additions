package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.racks.PalletJackAvatarPose;
import net.minecraft.util.Mth;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerPlushieCuddleMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void caszualAdditions$cuddlePlushie(AvatarRenderState state, CallbackInfo ci) {
        if(((PalletJackAvatarPose)state).caszual_additions$isPullingJack()){
            PlayerModel model=(PlayerModel)(Object)this;
            // Extend both hands BACK toward the horizontal handle bar.
            // Standing legs continue a short alternating walking stride
            // as the pallet jack moves, rather than bending into a seat.
            model.rightArm.xRot=1.06F;
            model.leftArm.xRot=1.06F;
            model.rightArm.yRot=-.12F;
            model.leftArm.yRot=.12F;
            model.rightArm.zRot=-.07F;
            model.leftArm.zRot=.07F;
            float stride=Mth.sin(state.ageInTicks*6.7F)
                    *((PalletJackAvatarPose)state).caszual_additions$walkingJack();
            model.rightLeg.xRot=stride;
            model.leftLeg.xRot=-stride;
            return;
        }
        boolean rightPlushie = state.rightHandItemStack.is(PlayerPlushies.PLAYER_PLUSHIE_ITEM);
        boolean leftPlushie = state.leftHandItemStack.is(PlayerPlushies.PLAYER_PLUSHIE_ITEM);
        if (!rightPlushie && !leftPlushie) return;

        PlayerModel model = (PlayerModel)(Object)this;
        boolean rightFree = state.rightHandItemStack.isEmpty() || rightPlushie;
        boolean leftFree = state.leftHandItemStack.isEmpty() || leftPlushie;

        if ((rightPlushie && leftFree) || (leftPlushie && rightFree)) {
            model.rightArm.xRot = -1.12F;
            model.rightArm.yRot = -0.52F;
            model.rightArm.zRot = 0.16F;

            model.leftArm.xRot = -1.12F;
            model.leftArm.yRot = 0.52F;
            model.leftArm.zRot = -0.16F;
            return;
        }

        if (rightPlushie) {
            model.rightArm.xRot = -1.02F;
            model.rightArm.yRot = -0.38F;
            model.rightArm.zRot = 0.10F;
        }

        if (leftPlushie) {
            model.leftArm.xRot = -1.02F;
            model.leftArm.yRot = 0.38F;
            model.leftArm.zRot = -0.10F;
        }
    }
}
