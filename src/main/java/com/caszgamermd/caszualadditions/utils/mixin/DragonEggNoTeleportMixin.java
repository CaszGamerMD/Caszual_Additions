package com.caszgamermd.caszualadditions.utils.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the dragon egg stay put when attacked or interacted with. */
@Mixin(DragonEggBlock.class)
public abstract class DragonEggNoTeleportMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void caszualAdditions$disableDragonEggTeleport(
            BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }
}
