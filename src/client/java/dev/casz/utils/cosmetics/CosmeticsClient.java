package dev.casz.utils.cosmetics;

import dev.casz.utils.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;

public final class CosmeticsClient {
    public static Component armorLabel() { return Component.translatable(AppearanceSettings.hideArmor() ? "gui.caszutils.armor_hidden" : "gui.caszutils.armor_shown"); }
    public static Button armorButton(int x, int y, int width) {
        var button = Button.builder(armorLabel(), b -> { AppearanceSettings.toggle(); b.setMessage(armorLabel()); }).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint"))); return button;
    }
    private static int buttonY(net.minecraft.client.gui.screens.Screen screen, ContainerScreenAccessor pos) {
        int gap = screen instanceof CreativeModeInventoryScreen ? 34 : 6;
        return Math.min(screen.height - 22, pos.caszutils$top() + pos.caszutils$height() + gap);
    }
    public static void initialize() {
        AppearanceSettings.load();
        BlockOutfitTextures.initialize();
        MenuScreens.register(Cosmetics.MENU, CosmeticsScreen::new);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
            var positions = (ContainerScreenAccessor)screen;
            var hide = armorButton(positions.caszutils$left(), buttonY(screen, positions), 86);
            var open = Button.builder(Component.translatable("gui.caszutils.cosmetics"), button -> {
                if (ClientPlayNetworking.canSend(OpenCosmetics.TYPE)) ClientPlayNetworking.send(OpenCosmetics.INSTANCE);
            }).bounds(positions.caszutils$left() + 90, buttonY(screen, positions), 86, 20).build();
            open.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.cosmetic_hint")));
            Screens.getWidgets(screen).add(hide); Screens.getWidgets(screen).add(open);
            ScreenEvents.beforeExtract(screen).register((s, graphics, mx, my, partial) -> {
                hide.setPosition(positions.caszutils$left(), buttonY(screen, positions));
                open.setPosition(positions.caszutils$left() + 90, buttonY(screen, positions));
            });
        });
    }
}