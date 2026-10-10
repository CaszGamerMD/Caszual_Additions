package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import com.caszgamermd.caszualadditions.quarter.QuarterBreakNetworking;
import com.caszgamermd.caszualadditions.quarter.BoinkrItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        // This hammer cannot destroy normal full blocks, even in Creative.
        // Its only action on them is Boink!'s right-click conversion.
        if (minecraft.player.getMainHandItem().getItem() instanceof BoinkrItem
                && !QuarterCreativeBreak.isQuarterContainer(minecraft.level.getBlockState(pos))) {
            cir.setReturnValue(false);
            return;
        }
        if (QuarterCreativeBreak.isQuarterContainer(minecraft.level.getBlockState(pos))) {
            // Minecraft normally sends a breaking packet here. Cancelling
            // destroyBlock stopped that packet, so a mined baby block was
            // restored by the server. Tell the server explicitly, then do
            // local prediction for responsive visual feedback.
            if (minecraft.player.getMainHandItem().getItem() instanceof BoinkrItem
                    && BoinkrItem.mode(minecraft.player.getMainHandItem()) == BoinkrItem.Mode.BOINK) {
                cir.setReturnValue(true);
                return;
            }
            ClientPlayNetworking.send(new QuarterBreakNetworking.BreakQuarter(pos));
            QuarterCreativeBreak.breakTargeted(minecraft.level, minecraft.player, pos);
            cir.setReturnValue(true);
        }
    }
}
