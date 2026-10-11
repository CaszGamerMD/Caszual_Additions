package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Warehouse-style pallet inventory, with an explicit distinction between
 * storage shelves, page navigation and the player's own inventory.
 *
 * Slot positions MUST match PalletMenu. The buttons are ordinary GUI widgets,
 * not hand-drawn hotspots, so their text, focus and mouse input stay aligned.
 */
public final class PalletScreen extends AbstractContainerScreen<PalletMenu> {
    private static final int IMAGE_W = 370;
    private static final int IMAGE_H = 296;
    private static final int STORAGE_X = 23;
    private static final int STORAGE_Y = 51;
    private static final int PLAYER_X = 104;
    private static final int PLAYER_Y = 207;

    // Neutral industrial styling, coordinated with Minecraft's own slot colors.
    private static final int OUTER = 0xff111923;
    private static final int HEADER = 0xff253341;
    private static final int FRAME = 0xff344454;
    private static final int CONTENT = 0xffd9e0e6;
    private static final int PANEL = 0xffc3cdd6;
    private static final int SLOT_EDGE = 0xff697781;
    private static final int SLOT_FILL = 0xffe5eaf0;
    private static final int TEXT = 0xff1c2c3a;
    private static final int MUTED = 0xff546473;
    private static final int ACCENT = 0xff3d9c9e;

    private Button previous;
    private Button next;

    public PalletScreen(PalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        // Labels rendered explicitly; no duplicated inventory text.
        this.titleLabelX = 16;
        this.titleLabelY = 16;
        this.inventoryLabelX = PLAYER_X;
        this.inventoryLabelY = 195;
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(
                Component.literal("◀ Previous"), button -> changePage(0))
                .bounds(leftPos + 18, topPos + 163, 92, 21).build());
        next = addRenderableWidget(Button.builder(
                Component.literal("Next ▶"), button -> changePage(1))
                .bounds(leftPos + imageWidth - 110, topPos + 163, 92, 21).build());
        updateControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    private void updateControls() {
        if (previous != null) previous.active = menu.page() > 0;
        if (next != null) next.active = menu.page() < 1;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x = leftPos;
        int y = topPos;

        // Outer shadow, body and title bar.
        graphics.fill(x, y, x + IMAGE_W, y + IMAGE_H, OUTER);
        graphics.fill(x + 2, y + 2, x + IMAGE_W - 2, y + IMAGE_H - 2, FRAME);
        graphics.fill(x + 4, y + 4, x + IMAGE_W - 4, y + IMAGE_H - 4, CONTENT);
        graphics.fill(x + 4, y + 4, x + IMAGE_W - 4, y + 32, HEADER);
        graphics.fill(x + 4, y + 31, x + IMAGE_W - 4, y + 33, ACCENT);

        // Storage shelf, 18 columns x 6 rows, centered with 14px side margins.
        graphics.fill(x + 12, y + 40, x + IMAGE_W - 12, y + 160, PANEL);
        graphics.fill(x + 14, y + 42, x + IMAGE_W - 14, y + 159, SLOT_EDGE);
        drawSlots(graphics, x + STORAGE_X, y + STORAGE_Y,
                PalletMenu.COLS, PalletMenu.ROWS);

        // Navigation background behind the native page buttons.
        graphics.fill(x + 12, y + 161, x + IMAGE_W - 12, y + 187, CONTENT);
        graphics.fill(x + 116, y + 164, x + IMAGE_W - 116, y + 184, HEADER);

        // Subtle separator and a separate player's inventory section.
        graphics.fill(x + 12, y + 190, x + IMAGE_W - 12, y + 191, SLOT_EDGE);
        graphics.fill(x + 12, y + 202, x + IMAGE_W - 12, y + 287, PANEL);
        graphics.fill(x + PLAYER_X - 5, y + PLAYER_Y - 4,
                x + PLAYER_X + 167, y + 262, SLOT_EDGE);
        drawSlots(graphics, x + PLAYER_X, y + PLAYER_Y, 9, 3);

        graphics.fill(x + PLAYER_X - 5, y + 262,
                x + PLAYER_X + 167, y + 286, SLOT_EDGE);
        drawSlots(graphics, x + PLAYER_X, y + PLAYER_Y + 58, 9, 1);
    }

    private static void drawSlots(GuiGraphicsExtractor graphics,
                                  int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int sx = x + col * 18;
                int sy = y + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_FILL);
            }
        }
    }

    @Override
    protected void extractLabels(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY
    ) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xfff1f6fa, false);
        Component capacity = Component.literal("216 SLOTS  /  2 PAGES");
        graphics.text(font, capacity,
                imageWidth - 16 - font.width(capacity), 17, 0xffbed0da, false);

        graphics.text(font, Component.literal("STORAGE"),
                22, 39, TEXT, false);
        Component visible = Component.literal("108 slots per page");
        graphics.text(font, visible,
                imageWidth - 23 - font.width(visible), 39, MUTED, false);

        Component page = Component.literal("PAGE " + (menu.page() + 1) + " / 2");
        graphics.text(font, page,
                (imageWidth - font.width(page)) / 2, 170,
                0xffe8f4f7, false);

        graphics.text(font, Component.literal("PLAYER INVENTORY"),
                PLAYER_X, 194, TEXT, false);

        Component hotbar = Component.literal("HOTBAR");
        graphics.text(font, hotbar,
                imageWidth - 22 - font.width(hotbar), 194, MUTED, false);
    }

    private void changePage(int page) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        if (page == menu.page()) return;
        if (menu.clickMenuButton(minecraft.player, page)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
            updateControls();
        }
    }
}
