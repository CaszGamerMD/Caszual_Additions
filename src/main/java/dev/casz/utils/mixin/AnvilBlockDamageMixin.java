package dev.casz.utils.mixin;

import com.caszgamermd.caszualadditions.unbreakable.UnbreakableContent;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private static void caszualAdditions$keepUnbreakableAnvilIntact(
            BlockState state,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (UnbreakableContent.UNBREAKABLE_ANVIL != null && state.is(UnbreakableContent.UNBREAKABLE_ANVIL)) {
            cir.setReturnValue(state);
        }
    }
}
