package com.caszgamermd.caszualadditions.painting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class IndustrialPaintScreen extends Screen {
    private final BlockPos can;
    private final int startingColor;
    private EditBox hex;
    private String error="";
    public IndustrialPaintScreen(BlockPos can,int color){
        super(Component.literal("Industrial Paint Mixer"));
        this.can=can;
        this.startingColor=color&0xffffff;
    }
    private int color(){
        String text=hex.getValue().trim();
        if(text.startsWith("#"))text=text.substring(1);
        if(!text.matches("[0-9a-fA-F]{6}"))return -1;
        return Integer.parseInt(text,16);
    }
    private void submit(int action){
        int rgb=color();
        if(rgb<0){error="Enter a 6-digit hex color, e.g. #39C5EE";return;}
        ClientPlayNetworking.send(new IndustrialPaintNet.Request(can,rgb,action));
        error="";
        if(action==1)onClose();
    }
    @Override protected void init(){
        int left=width/2-108, top=height/2-80;
        hex=new EditBox(font,left,top+37,216,22,
                Component.literal("Paint Color (RRGGBB)"));
        hex.setMaxLength(7);
        hex.setValue("#"+String.format("%06X",startingColor));
        addRenderableWidget(hex);
        addRenderableWidget(Button.builder(Component.literal("Save Hex Color"),
                btn->submit(0)).bounds(left,top+68,104,22).build());
        addRenderableWidget(Button.builder(Component.literal("Take Paint Brush"),
                btn->submit(1)).bounds(left+110,top+68,106,22).build());
        addRenderableWidget(Button.builder(Component.literal("Close"),
                btn->onClose()).bounds(left,top+98,216,22).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);
        int left=width/2-125,top=height/2-100;
        g.fill(left,top,left+250,top+175,0xee121e20);
        g.fill(left+4,top+4,left+246,top+28,0xff536d72);
        g.text(font,Component.literal("INDUSTRIAL PAINT MIXER"),
                left+17,top+11,0xffffffff,false);
        int rgb=hex==null?-1:color();
        g.fill(left+20,top+137,left+230,top+151,
                rgb<0?0xff555555:0xff000000|rgb);
        g.text(font,Component.literal(error.isBlank()
                        ?"Pick any RGB hex color; brush is reusable.":error),
                left+13,top+160,error.isBlank()?0xffb0c8be:0xffff6a6a,false);
    }
    @Override public boolean isPauseScreen(){return false;}
}