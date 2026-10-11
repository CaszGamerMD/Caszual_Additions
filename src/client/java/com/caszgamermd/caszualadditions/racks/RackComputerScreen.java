package com.caszgamermd.caszualadditions.racks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Full-window warehouse terminal. The server, not the client, decides which
 * racks are connected or whose inventory may be inspected.
 */
public final class RackComputerScreen extends Screen {
    private final BlockPos computer;
    private EditBox search, orderQuantity;
    private String selectedItemId = "";
    private JsonArray racks=new JsonArray(), contents=new JsonArray();
    private int selectedX=Integer.MIN_VALUE, selectedY,selectedZ;
    private int totalRacks, totalItems, scroll;
    private String status="Connecting to racks...";
    private String transferPointStatus="";
    private Button golemButton;
    private int shelfCount,dockedGolems;
    private boolean golemSorting;
    private int savedQueryLength;

    public RackComputerScreen(BlockPos computer) {
        super(Component.literal("Rack Computer"));
        this.computer=computer;
    }

    @Override protected void init() {
        String typed=search==null?"":search.getValue();
        int left=22, y=45;
        search=addRenderableWidget(new EditBox(font,left,y,
                Math.max(90,width-270),23,Component.literal("Search rack network")));
        search.setMaxLength(60);
        search.setHint(Component.literal("Item name, item ID, rack, or aisle"));
        search.setValue(typed);
        addRenderableWidget(Button.builder(Component.literal("Search"),
                b->sendQuery()).bounds(width-225,y,90,23).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"),
                b->sendQuery()).bounds(width-128,y,90,23).build());
        golemButton=addRenderableWidget(Button.builder(
                Component.literal("GOLEMS: AUTO"),btn->{
                    ClientPlayNetworking.send(new RackComputerNet.GolemControl(
                            computer,!golemSorting));
                }).bounds(22,height-63,142,22).build());
        // Put the order controls in their own full-width footer so they
        // remain clickable at high Minecraft GUI Scale settings.
        int orderInputX=Math.max(22,width-230);
        orderQuantity=addRenderableWidget(new EditBox(font,orderInputX,height-63,
                56,22,Component.literal("Order quantity")));
        orderQuantity.setMaxLength(4);
        orderQuantity.setValue("64");
        int orderButtonX=orderInputX+64;
        addRenderableWidget(Button.builder(Component.literal("REQUEST ITEMS"),
                b->requestItem()).bounds(orderButtonX,height-63,
                        Math.max(60,width-orderButtonX-20),22).build());
        addRenderableWidget(Button.builder(Component.literal("Close"),
                b->onClose()).bounds(width-99,height-34,80,22).build());
        addRenderableWidget(Button.builder(Component.literal("▲"),
                b->{scroll=Math.max(0,scroll-1);})
                .bounds(22,height-34,26,22).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),
                b->{scroll=Math.min(Math.max(0,racks.size()-1),scroll+1);})
                .bounds(51,height-34,26,22).build());
        sendQuery();
    }

    private void sendQuery() {
        if(search==null)return;
        savedQueryLength=search.getValue().length();
        ClientPlayNetworking.send(new RackComputerNet.Query(computer,
                search.getValue().strip(),
                selectedX==Integer.MIN_VALUE?BlockPos.ZERO
                        :new BlockPos(selectedX,selectedY,selectedZ)));
    }

    private void requestItem() {
        if(selectedItemId.isBlank()){status="Click an item in the inspected rack first.";return;}
        int count;
        try{count=Integer.parseInt(orderQuantity.getValue().strip());}
        catch(NumberFormatException e){status="Enter a quantity from 1 to 4096.";return;}
        if(count<1||count>4096){status="Order quantity must be 1–4096.";return;}
        ClientPlayNetworking.send(new RackComputerNet.OrderRequest(computer,selectedItemId,count));
    }

    public void acceptOrderReply(String message){status=message;}

    public void accept(BlockPos pos,String raw) {
        if(!computer.equals(pos))return;
        try {
            JsonObject obj=JsonParser.parseString(raw).getAsJsonObject();
            racks=obj.getAsJsonArray("racks");
            contents=obj.has("contents")?obj.getAsJsonArray("contents"):new JsonArray();
            totalRacks=obj.get("networkSize").getAsInt();
            totalItems=obj.get("totalItems").getAsInt();
            shelfCount=obj.has("shelfCount")?obj.get("shelfCount").getAsInt():0;
            dockedGolems=obj.has("dockedGolems")?obj.get("dockedGolems").getAsInt():0;
            golemSorting=obj.has("golemSortingEnabled")
                    &&obj.get("golemSortingEnabled").getAsBoolean();
            if(golemButton!=null){
                golemButton.active=shelfCount>0;
                golemButton.setMessage(Component.literal("GOLEMS: "
                        +(golemSorting?"AUTO":"PAUSED")+" ("+dockedGolems+")"));
            }
            transferPointStatus=obj.has("ptpSummary")
                    ?obj.get("ptpSummary").getAsString():"";
            status=(racks.size())+" matching racks in "+totalRacks+" connected racks";
            scroll=Math.min(scroll,Math.max(0,racks.size()-1));
        }catch(RuntimeException e){status="Unable to read network information.";}
    }

    private static String compact(String value,int limit) {
        if(value.length()<=limit)return value;
        return value.substring(0,Math.max(0,limit-1))+"…";
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics,
            int mouseX,int mouseY,float partialTick) {
        super.extractBackground(graphics,mouseX,mouseY,partialTick);
        graphics.fill(0,0,width,height,0xf00c1518);
        graphics.fill(8,8,width-8,height-8,0xff162a25);
        graphics.fill(10,10,width-10,35,0xff1c5839);
        graphics.text(font,Component.literal("RACK COMPUTER  •  WAREHOUSE NETWORK"),
                22,19,0xffc5ffd0,false);
        graphics.text(font,Component.literal(status),22,77,0xff9fedaf,false);
        graphics.text(font,Component.literal("Total stored: "+totalItems+" items"),
                width-197,77,0xffa4d5b7,false);
        if(!transferPointStatus.isBlank())
            graphics.text(font,Component.literal(compact(transferPointStatus,
                    Math.max(16,(width-42)/6))),22,88,0xffa4d5b7,false);
        int splitter=Math.max(200,width/2);
        graphics.fill(20,97,splitter-5,height-45,0xff21392e);
        graphics.fill(splitter,97,width-20,height-45,0xff22382f);
        graphics.text(font,Component.literal("AISLES / RACKS"),29,104,0xffb3f9c1,false);
        graphics.text(font,Component.literal("SELECTED RACK • CONTENTS"),splitter+11,104,0xffb3f9c1,false);
        int rows=Math.max(0,(height-173)/25);
        for(int i=scroll;i<racks.size()&&i<scroll+rows;i++){
            JsonObject rack=racks.get(i).getAsJsonObject();
            int y=120+(i-scroll)*25;
            int x=rack.get("x").getAsInt(),z=rack.get("z").getAsInt(),ry=rack.get("y").getAsInt();
            boolean chosen=x==selectedX&&z==selectedZ&&ry==selectedY;
            graphics.fill(25,y,splitter-11,y+23,
                    chosen?0xff3b7d54:(i%2==0?0xff2c493b:0xff294336));
            String name=compact(rack.get("name").getAsString(),
                    Math.max(8,(splitter-140)/6));
            graphics.text(font,Component.literal(name),32,y+4,
                    chosen?0xffefffed:0xffcee4d1,false);
            String used=rack.get("slots").getAsInt()+"/216";
            graphics.text(font,Component.literal(used),splitter-68,y+4,0xffa4e5b1,false);
            float pct=rack.get("slots").getAsFloat()/216.0f;
            graphics.fill(32,y+18,splitter-68,y+21,0xff4c6655);
            graphics.fill(32,y+18,32+(int)((splitter-100)*pct),y+21,0xff64d98a);
        }
        int remaining=Math.max(0,(height-220)/17);
        for(int i=0;i<contents.size()&&i<remaining;i++){
            JsonObject item=contents.get(i).getAsJsonObject();
            int y=123+i*17;
            if(item.get("id").getAsString().equals(selectedItemId))
                graphics.fill(splitter+7,y-2,width-27,y+14,0xff43835a);
            else if(i%2==0)graphics.fill(splitter+7,y-2,width-27,y+14,0xff2b4538);
            graphics.text(font,Component.literal(compact(item.get("name").getAsString(),
                    Math.max(9,(width-splitter-95)/6))),
                    splitter+13,y,0xffd8f5de,false);
            String count="x"+item.get("count").getAsInt();
            graphics.text(font,Component.literal(count),width-74,y,0xffa8e4c5,false);
        }
        graphics.text(font,Component.literal("Click rack to inspect  •  Search flashes matching racks for 10 seconds"),
                99,height-27,0xff91c3a1,false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        int split=Math.max(200,width/2);
        int mx=(int)event.x(),my=(int)event.y();
        if(mx>=split+7&&mx<width-27 && my>=121 && my<height-95){
            int index=(my-121)/17;
            if(index>=0&&index<contents.size()){
                selectedItemId=contents.get(index).getAsJsonObject().get("id").getAsString();
                status="Selected "+selectedItemId+". Enter quantity and request.";
                return true;
            }
        }
        int rows=Math.max(0,(height-173)/25);
        if(mx>=25&&mx<split-11&&my>=120&&my<120+rows*25) {
            int index=scroll+(my-120)/25;
            if(index<racks.size()){
                JsonObject row=racks.get(index).getAsJsonObject();
                selectedX=row.get("x").getAsInt();
                selectedY=row.get("y").getAsInt();
                selectedZ=row.get("z").getAsInt();
                selectedItemId="";
                sendQuery();
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean isPauseScreen(){return false;}
}
