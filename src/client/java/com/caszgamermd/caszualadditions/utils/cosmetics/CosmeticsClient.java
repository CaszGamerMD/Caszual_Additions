package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.caszgamermd.caszualadditions.utils.mixin.ContainerScreenAccessor;
import com.caszgamermd.caszualadditions.utils.mixin.ScreenWidgetAccessor;
import com.caszgamermd.caszualadditions.utils.mixin.SlotAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
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

    /**
     * Draw the armor icon INSIDE the button's normal extraction phase.
     * The previous ScreenEvents.afterExtract rendered it after item tooltips,
     * causing the chestplate to float visibly over tooltip text.
     */
    private static final class ItemIconButton extends Button {
        private final java.util.function.Supplier<ItemStack> icon;

        private ItemIconButton(Builder builder,
                java.util.function.Supplier<ItemStack> icon) {
            super(builder);
            this.icon = icon;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX,
                                    int mouseY, float partialTick) {
            super.extractContents(graphics, mouseX, mouseY, partialTick);
            graphics.item(icon.get(), getX() + 2, getY() + 2);
        }
    }

    public static Button armorButton(int x, int y, int width) {
        boolean iconOnly = width <= 22;
        Button.OnPress action = b -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            boolean hidden = !Cosmetics.hideArmor(player);
            ClientPlayNetworking.send(new ToggleArmorVisibility(hidden));
            Cosmetics.setHideArmor(player, hidden);
            b.setMessage(iconOnly ? Component.empty() : armorLabel());
            b.setTooltip(Tooltip.create(armorTooltip()));
        };
        var builder = Button.builder(iconOnly ? Component.empty() : armorLabel(), action)
                .bounds(x, y, width, 20);
        Button button = iconOnly
                ? new ItemIconButton(builder,
                        () -> new ItemStack(armorHidden()
                                ? Items.CHAINMAIL_CHESTPLATE : Items.DIAMOND_CHESTPLATE))
                : builder.build();
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
            // Creative mode must use the server-backed cosmetics menu,
            // but the old wide "Cosmetics" text button covered creative slots.
            // Replace it with a compact armor-stand icon outside the item grid.
            if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
                var pos = (ContainerScreenAccessor)screen;
                var widgets = (ScreenWidgetAccessor)(Object)screen;
                var open = new ItemIconButton(
                        Button.builder(Component.empty(),
                                b -> ClientPlayNetworking.send(new OpenCosmetics()))
                                .bounds(pos.caszual_additions$left() - 22,
                                        pos.caszual_additions$top() + 108, 20, 20),
                        () -> new ItemStack(Items.ARMOR_STAND));
                open.setTooltip(Tooltip.create(
                        Component.literal("Open cosmetic slots")));
                widgets.caszual_additions$addRenderableWidget(open);
                ScreenEvents.beforeExtract(screen).register((s, graphics, mx, my, partial) -> {
                    int left = pos.caszual_additions$left();
                    int top = pos.caszual_additions$top();
                    open.setPosition(left - 22, top + 108);
                    open.visible = creativeScreen.isInventoryOpen();
                    open.active = open.visible;
                });
                return;
            }
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
            var toggle = new ItemIconButton(
                    Button.builder(Component.empty(), button -> {
                        shown[0] = !shown[0];
                        CosmeticSlotPreferences.setExpanded(client, shown[0]);
                        placeCosmeticSlots(inventoryScreen, shown[0]);
                        button.setTooltip(Tooltip.create(cosmeticTooltip(shown[0])));
                    }).bounds(left - 22, top + 83, 20, 20),
                    () -> new ItemStack(Items.ARMOR_STAND));
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

            // Both item icons are rendered by their own Button during normal
            // widget extraction, before tooltips. Never draw via AFTER_EXTRACT.
        });
    }
}
