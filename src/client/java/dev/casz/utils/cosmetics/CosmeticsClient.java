package dev.casz.utils.cosmetics;

import dev.casz.utils.mixin.ContainerScreenAccessor;
import dev.casz.utils.mixin.ScreenWidgetAccessor;
import dev.casz.utils.mixin.SlotAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class CosmeticsClient {
    private static final int COSMETIC_SLOT_X=-20;
    private static final int HIDDEN_SLOT=-10000;

    private CosmeticsClient(){}

    public static boolean armorHidden(){
        return net.minecraft.client.Minecraft.getInstance().player!=null
            &&Cosmetics.hideArmor(net.minecraft.client.Minecraft.getInstance().player);
    }

    public static Component armorLabel(){
        return Component.translatable(armorHidden()?"gui.caszutils.armor_hidden":"gui.caszutils.armor_shown");
    }

    public static Button armorButton(int x,int y,int width){
        var button=Button.builder(armorLabel(),b->{
            var player=net.minecraft.client.Minecraft.getInstance().player;
            if(player==null)return;
            boolean hidden=!Cosmetics.hideArmor(player);
            ClientPlayNetworking.send(new ToggleArmorVisibility(hidden));
            Cosmetics.setHideArmor(player,hidden);
            b.setMessage(Component.literal(hidden?"▦":"◆"));
        }).bounds(x,y,width,20).build();
        button.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint")));
        return button;
    }

    private static void placeCosmeticSlots(InventoryScreen screen,boolean shown){
        int first=screen.getMenu().slots.size()-4;
        if(first<0)return;
        for(int i=0;i<4;i++){
            var slot=(SlotAccessor)(Object)screen.getMenu().slots.get(first+i);
            slot.caszutils$setX(shown?COSMETIC_SLOT_X:HIDDEN_SLOT);
            slot.caszutils$setY(shown?8+18*i:HIDDEN_SLOT);
        }
    }

    public static void initialize(){
        BlockOutfitTextures.initialize();
        MenuScreens.register(Cosmetics.MENU,CosmeticsScreen::new);

        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof InventoryScreen inventoryScreen))return;

            var positions=(ContainerScreenAccessor)screen;
            var widgets=(ScreenWidgetAccessor)(Object)screen;
            final boolean[] shown={false};

            placeCosmeticSlots(inventoryScreen,false);

            // Vanilla recipe book is at roughly left+104/top+61 in the player inventory.
            // Keep the armor visibility toggle immediately to its right.
            var hide=armorButton(positions.caszutils$left()+126,positions.caszutils$top()+61,20);
            hide.setMessage(Component.literal(armorHidden()?"▦":"◆"));
            hide.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.hide_hint")));

            // Cosmetic slot visibility button: attached to the left edge directly below
            // the cosmetic-slot column, aligned with the first normal inventory row.
            var toggle=Button.builder(Component.literal("▰"),button->{
                shown[0]=!shown[0];
                placeCosmeticSlots(inventoryScreen,shown[0]);
                button.setMessage(Component.literal(shown[0]?"▣":"▰"));
            }).bounds(positions.caszutils$left()-22,positions.caszutils$top()+83,20,20).build();
            toggle.setTooltip(Tooltip.create(Component.translatable("gui.caszutils.cosmetic_hint")));

            widgets.caszutils$addRenderableWidget(hide);
            widgets.caszutils$addRenderableWidget(toggle);

            ScreenEvents.beforeExtract(screen).register((s,graphics,mx,my,partial)->{
                hide.setPosition(positions.caszutils$left()+126,positions.caszutils$top()+61);
                hide.setMessage(Component.literal(armorHidden()?"▦":"◆"));
                toggle.setPosition(positions.caszutils$left()-22,positions.caszutils$top()+83);
                placeCosmeticSlots(inventoryScreen,shown[0]);
                if(shown[0]){
                    int px=positions.caszutils$left()-22;
                    int py=positions.caszutils$top()+6;
                    graphics.fill(px,py,positions.caszutils$left(),py+74,0xff30303b);
                    graphics.fill(px+1,py+1,positions.caszutils$left()-1,py+73,0xffc6c6c6);
                    for(int i=0;i<4;i++){
                        int sy=positions.caszutils$top()+8+18*i;
                        graphics.fill(positions.caszutils$left()-21,sy-1,positions.caszutils$left()-2,sy+18,0xff373737);
                        graphics.fill(positions.caszutils$left()-20,sy,positions.caszutils$left()-3,sy+17,0xff8b8b8b);
                    }
                }
            });
        });
    }
}
