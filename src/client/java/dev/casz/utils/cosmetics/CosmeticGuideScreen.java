package dev.casz.utils.cosmetics;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class CosmeticGuideScreen extends Screen {
    private int index;
    private int category;
    private final java.util.ArrayList<Button> categoryButtons = new java.util.ArrayList<>();

    public CosmeticGuideScreen() {
        super(Component.literal("Cosmetic Guide"));
    }

    @Override
    public void removed() {
        CosmeticPreviewState.end();
        super.removed();
    }

    private List<CosmeticGuideCatalog.Entry> visible() {
        String cat = CosmeticGuideCatalog.CATEGORIES[category];
        return cat.equals("All")
                ? CosmeticGuideCatalog.ENTRIES
                : CosmeticGuideCatalog.ENTRIES.stream()
                        .filter(e -> e.category().equals(cat)
                                || e.slots().contains(cat)
                                || e.slots().equals("All slots")
                                || e.slots().equals("Any slot")
                                || e.slots().equals("Full set"))
                        .toList();
    }

    private void clamp() {
        var entries = visible();
        if (entries.isEmpty()) index = 0;
        else index = Math.floorMod(index, entries.size());
    }

    private void selectCategory(int value) {
        category = value;
        index = 0;
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).active = i != category;
        }
    }

    @Override
    protected void init() {
        categoryButtons.clear();
        CosmeticPreviewState.end();

        int left = width / 2 - 130;
        int top = height / 2 - 104;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            index--;
            clamp();
        }).bounds(left + 12, top + 184, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            index++;
            clamp();
        }).bounds(left + 224, top + 184, 22, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 34, top + 184, 68, 18)
                .build());

        for (int i = 0; i < CosmeticGuideCatalog.CATEGORIES.length; i++) {
            final int selected = i;
            int row = i / 4;
            int col = i % 4;
            Button button = Button.builder(
                            Component.literal(CosmeticGuideCatalog.CATEGORIES[i]),
                            b -> selectCategory(selected))
                    .bounds(left + 8 + col * 61, top + 7 + row * 18, 58, 16)
                    .build();
            categoryButtons.add(button);
            addRenderableWidget(button);
        }

        selectCategory(category);
    }

    private int wrapped(
            GuiGraphicsExtractor graphics,
            String text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(next) > maxWidth) {
                graphics.text(font, Component.literal(line.toString()), x, y, color, false);
                y += 10;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }

        if (!line.isEmpty()) {
            graphics.text(font, Component.literal(line.toString()), x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void preview(
            GuiGraphicsExtractor graphics,
            CosmeticGuideCatalog.Entry entry,
            int left,
            int top,
            int mouseX,
            int mouseY
    ) {
        if (minecraft.player == null) return;

        try {
            CosmeticPreviewState.begin(
                    minecraft.player,
                    entry.head(),
                    entry.chest(),
                    entry.legs(),
                    entry.feet()
            );
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    left + 18,
                    top + 92,
                    left + 111,
                    top + 174,
                    32,
                    .0625f,
                    mouseX,
                    mouseY,
                    minecraft.player
            );
        } finally {
            CosmeticPreviewState.end();
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int left = width / 2 - 130;
        int top = height / 2 - 104;
        int right = width / 2 + 130;
        int bottom = height / 2 + 104;
        int fold = width / 2;

        graphics.fill(left, top, right, bottom, 0xff8c6d42);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0xffefdca8);
        graphics.fill(fold - 2, top + 3, fold + 2, bottom - 3, 0xff9a7b4f);

        var entries = visible();
        if (entries.isEmpty()) return;

        clamp();
        var entry = entries.get(index);

        graphics.text(font, Component.literal("Cosmetic Guide"), left + 12, top + 45, 0xff3b2b1c, false);
        graphics.text(
                font,
                Component.literal(CosmeticGuideCatalog.CATEGORIES[category] + "  " + (index + 1) + "/" + entries.size()),
                left + 12,
                top + 58,
                0xff6a5132,
                false
        );
        graphics.text(font, Component.literal("Preview only - nothing is equipped"), left + 12, top + 71, 0xff7a6244, false);
        graphics.text(font, Component.literal("Choose a category, then use <  >"), left + 12, top + 82, 0xff7a6244, false);

        preview(graphics, entry, left, top, mouseX, mouseY);

        int x = fold + 11;
        int y = top + 45;
        int textWidth = 112;

        graphics.text(font, Component.literal(entry.name()), x, y, 0xff2f2116, false);
        y += 14;
        graphics.text(font, Component.literal("Example: " + entry.item().getHoverName().getString()), x, y, 0xff4a3925, false);
        y += 12;
        graphics.text(font, Component.literal("Slots: " + entry.slots()), x, y, 0xff4a3925, false);
        y += 13;
        graphics.text(font, Component.literal("Required:"), x, y, 0xff6a5132, false);
        y += 11;
        y = wrapped(graphics, entry.required(), x, y, textWidth, 0xff4a3925) + 3;
        graphics.text(font, Component.literal("Effect:"), x, y, 0xff6a5132, false);
        y += 11;
        wrapped(graphics, entry.description(), x, y, textWidth, 0xff4a3925);
    }
}
