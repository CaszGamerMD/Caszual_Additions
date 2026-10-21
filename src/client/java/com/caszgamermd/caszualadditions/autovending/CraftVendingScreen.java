package com.caszgamermd.caszualadditions.autovending;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** All 9 pattern samples and 27 ingredient stacks remain editable.
 * Only the output preview is locked; normal use of the block dispenses it.
 */
public final class CraftVendingScreen extends AbstractContainerScreen<CraftVendingMenu> {
    private static final int W=220,H=295;
    public CraftVendingScreen(CraftVendingMenu menu,Inventory inv,Component title){
        super(menu,inv,title,W,H);
        titleLabelX=11;titleLabelY=9;
        inventoryLabelX=28;inventoryLabelY=196;
    }
    private static void cell(GuiGraphicsExtractor g,int x,int y,int stroke,int fill){
        g.fill(x-1,y-1,x+17,y+17,stroke);
        g.fill(x,y,x+16,y+16,fill);
    }
    private static void button(GuiGraphicsExtractor g,int x,int y,String text){
        g.fill(x,y,x+18,y+15,0xff222f36);
        g.fill(x+1,y+1,x+17,y+14,0xffe2ae43);
        g.text(net.minecraft.client.Minecraft.getInstance().font,
                text,x+6,y+3,0xff1d2830,false);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,
            int mx,int my,float tick){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+W,y+H,0xff161f29);
        g.fill(x+3,y+3,x+W-3,y+H-3,0xff344652);
        g.fill(x+6,y+6,x+W-6,y+H-6,0xffd4dedb);
        g.fill(x+8,y+8,x+W-8,y+27,0xff1e3642);
        g.fill(x+19,y+33,x+89,y+98,0xff61777d);
        g.fill(x+112,y+35,x+200,y+98,0xff81908b);
        for(int i=0;i<9;i++)
            cell(g,x+29+(i%3)*18,y+39+(i/3)*18,0xff34434c,0xffeaf1df);
        cell(g,x+160,y+56,0xff35535a,0xfff5e3a9);
        g.fill(x+24,y+120,x+197,y+188,0xff829b94);
        for(int i=0;i<27;i++)
            cell(g,x+28+(i%9)*18,y+127+(i/9)*18,0xff34434c,0xffe9efe7);
        g.fill(x+24,y+202,x+197,y+289,0xff8a9b98);
        for(int i=0;i<27;i++)
            cell(g,x+28+(i%9)*18,y+208+(i/9)*18,0xff47535a,0xffe2e8df);
        for(int i=0;i<9;i++)
            cell(g,x+28+i*18,y+266,0xff47535a,0xffe2e8df);
        button(g,x+119,y+101,"-");
        button(g,x+179,y+101,"+");
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,title,11,10,0xffebf9f2,false);
        g.text(font,Component.literal("RECIPE"),28,29,0xff344552,false);
        g.text(font,Component.literal("RESULT"),141,39,0xff344552,false);
        g.text(font,Component.literal("STORAGE - 27 SLOTS"),28,113,0xff344552,false);
        ItemStack result=menu.output();
        String amount=""+menu.outputCount();
        g.text(font,Component.literal("x"+amount),143,103,0xff263e42,false);
        if(!result.isEmpty())
            g.text(font,Component.literal("Per craft: "+result.getCount()),
                    116,84,0xff203c42,false);
        else g.text(font,Component.literal("Set recipe"),118,84,0xff4c5a60,false);
        g.text(font,Component.literal("PLAYER INVENTORY"),28,195,0xff33434b,false);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0){
            double x=event.x()-leftPos,y=event.y()-topPos;
            if(y>=101&&y<116){
                if(x>=119&&x<137){adjust(0);return true;}
                if(x>=179&&x<197){adjust(1);return true;}
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    private void adjust(int id){
        if(menu.clickMenuButton(minecraft.player,id))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
}
