package com.caszgamermd.caszualadditions.mail;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Blue postal terminal with browsable heads and private outgoing slots.
 * The PO Box view looks like a brass mail locker with 27 real inventory slots.
 */
public final class MailScreen extends AbstractContainerScreen<MailMenu> {
    private final boolean sending;
    private Button previous,next,send;
    public MailScreen(MailMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,176,menu.sending()?238:196);
        sending=menu.sending();
        titleLabelX=8;titleLabelY=5;
        inventoryLabelX=8;inventoryLabelY=sending?139:89;
    }
    @Override protected void init(){
        super.init();
        if(!sending)return;
        previous=addRenderableWidget(Button.builder(Component.literal("<"),
                button->press(MailMenu.PAGE_PREVIOUS))
                .bounds(leftPos+8,topPos+62,26,20).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),
                button->press(MailMenu.PAGE_NEXT))
                .bounds(leftPos+142,topPos+62,26,20).build());
        send=addRenderableWidget(Button.builder(Component.literal("SEND MAIL"),
                button->press(MailMenu.SEND))
                .bounds(leftPos+35,topPos+115,106,20).build());
    }
    private void press(int action){
        if(minecraft==null||minecraft.gameMode==null||minecraft.player==null)return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(!sending)return;
        previous.active=menu.page()>0;
        next.active=menu.page()+1<menu.pageCount();
        send.active=menu.selected()>=0;
    }
    private static void slot(GuiGraphicsExtractor graphics,int x,int y){
        graphics.fill(x-1,y-1,x+17,y+17,0xff183046);
        graphics.fill(x,y,x+16,y+16,0xffb9c8d2);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,
            int mouseX,int mouseY,float partial){
        int x=leftPos,y=topPos;
        int blue=sending?0xff144d93:0xff795e40;
        graphics.fill(x,y,x+imageWidth,y+imageHeight,0xff152536);
        graphics.fill(x+2,y+2,x+174,y+imageHeight-2,blue);
        graphics.fill(x+5,y+5,x+171,y+imageHeight-5,0xffd2d8d4);
        graphics.fill(x+6,y+6,x+170,y+18,0xff244b6e);
        if(sending){
            for(int i=0;i<18;i++)
                slot(graphics,x+8+i%9*18,y+20+i/9*18);
            for(int i=0;i<9;i++)slot(graphics,x+8+i*18,y+90);
            graphics.fill(x+7,y+145,x+169,y+231,0xff8ea4af);
            for(int i=0;i<27;i++)slot(graphics,x+8+i%9*18,y+152+i/9*18);
            for(int i=0;i<9;i++)slot(graphics,x+8+i*18,y+210);
        }else{
            graphics.fill(x+7,y+18,x+169,y+76,0xffa5a18b);
            for(int i=0;i<27;i++)slot(graphics,x+8+i%9*18,y+20+i/9*18);
            graphics.fill(x+7,y+96,x+169,y+188,0xffa5a18b);
            for(int i=0;i<27;i++)slot(graphics,x+8+i%9*18,y+102+i/9*18);
            for(int i=0;i<9;i++)slot(graphics,x+8+i*18,y+160);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(sending&&event.button()==0){
            double x=event.x()-leftPos,y=event.y()-topPos;
            if(x>=8&&x<170&&y>=20&&y<56){
                int col=(int)(x-8)/18,row=(int)(y-20)/18;
                if(col>=0&&col<9&&row>=0&&row<2){
                    press(row*9+col);
                    return true;
                }
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,
            int mouseX,int mouseY){
        graphics.text(font,Component.literal(sending?"BLUE MAILBOX":"MY PRIVATE PO BOX"),
                8,6,0xffffffff,false);
        if(sending){
            graphics.text(font,Component.literal((menu.page()+1)+"/"+menu.pageCount()),
                    72,68,0xff174270,false);
            graphics.text(font,Component.literal("To: "+menu.selectedName()),
                    8,82,0xff25455e,false);
            graphics.text(font,Component.literal("Outgoing items (all-or-nothing)"),
                    8,107,0xff214560,false);
            graphics.text(font,Component.literal("Inventory"),8,139,0xff203746,false);
        }else{
            graphics.text(font,Component.literal("27 shared personal slots"),
                    8,81,0xff263f50,false);
            graphics.text(font,Component.literal("Inventory"),8,89,0xff253b46,false);
        }
    }
}
