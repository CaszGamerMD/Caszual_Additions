package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PalletScreen extends AbstractContainerScreen<PalletMenu> {
    private static final int IMAGE_W = 340;
    private static final int IMAGE_H = 222;

    public PalletScreen(PalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 89;
        this.inventoryLabelY = 128;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x0 = leftPos;
        int y0 = topPos;

        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xffc6c6c6);
        graphics.fill(x0 + 3, y0 + 3, x0 + imageWidth - 3, y0 + imageHeight - 3, 0xff5b5b5b);
        graphics.fill(x0 + 5, y0 + 5, x0 + imageWidth - 5, y0 + imageHeight - 5, 0xffc6c6c6);

        drawSlotGrid(graphics, x0 + 7, y0 + 17, PalletMenu.COLS, PalletMenu.ROWS);
        drawSlotGrid(graphics, x0 + 88, y0 + 139, 9, 3);
        drawSlotGrid(graphics, x0 + 88, y0 + 197, 9, 1);

        int buttonY = y0 + 122;
        drawPageButton(graphics, x0 + 8, buttonY, 0, "<");
        drawPageButton(graphics, x0 + imageWidth - 28, buttonY, 1, ">");
    }

    private static void drawSlotGrid(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int cols,
            int rows
    ) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * 18;
                int sy = y + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xff373737);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff8b8b8b);
            }
        }
    }

    private void drawPageButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int page,
            String text
    ) {
        boolean active = menu.page() != page;
        int bg = active ? 0xff777777 : 0xff555555;
        graphics.fill(x, y, x + 20, y + 14, 0xff2b2b2b);
        graphics.fill(x + 1, y + 1, x + 19, y + 13, bg);
        graphics.text(font, text, x + 7, y + 3, active ? 0xffffffff : 0xff999999, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component page = Component.literal("Page " + (menu.page() + 1) + " / 2");
        int x = (imageWidth - font.width(page)) / 2;
        graphics.text(font, page, x, 124, 0xff303030, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() - leftPos;
        double my = event.y() - topPos;

        if (event.button() == 0 && my >= 122 && my < 136) {
            if (mx >= 8 && mx < 28 && menu.page() != 0) {
                changePage(0);
                return true;
            }
            if (mx >= imageWidth - 28 && mx < imageWidth - 8 && menu.page() != 1) {
                changePage(1);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void changePage(int page) {
        if (menu.clickMenuButton(minecraft.player, page)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        }
    }
}
