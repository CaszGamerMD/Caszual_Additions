package com.caszgamermd.caszualadditions.utils.mixin;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Slot.class)
public interface SlotAccessor {
 @Accessor("x") void caszual_additions$setX(int x);
 @Accessor("y") void caszual_additions$setY(int y);
}
