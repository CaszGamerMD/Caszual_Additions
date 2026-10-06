package dev.casz.utils.mixin;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Slot.class)
public interface SlotAccessor {
 @Accessor("x") void caszutils$setX(int x);
 @Accessor("y") void caszutils$setY(int y);
}
