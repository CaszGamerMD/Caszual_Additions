package com.caszgamermd.caszualadditions.utils.cosmetics;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CosmeticsScreen extends AbstractContainerScreen<CosmeticsMenu> {
    public CosmeticsScreen(CosmeticsMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 192); inventoryLabelY = 98; }
    @Override protected void init() { super.init(); addRenderableWidget(CosmeticsClient.armorButton(leftPos + 88, topPos + 84, 80)); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mx, int my, float partial) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, 0xff30303b);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff373737);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff8b8b8b);
        }
        if (minecraft.player != null) InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
            leftPos + 30, topPos + 18, leftPos + 83, topPos + 94, 30, .0625f, mx, my, minecraft.player);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mx, int my) {
        graphics.text(font, title, 8, 5, 0xff30303b, false);
        String[] labels = {"Head", "Chest", "Legs", "Feet"};
        for (int i = 0; i < 4; i++) graphics.text(font, Component.literal(labels[i]), 88, 20 + 18 * i, 0xff30303b, false);
        graphics.text(font, Component.translatable("container.inventory"), 8, inventoryLabelY, 0xff30303b, false);
    }
}