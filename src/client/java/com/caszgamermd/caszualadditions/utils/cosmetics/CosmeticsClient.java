package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.caszgamermd.caszualadditions.utils.mixin.ContainerScreenAccessor;
import com.caszgamermd.caszualadditions.utils.mixin.ScreenWidgetAccessor;
import com.caszgamermd.caszualadditions.utils.mixin.SlotAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticsClient {
    private static final int COSMETIC_SLOT_X = -20;
    private static final int HIDDEN_SLOT = -10000;

    private CosmeticsClient() {}

    public static boolean armorHidden() {
        var player = Minecraft.getInstance().player;
        return player != null && Cosmetics.hideArmor(player);
    }

    public static Component armorLabel() {
        return Component.translatable(armorHidden()
                ? "gui.caszual_additions.armor_hidden"
                : "gui.caszual_additions.armor_shown");
    }

    private static Component armorTooltip() {
        return Component.literal(armorHidden()
                ? "Armor hidden - click to show your armor"
                : "Armor visible - click to hide your armor");
    }

    private static Component cosmeticTooltip(boolean shown) {
        return Component.literal(shown
                ? "Cosmetic slots open - click to close"
                : "Cosmetic slots closed - click to open");
    }

    public static Button armorButton(int x, int y, int width) {
        boolean iconOnly = width <= 22;
        var button = Button.builder(iconOnly ? Component.empty() : armorLabel(), b -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            boolean hidden = !Cosmetics.hideArmor(player);
            ClientPlayNetworking.send(new ToggleArmorVisibility(hidden));
            Cosmetics.setHideArmor(player, hidden);
            b.setMessage(iconOnly ? Component.empty() : armorLabel());
            b.setTooltip(Tooltip.create(armorTooltip()));
        }).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(armorTooltip()));
        return button;
    }

    private static void placeCosmeticSlots(InventoryScreen screen, boolean shown) {
        int first = screen.getMenu().slots.size() - 4;
        if (first < 0) return;
        for (int i = 0; i < 4; i++) {
            var slot = (SlotAccessor)(Object)screen.getMenu().slots.get(first + i);
            slot.caszual_additions$setX(shown ? COSMETIC_SLOT_X : HIDDEN_SLOT);
            slot.caszual_additions$setY(shown ? 8 + 18 * i : HIDDEN_SLOT);
        }
    }

    public static void initialize() {
        BlockOutfitTextures.initialize();
        MenuScreens.register(Cosmetics.MENU, CosmeticsScreen::new);

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) return;

            var positions = (ContainerScreenAccessor)screen;
            var widgets = (ScreenWidgetAccessor)(Object)screen;
            // Restore the preference for this account, including after reopening the
            // inventory or restarting the game. The preference is stored per UUID.
            final boolean[] shown = {CosmeticSlotPreferences.expanded(client)};
            placeCosmeticSlots(inventoryScreen, shown[0]);

            int left = positions.caszual_additions$left();
            int top = positions.caszual_additions$top();
            // Separate, recognizable chestplate icons distinguish visible / hidden armor.
            var hide = armorButton(left + 126, top + 61, 20);
            var toggle = Button.builder(Component.empty(), button -> {
                shown[0] = !shown[0];
                CosmeticSlotPreferences.setExpanded(client, shown[0]);
                placeCosmeticSlots(inventoryScreen, shown[0]);
                button.setTooltip(Tooltip.create(cosmeticTooltip(shown[0])));
            }).bounds(left - 22, top + 83, 20, 20).build();
            toggle.setTooltip(Tooltip.create(cosmeticTooltip(shown[0])));

            widgets.caszual_additions$addRenderableWidget(hide);
            widgets.caszual_additions$addRenderableWidget(toggle);

            ScreenEvents.beforeExtract(screen).register((s, graphics, mx, my, partial) -> {
                int x = positions.caszual_additions$left();
                int y = positions.caszual_additions$top();
                hide.setPosition(x + 126, y + 61);
                hide.setTooltip(Tooltip.create(armorTooltip()));
                toggle.setPosition(x - 22, y + 83);
                placeCosmeticSlots(inventoryScreen, shown[0]);
                if (shown[0]) {
                    int px = x - 22;
                    int py = y + 6;
                    graphics.fill(px, py, x, py + 74, 0xff30303b);
                    graphics.fill(px + 1, py + 1, x - 1, py + 73, 0xffc6c6c6);
                    for (int i = 0; i < 4; i++) {
                        int sy = y + 8 + 18 * i;
                        graphics.fill(x - 21, sy - 1, x - 2, sy + 18, 0xff373737);
                        graphics.fill(x - 20, sy, x - 3, sy + 17, 0xff8b8b8b);
                    }
                }
            });

            // Draw real Minecraft item icons instead of font-dependent glyphs (the
            // old unicode symbols rendered as tiny dots/dashes on some systems).
            ScreenEvents.afterExtract(screen).register((s, graphics, mx, my, partial) -> {
                if (!hide.visible || !toggle.visible) return;
                int x = positions.caszual_additions$left();
                int y = positions.caszual_additions$top();
                graphics.item(new ItemStack(armorHidden()
                        ? Items.CHAINMAIL_CHESTPLATE : Items.DIAMOND_CHESTPLATE),
                        x + 128, y + 63);
                graphics.item(new ItemStack(Items.ARMOR_STAND), x - 20, y + 85);
                // Small state pip: green = cosmetic slots open; grey = closed.
                graphics.fill(x - 7, y + 98, x - 4, y + 101,
                        shown[0] ? 0xff62e69a : 0xff666666);
            });
        });
    }
}
