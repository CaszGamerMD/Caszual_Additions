package com.caszgamermd.caszualadditions.funbarrel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Paged creative-only collection. All 54 catalog slots and the normal player
 * inventory are interactable regardless of how many Caszual mods are installed.
 */
public final class FunBarrelScreen extends AbstractContainerScreen<FunBarrelMenu> {
    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 222;

    public FunBarrelScreen(FunBarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x0 = leftPos;
        int y0 = topPos;

        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xff2d1f33);
        graphics.fill(x0 + 3, y0 + 3, x0 + imageWidth - 3, y0 + imageHeight - 3, 0xff786b83);
        graphics.fill(x0 + 5, y0 + 5, x0 + imageWidth - 5, y0 + imageHeight - 5, 0xffded5bd);
        drawGrid(graphics, x0 + 7, y0 + 17, 9, 6);
        drawGrid(graphics, x0 + 7, y0 + 139, 9, 3);
        drawGrid(graphics, x0 + 7, y0 + 197, 9, 1);

        drawPageButton(graphics, x0 + 8, y0 + 122, menu.page() > 0, "<");
        drawPageButton(graphics, x0 + imageWidth - 28, y0 + 122,
                menu.page() + 1 < menu.pageCount(), ">");
    }

    private static void drawGrid(GuiGraphicsExtractor graphics, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int px = x + col * 18;
                int py = y + row * 18;
                graphics.fill(px, py, px + 18, py + 18, 0xff4b3c50);
                graphics.fill(px + 1, py + 1, px + 17, py + 17, 0xffb1a7b8);
            }
        }
    }

    private static void drawPageButton(
            GuiGraphicsExtractor graphics, int x, int y, boolean enabled, String label) {
        graphics.fill(x, y, x + 20, y + 14, 0xff302037);
        graphics.fill(x + 1, y + 1, x + 19, y + 13,
                enabled ? 0xff7d598f : 0xff655e69);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, label, x + 7, y + 3,
                enabled ? 0xffffffff : 0xff9f979f, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component label = Component.literal((menu.page() + 1) + "/" + menu.pageCount()
                + " · " + menu.itemCount() + " items");
        graphics.text(font, label, (imageWidth - font.width(label)) / 2, 124,
                0xff47314b, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x() - leftPos;
        double y = event.y() - topPos;
        if (event.button() == 0 && y >= 122 && y < 136) {
            if (x >= 8 && x < 28 && menu.page() > 0) {
                turnPage(menu.page() - 1);
                return true;
            }
            if (x >= imageWidth - 28 && x < imageWidth - 8
                    && menu.page() + 1 < menu.pageCount()) {
                turnPage(menu.page() + 1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void turnPage(int page) {
        if (menu.clickMenuButton(minecraft.player, page)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        }
    }
}
