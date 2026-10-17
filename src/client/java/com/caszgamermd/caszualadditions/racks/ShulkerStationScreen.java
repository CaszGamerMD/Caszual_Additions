package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Each lane and visible box slot is backed by the real station inventory. */
public final class ShulkerStationScreen extends AbstractContainerScreen<ShulkerStationMenu> {
    private static final int WIDTH=380,HEIGHT=375;
    private final Button[] laneButtons=new Button[3];
    private Button loadButton,sendButton;

    public ShulkerStationScreen(ShulkerStationMenu menu,Inventory inventory,
            Component title){
        super(menu,inventory,title,WIDTH,HEIGHT);
        titleLabelX=13;
        titleLabelY=12;
        inventoryLabelX=110;
        inventoryLabelY=270;
    }
    @Override protected void init(){
        super.init();
        for(int i=0;i<3;i++){
            final int lane=i;
            laneButtons[i]=addRenderableWidget(Button.builder(Component.literal("LANE "+(i+1)),
                    button->pressMenu(lane))
                    .bounds(leftPos+184+50*i,topPos+106,47,21).build());
        }
        loadButton=addRenderableWidget(Button.builder(Component.literal("LOAD EMPTY"),
                button->pressMenu(3)).bounds(leftPos+184,topPos+204,88,22).build());
        sendButton=addRenderableWidget(Button.builder(Component.literal("SEND TO PALLET"),
                button->pressMenu(4)).bounds(leftPos+277,topPos+204,90,22).build());
    }
    private void pressMenu(int id){
        if(minecraft==null||minecraft.player==null||minecraft.gameMode==null)return;
        if(id<3){
            menu.clickMenuButton(minecraft.player,id);
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(sendButton!=null)sendButton.active=menu.outputConnected();
        if(loadButton!=null)loadButton.active=menu.emptyCount()>0;
    }
    private static void slot(GuiGraphicsExtractor g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,0xff304850);
        g.fill(x,y,x+16,y+16,0xffc3cdc7);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,
            int mouseY,float partial){
        int x=leftPos,y=topPos;
        graphics.fill(x,y,x+WIDTH,y+HEIGHT,0xff17252b);
        graphics.fill(x+3,y+3,x+WIDTH-3,y+HEIGHT-3,0xff3a555d);
        graphics.fill(x+6,y+6,x+WIDTH-6,y+HEIGHT-6,0xffd0d8d2);
        graphics.fill(x+8,y+8,x+WIDTH-8,y+30,0xff243b44);

        // Magazine remains a true 8x8 inventory, never a summary-only panel.
        graphics.fill(x+11,y+39,x+165,y+198,0xff70817d);
        for(int i=0;i<ShulkerStationMenu.STOCK;i++)
            slot(graphics,x+16+i%8*18,y+48+i/8*18);

        graphics.fill(x+177,y+39,x+371,y+199,0xff70817d);
        for(int i=0;i<3;i++){
            int col=x+207+50*i;
            if(menu.selectedLane()==i)
                graphics.fill(col-5,y+48,col+22,y+104,0xff49ab98);
            slot(graphics,col,y+54); // actual active Shulker
            slot(graphics,col,y+85); // actual completed Shulker
        }
        for(int i=0;i<27;i++)
            slot(graphics,x+184+i%9*18,y+135+i/9*18);

        graphics.fill(x+11,y+267,x+WIDTH-11,y+366,0xff718782);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            slot(graphics,x+110+col*18,y+283+row*18);
        for(int col=0;col<9;col++)slot(graphics,x+110+col*18,y+341);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,
            int mouseX,int mouseY){
        graphics.text(font,Component.literal("SHULKER STATION"),13,14,0xffb9efe0,false);
        graphics.text(font,Component.literal("EMPTY SHULKERS  "+menu.emptyCount()+"/64"),
                16,34,0xff273c40,false);
        graphics.text(font,Component.literal("ACTIVE"),181,43,0xff233d45,false);
        graphics.text(font,Component.literal("READY"),181,74,0xff233d45,false);
        graphics.text(font,Component.literal("SELECTED BOX  •  CONTENTS"),
                184,127,0xff273d42,false);
        graphics.text(font,Component.literal("Packing "+menu.packingCount()+"/3   Ready "
                +menu.readyCount()+"/3"),14,219,0xff263b41,false);
        graphics.text(font,Component.literal("Buffered items: "+menu.bufferedCount()),
                14,233,0xff263b41,false);
        graphics.text(font,Component.literal(menu.outputConnected()
                        ?"OUTPUT PTP CONNECTED":"NO OUTPUT HOPPER ADAPTER"),
                14,249,menu.outputConnected()?0xff226f49:0xffad4539,false);
        graphics.text(font,Component.literal("PLAYER INVENTORY"),110,270,
                0xff273d42,false);
    }
}
