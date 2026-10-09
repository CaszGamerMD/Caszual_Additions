package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Survival AND creative both preserve the other seven octants. */
@Mixin(ServerPlayerGameMode.class)
public abstract class QuarterCreativeBreakServerMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$breakQuarter(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (QuarterCreativeBreak.isQuarterContainer(level.getBlockState(pos))) {
            cir.setReturnValue(QuarterCreativeBreak.breakTargeted(level, player, pos));
        }
    }
}
