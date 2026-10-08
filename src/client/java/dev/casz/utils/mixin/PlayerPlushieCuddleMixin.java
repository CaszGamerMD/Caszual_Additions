package dev.casz.utils.mixin;

import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
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
