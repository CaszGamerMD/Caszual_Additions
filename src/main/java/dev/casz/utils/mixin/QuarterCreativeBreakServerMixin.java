package dev.casz.utils.mixin;

import com.casz.colorfulrods.RgbQuarterBlock;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.caszgamermd.caszualadditions.quarter.QuarterCreativeBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class QuarterCreativeBreakServerMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$breakOneQuarter(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!player.getAbilities().instabuild) return;

        BlockState state = level.getBlockState(pos);
        if (!state.is(QuarterBlocks.QUARTER_BLOCK)
                && !(state.getBlock() instanceof RgbQuarterBlock)) {
            return;
        }

        if (QuarterCreativeBreak.removeTargetedCorner(level, player, pos)) {
            cir.setReturnValue(true);
        }
    }
}
