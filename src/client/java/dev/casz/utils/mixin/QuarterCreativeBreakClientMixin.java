package dev.casz.utils.mixin;

import com.casz.colorfulrods.RgbQuarterBlock;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class QuarterCreativeBreakClientMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$breakOneQuarter(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.player.getAbilities().instabuild) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (!state.is(QuarterBlocks.QUARTER_BLOCK)
                && !(state.getBlock() instanceof RgbQuarterBlock)) {
            return;
        }

        if (QuarterCreativeBreak.removeTargetedCorner(
                minecraft.level,
                minecraft.player,
                pos
        )) {
            cir.setReturnValue(true);
        }
    }
}
