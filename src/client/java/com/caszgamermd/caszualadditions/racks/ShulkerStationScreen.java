package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Warehouse terminal with real magazine inventory and three packing lanes. */
public final class ShulkerStationScreen extends AbstractContainerScreen<ShulkerStationMenu> {
    private static final int WIDTH=276,HEIGHT=295;
    public ShulkerStationScreen(ShulkerStationMenu menu,Inventory inventory,
            Component title){
        super(menu,inventory,title,WIDTH,HEIGHT);
        titleLabelX=13;titleLabelY=12;
        inventoryLabelX=8;inventoryLabelY=190;
    }
    @Override protected void renderBg(GuiGraphicsExtractor graphics,float partial,
            int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        graphics.fill(x,y,x+WIDTH,y+HEIGHT,0xff17242b);
        graphics.fill(x+3,y+3,x+WIDTH-3,y+HEIGHT-3,0xff38515a);
        graphics.fill(x+6,y+6,x+WIDTH-6,y+HEIGHT-6,0xffcad1c8);
        graphics.fill(x+8,y+8,x+WIDTH-8,y+29,0xff243a43);
        graphics.fill(x+12,y+35,x+161,y+187,0xff647b78);
        for(int i=0;i<ShulkerStationMenu.STOCK;i++){
            int slotX=x+17+i%8*18,slotY=y+40+i/8*18;
            graphics.fill(slotX-1,slotY-1,slotX+17,slotY+17,0xff354b52);
            graphics.fill(slotX,slotY,slotX+16,slotY+16,0xffabb6b0);
        }
        graphics.fill(x+169,y+35,x+WIDTH-12,y+187,0xff354b52);
        graphics.fill(x+12,y+197,x+WIDTH-12,y+287,0xff647b78);
        for(int row=0;row<4;row++){
            int start=row==3?0:0;
            int count=row==3?9:9;
            int slotY=row==3?y+261:y+203+row*18;
            for(int col=start;col<count;col++){
                int slotX=x+8+col*18;
                graphics.fill(slotX-1,slotY-1,slotX+17,slotY+17,0xff354b52);
                graphics.fill(slotX,slotY,slotX+16,slotY+16,0xffabb6b0);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphicsExtractor graphics,
            int mouseX,int mouseY){
        graphics.text(font,Component.literal("SHULKER STATION"),
                13,14,0xffb9efe0,false);
        graphics.text(font,Component.literal("EMPTY SHULKERS"),17,29,0xff2a414b,false);
        graphics.text(font,Component.literal("3 PACKING LANES"),173,42,0xffc3e9cf,false);
        graphics.text(font,Component.literal("Empties  "+menu.emptyCount()+"/64"),
                174,62,0xffebf6ed,false);
        graphics.text(font,Component.literal("Working  "+menu.packingCount()+"/3"),
                174,83,0xffebf6ed,false);
        graphics.text(font,Component.literal("Ready  "+menu.readyCount()+"/3"),
                174,104,0xffebf6ed,false);
        graphics.text(font,Component.literal("Items  "+menu.bufferedCount()),
                174,125,0xffebf6ed,false);
        graphics.text(font,Component.literal(menu.outputConnected()
                        ?"OUTPUT CONNECTED":"NO OUTPUT ADAPTER"),
                173,154,menu.outputConnected()?0xff8be9ad:0xffffa099,false);
        graphics.text(font,Component.literal("PLAYER INVENTORY"),
                10,190,0xff2a414b,false);
    }
}
