package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.racks.PalletJackAvatarPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Do not inspect live player entities from the render thread. */
@Mixin(AvatarRenderState.class)
public abstract class PalletJackAvatarStateMixin implements PalletJackAvatarPose {
    @Unique private boolean caszual_additions$pullingJack;
    @Unique private float caszual_additions$jackWalking;

    @Override public void caszual_additions$setPullingJack(
            boolean pulling,float walking) {
        caszual_additions$pullingJack=pulling;
        caszual_additions$jackWalking=pulling?walking:0f;
    }
    @Override public boolean caszual_additions$isPullingJack(){
        return caszual_additions$pullingJack;
    }
    @Override public float caszual_additions$walkingJack(){
        return caszual_additions$jackWalking;
    }
}
