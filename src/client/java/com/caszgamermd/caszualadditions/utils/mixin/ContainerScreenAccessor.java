package com.caszgamermd.caszualadditions.utils.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int caszual_additions$left();
    @Accessor("topPos") int caszual_additions$top();
    @Accessor("imageHeight") int caszual_additions$height();
}