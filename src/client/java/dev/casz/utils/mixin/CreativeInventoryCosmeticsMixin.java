package dev.casz.utils.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryCosmeticsMixin {
    private static final int[] CASZ_X = {132, 150, 132, 150};
    private static final int[] CASZ_Y = {6, 6, 33, 33};

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void caszutils$positionCosmeticSlots(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;

        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
        int found = 0;

        for (Slot slot : screen.getMenu().slots) {
            int targetIndex = slot.getContainerSlot();
            if (targetIndex < 46 || targetIndex > 49) continue;

            int cosmeticIndex = targetIndex - 46;
            ((SlotAccessor)(Object)slot).caszutils$setX(CASZ_X[cosmeticIndex]);
            ((SlotAccessor)(Object)slot).caszutils$setY(CASZ_Y[cosmeticIndex]);
            found++;
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void caszutils$drawCosmeticSlotBackgrounds(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
        if (!screen.isInventoryOpen()) return;

        ContainerScreenAccessor pos = (ContainerScreenAccessor)(Object)screen;
        int left = pos.caszutils$left();
        int top = pos.caszutils$top();

        for (int i = 0; i < 4; i++) {
            int x = left + CASZ_X[i];
            int y = top + CASZ_Y[i];
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
            graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
        }
    }
}
