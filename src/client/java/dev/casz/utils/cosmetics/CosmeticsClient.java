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
    public static boolean armorHidden() { return net.minecraft.client.Minecraft.getInstance().player != null && Cosmetics.hideArmor(net.minecraft.client.Minecraft.getInstance().player); }
    public static Component armorLabel() { return Component.translatable(armorHidden() ? "gui.caszutils.armor_hidden" : "gui.caszutils.armor_shown"); }
    public static Button armorButton(int x, int y, int width) {
        var button = Button.builder(armorLabel(), b -> { boolean hidden=!armorHidden(); ClientPlayNetworking.send(new ToggleArmorVisibility(hidden)); Cosmetics.setHideArmor(net.minecraft.client.Minecraft.getInstance().player, hidden); b.setMessage(armorLabel()); }).bounds(x, y, width, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint"))); return button;
    }
    public static void initialize() {
        BlockOutfitTextures.initialize();
        MenuScreens.register(Cosmetics.MENU, CosmeticsScreen::new);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
            var positions = (ContainerScreenAccessor)screen;
            int recipeX = positions.caszutils$left() + 104;
            int recipeY = positions.caszutils$top() + 22;
            var hide = armorButton(recipeX + 24, recipeY, 20);
            hide.setMessage(Component.literal(armorHidden() ? "⛓" : "◆"));
            hide.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint")));
            var open = Button.builder(Component.literal("🖌"), button -> {
                if (ClientPlayNetworking.canSend(OpenCosmetics.TYPE)) ClientPlayNetworking.send(OpenCosmetics.INSTANCE);
            }).bounds(positions.caszutils$left() + 7, positions.caszutils$top() + 80, 20, 20).build();
            open.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.cosmetic_hint")));
            Screens.getWidgets(screen).add(hide); Screens.getWidgets(screen).add(open);
            ScreenEvents.beforeExtract(screen).register((s, graphics, mx, my, partial) -> {
                hide.setPosition(positions.caszutils$left() + 128, positions.caszutils$top() + 22);
                hide.setMessage(Component.literal(armorHidden() ? "⛓" : "◆"));
                open.setPosition(positions.caszutils$left() + 7, positions.caszutils$top() + 80);
            });
        });
    }
}