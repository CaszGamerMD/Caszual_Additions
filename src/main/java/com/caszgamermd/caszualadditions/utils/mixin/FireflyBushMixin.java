package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.FireflyGlass;
import com.caszgamermd.caszualadditions.utils.FireflyGlassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireflyBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireflyBushBlock.class)
public abstract class FireflyBushMixin {
    @Redirect(method = "animateTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void caszual_additions$tint(Level receiver, ParticleOptions particle, double x, double y, double z,
            double dx, double dy, double dz, BlockState state, Level level, BlockPos bush, RandomSource random) {
        if (state.getBlock() instanceof FireflyGlassBlock cover && state.getValue(FireflyGlassBlock.CONTAINS_BUSH) && cover.color() != null) {
            particle = FireflyGlass.PARTICLES.get(cover.color());
        }
        receiver.addParticle(particle, x, y, z, dx, dy, dz);
    }
}