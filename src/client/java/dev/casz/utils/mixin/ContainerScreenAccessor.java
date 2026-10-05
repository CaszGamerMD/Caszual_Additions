package dev.casz.utils.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int caszutils$left();
    @Accessor("topPos") int caszutils$top();
    @Accessor("imageHeight") int caszutils$height();
}