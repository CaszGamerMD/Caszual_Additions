package dev.casz.utils.cosmetics;

import dev.casz.utils.mixin.ContainerScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class CosmeticsClient {
    public static boolean armorHidden(){return net.minecraft.client.Minecraft.getInstance().player!=null&&Cosmetics.hideArmor(net.minecraft.client.Minecraft.getInstance().player);}
    public static Component armorLabel(){return Component.translatable(armorHidden()?"gui.caszutils.armor_hidden":"gui.caszutils.armor_shown");}
    public static Button armorButton(int x,int y,int width){
        var button=Button.builder(armorLabel(),b->{boolean hidden=!armorHidden();ClientPlayNetworking.send(new ToggleArmorVisibility(hidden));Cosmetics.setHideArmor(net.minecraft.client.Minecraft.getInstance().player,hidden);b.setMessage(armorLabel());}).bounds(x,y,width,20).build();
        button.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint")));return button;
    }
    public static void initialize(){
        BlockOutfitTextures.initialize();
        MenuScreens.register(Cosmetics.MENU,CosmeticsScreen::new);
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof InventoryScreen inventoryScreen))return;
            var positions=(ContainerScreenAccessor)screen;
            var hide=armorButton(positions.caszutils$left()+128,positions.caszutils$top()+22,20);
            hide.setMessage(Component.literal(armorHidden()?"C":"D"));
            hide.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint")));
            final boolean[] shown={true};
            var toggle=Button.builder(Component.literal("B"),button->{
                shown[0]=!shown[0];
                int first=inventoryScreen.getMenu().slots.size()-4;
                for(int i=first;i<inventoryScreen.getMenu().slots.size();i++)inventoryScreen.getMenu().slots.get(i).setActive(shown[0]);
            }).bounds(positions.caszutils$left()+58,positions.caszutils$top()+80,20,20).build();
            toggle.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.cosmetic_hint")));
            Screens.getWidgets(screen).add(hide);Screens.getWidgets(screen).add(toggle);
            ScreenEvents.beforeExtract(screen).register((s,graphics,mx,my,partial)->{
                hide.setPosition(positions.caszutils$left()+128,positions.caszutils$top()+22);
                hide.setMessage(Component.literal(armorHidden()?"C":"D"));
                toggle.setPosition(positions.caszutils$left()+58,positions.caszutils$top()+80);
            });
        });
    }
}
