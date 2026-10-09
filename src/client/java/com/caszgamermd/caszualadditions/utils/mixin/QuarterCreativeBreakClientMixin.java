package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Predict only the targeted octant disappearing instead of the full block. */
@Mixin(MultiPlayerGameMode.class)
public abstract class QuarterCreativeBreakClientMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$breakQuarter(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player == null || minecraft.level == null) return;
        if (QuarterCreativeBreak.isQuarterContainer(minecraft.level.getBlockState(pos))) {
            cir.setReturnValue(QuarterCreativeBreak.breakTargeted(
                    minecraft.level, minecraft.player, pos));
        }
    }
}
