package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.SilkTouchSpecialBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reharvesting a preserved spawner must not become an unlimited XP farm. */
@Mixin(SpawnerBlock.class)
public abstract class SpawnerSilkTouchXpMixin {
    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$noXpWhenSilkTouched(
            BlockState state, ServerLevel level, BlockPos pos,
            ItemStack tool, boolean dropExperience, CallbackInfo ci) {
        if (SilkTouchSpecialBlocks.hasSilkTouch(level, tool)) {
            ci.cancel();
        }
    }
}
