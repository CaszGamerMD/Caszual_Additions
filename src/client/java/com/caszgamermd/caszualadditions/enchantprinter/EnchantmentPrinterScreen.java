package com.caszgamermd.caszualadditions.enchantprinter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Flat, readable control panel with seven toggle rows, visible failure
 * probabilities, resource totals, and a remembered multi-enchantment preset. */
public final class EnchantmentPrinterScreen extends AbstractContainerScreen<EnchantmentPrinterMenu> {
    private static final int WIDTH=520,HEIGHT=418;
    private static final int BG=0xff0b141b,PANEL=0xff182730,PANEL2=0xff21353e,
        ACCENT=0xff70e5d6,WHITE=0xffe6f3f4,MUTED=0xff9aaeb8,
        GOLD=0xffedc981,WARN=0xffed947d;
    private record Entry(String id,String name,int level,int risk,int cooldown,boolean selected){}
    private final List<Entry> entries=new ArrayList<>();
    private final Button[] toggles=new Button[EnchantmentPrinterMenu.PAGE_SIZE];
    private Button prev,next,print,feed,clear;
    private int libraryCount,page,totalSelected,xpCost,lapisCost,emeraldCost;
    private String message="Feed an enchanted item or book to begin.";
    public EnchantmentPrinterScreen(EnchantmentPrinterMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,WIDTH,HEIGHT);
        titleLabelY=11;inventoryLabelX=171;inventoryLabelY=288;
    }
    private void click(int id){
        if(minecraft.gameMode!=null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
    public void updateState(String state){
        entries.clear();
        for(String row:state.split("\n")){
            String[] values=row.split("\t",-1);
            try{
                if(values.length>=5&&values[0].equals("STATE")){
                    libraryCount=Integer.parseInt(values[1]);page=Integer.parseInt(values[2]);
                    totalSelected=Integer.parseInt(values[3]);message=values[4];
                }else if(values.length>=4&&values[0].equals("COST")){
                    xpCost=Integer.parseInt(values[1]);
                    lapisCost=Integer.parseInt(values[2]);emeraldCost=Integer.parseInt(values[3]);
                }else if(values.length>=7&&values[0].equals("ROW")){
                    entries.add(new Entry(values[1],values[2],Integer.parseInt(values[3]),
                        Integer.parseInt(values[4]),Integer.parseInt(values[5]),
                        values[6].equals("1")));
                }
            }catch(NumberFormatException ignored){}
        }
        updateButtons();
    }
    @Override protected void init(){
        super.init();
        for(int i=0;i<toggles.length;i++){
            int row=i;
            toggles[i]=addRenderableWidget(Button.builder(Component.literal("SELECT"),b->click(100+row))
                .bounds(leftPos+436,topPos+125+i*22,65,19).build());
        }
        prev=addRenderableWidget(Button.builder(Component.literal("PREV"),b->click(0))
            .bounds(leftPos+267,topPos+285,67,18).build());
        next=addRenderableWidget(Button.builder(Component.literal("NEXT"),b->click(1))
            .bounds(leftPos+341,topPos+285,67,18).build());
        clear=addRenderableWidget(Button.builder(Component.literal("CLEAR SELECTION"),b->click(2))
            .bounds(leftPos+14,topPos+285,140,19).build());
        feed=addRenderableWidget(Button.builder(Component.literal("LEARN / REFILL"),b->click(3))
            .bounds(leftPos+254,topPos+63,120,25).build());
        print=addRenderableWidget(Button.builder(Component.literal("PRINT SELECTED"),b->click(4))
            .bounds(leftPos+382,topPos+63,123,25).build());
        updateButtons();
    }
    private void updateButtons(){
        if(print==null)return;
        print.active=totalSelected>0;
        clear.active=totalSelected>0;
        prev.active=page>0;
        next.active=page<(libraryCount-1)/EnchantmentPrinterMenu.PAGE_SIZE;
        for(int i=0;i<toggles.length;i++){
            Button b=toggles[i];
            boolean present=i<entries.size();
            b.visible=present;
            if(!present)continue;
            Entry entry=entries.get(i);
            b.setMessage(Component.literal(entry.selected()?"✓ ADDED":"ADD"));
        }
    }
    @Override protected void containerTick(){
        super.containerTick();
        updateButtons();
    }
    private static void cell(GuiGraphicsExtractor g,int x,int y){
        g.fill(x-2,y-2,x+18,y+18,0xff537a7e);
        g.fill(x,y,x+16,y+16,0xff1a2830);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+WIDTH,y+HEIGHT,BG);
        g.fill(x+6,y+7,x+WIDTH-6,y+38,PANEL2);
        g.fill(x+9,y+43,x+WIDTH-9,y+99,PANEL);
        g.fill(x+9,y+106,x+WIDTH-9,y+281,PANEL);
        g.fill(x+9,y+310,x+WIDTH-9,y+WIDTH/5+309,PANEL);
        for(int i=0;i<4;i++)cell(g,x+31+i*55,y+70);
        for(int i=0;i<EnchantmentPrinterMenu.PAGE_SIZE;i++){
            int rowY=y+123+i*22;
            g.fill(x+14,rowY,x+506,rowY+21,i%2==0?0xff22343b:0xff1b2d34);
        }
        for(int i=0;i<27;i++)cell(g,x+169+i%9*18,y+302+i/9*18);
        for(int i=0;i<9;i++)cell(g,x+169+i*18,y+360);
        g.fill(x+9,y+388,x+WIDTH-9,y+413,PANEL);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);
        int x=leftPos,y=topPos;
        g.text(font,"ENCHANTMENT PRINTER",x+17,y+17,ACCENT,false);
        g.text(font,"LIBRARY "+libraryCount+"  |  PRESET "+totalSelected+" SELECTED",
            x+270,y+18,WHITE,false);
        String[] labels={"FEED","TARGET","LAPIS","EMERALDS"};
        for(int i=0;i<labels.length;i++)
            g.text(font,labels[i],x+24+i*55,y+49,MUTED,false);
        g.text(font,"APPLY MULTIPLE ENCHANTMENTS IN ONE PRINT",x+18,y+102,MUTED,false);
        for(int i=0;i<entries.size()&&i<EnchantmentPrinterMenu.PAGE_SIZE;i++){
            Entry e=entries.get(i),unused=e;
            int rowY=y+129+i*22;
            String name=e.name().length()>32?e.name().substring(0,31)+"…":e.name();
            g.text(font,name+" "+e.level(),x+23,rowY,WHITE,false);
            if(e.cooldown()>0)
                g.text(font,"OFFLINE "+e.cooldown()+"s",x+293,rowY,WARN,false);
            else g.text(font,"RUN-OUT "+e.risk()+"%",x+292,rowY,
                    e.risk()>=50?WARN:GOLD,false);
        }
        g.text(font,"PAGE "+(page+1)+"/"+Math.max(1,(libraryCount+6)/7),
            x+414,y+290,MUTED,false);
        g.text(font,"INVENTORY  |  XP LEVELS "+xpCost+"  •  LAPIS "+lapisCost+
            "  •  EMERALDS "+emeraldCost,x+172,y+296,ACCENT,false);
        g.text(font,message.length()>77?message.substring(0,74)+"...":message,
            x+17,y+398,MUTED,false);
    }
}
