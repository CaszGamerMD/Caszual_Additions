package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact storage GUI: twelve distinct pallets, no overlapping buttons. */
public final class StackOfPalletsScreen extends AbstractContainerScreen<StackOfPalletsMenu> {
    private static final int WIDTH=176,HEIGHT=205;
    public StackOfPalletsScreen(StackOfPalletsMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,WIDTH,HEIGHT);
        titleLabelX=10;titleLabelY=10;
        inventoryLabelX=8;inventoryLabelY=99;
    }
    private static void slot(GuiGraphicsExtractor g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,0xff475457);
        g.fill(x,y,x+16,y+16,0xffdce5df);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,
            int mouseY,float partialTick){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+WIDTH,y+HEIGHT,0xff18262b);
        g.fill(x+3,y+3,x+WIDTH-3,y+HEIGHT-3,0xff395258);
        g.fill(x+6,y+6,x+WIDTH-6,y+HEIGHT-6,0xffccd7d0);
        g.fill(x+8,y+8,x+WIDTH-8,y+27,0xff263f47);
        g.fill(x+22,y+30,x+154,y+91,0xff738681);
        for(int i=0;i<StackOfPalletsMenu.CAPACITY;i++)
            slot(g,x+52+(i%4)*18,y+35+(i/4)*18);
        g.fill(x+6,y+106,x+170,y+195,0xff738681);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            slot(g,x+8+col*18,y+111+row*18);
        for(int col=0;col<9;col++)slot(g,x+8+col*18,y+169);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        g.text(font,title,10,10,0xffd4ffea,false);
        g.text(font,Component.literal("12 PALLET SLOTS"),46,27,0xff344a4b,false);
        g.text(font,Component.literal("PLAYER INVENTORY"),8,99,0xff344a4b,false);
    }
}
