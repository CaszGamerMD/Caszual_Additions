package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import com.caszgamermd.caszualadditions.quarter.QuarterBreakNetworking;
import com.caszgamermd.caszualadditions.quarter.BoinkrItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    /**
     * Handle the Boink'r at the very first attack, before Minecraft emits its
     * vanilla START_DESTROY_BLOCK packet. That would otherwise race with our
     * custom quarter break packet and sometimes destroy TWO corners per hit.
     * The normal mining pipeline still applies to all other held items.
     */
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$boinkrAttack(BlockPos pos, Direction face,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player == null || minecraft.level == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof BoinkrItem)) return;
        boolean quarter = QuarterCreativeBreak.isQuarterContainer(minecraft.level.getBlockState(pos));
        if (quarter && BoinkrItem.mode(minecraft.player.getMainHandItem()) != BoinkrItem.Mode.BOINK) {
            // The server performs the real mining and sends the new blockstate
            // back. No speculative local removal in protected claims.
            ClientPlayNetworking.send(new QuarterBreakNetworking.BreakQuarter(pos));
            cir.setReturnValue(true);
        } else {
            // Normal blocks are never mineable with this tool, and Boink!
            // mode changes shapes only via its right-click action.
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$breakQuarter(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player == null || minecraft.level == null) return;
        // Safety net for direct destroyBlock calls by other code: the
        // Boink'r is handled only in startDestroyBlock above.
        if (minecraft.player.getMainHandItem().getItem() instanceof BoinkrItem) {
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
