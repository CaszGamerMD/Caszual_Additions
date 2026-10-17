package com.caszgamermd.caszualadditions.racks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
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
    private final java.util.List<AbstractWidget> rackWidgets=new java.util.ArrayList<>();
    private final java.util.List<AbstractWidget> workerWidgets=new java.util.ArrayList<>();
    private final java.util.List<AbstractWidget> orderWidgets=new java.util.ArrayList<>();
    // A draft belongs to a physical Rack Computer in this loaded world,
    // NOT one temporary Screen instance. The weak world key ensures an
    // unrelated server/world can't see the previous session's shopping list.
    private static final java.util.WeakHashMap<Object,
            java.util.Map<BlockPos,java.util.LinkedHashMap<String,Integer>>>
            SAVED_DRAFTS=new java.util.WeakHashMap<>();
    private final java.util.LinkedHashMap<String,Integer> orderList=new java.util.LinkedHashMap<>();
    private Object draftWorld;
    private void saveDraft(){
        if(draftWorld==null)return;
        synchronized(SAVED_DRAFTS){
            SAVED_DRAFTS.computeIfAbsent(draftWorld,
                    ignored->new java.util.HashMap<>())
                    .put(computer.immutable(),new java.util.LinkedHashMap<>(orderList));
        }
    }
    private void loadDraft(){
        Object world=Minecraft.getInstance().level;
        if(world==null||world==draftWorld)return;
        draftWorld=world;
        synchronized(SAVED_DRAFTS){
            orderList.clear();
            var stored=SAVED_DRAFTS.get(world);
            if(stored!=null&&stored.containsKey(computer))
                orderList.putAll(stored.get(computer));
        }
        if(!orderList.isEmpty())
            orderStatus="Draft restored: "+orderList.size()
                    +" items. Review before submitting.";
    }
    private int orderScroll;
    private String orderStatus="Select stock on RACKS, add items, then submit the list.";
    private JsonArray workers=new JsonArray();
    private int page,workerScroll,activeGolems,igcCount,refreshTicks;
    private String workerStatus="Choose a work request or inspect active tasks.";
    private Button workersToggleButton;
    private <T extends AbstractWidget> T addRack(T widget){
        rackWidgets.add(widget);
        return addRenderableWidget(widget);
    }
    private <T extends AbstractWidget> T addWorkers(T widget){
        workerWidgets.add(widget);
        return addRenderableWidget(widget);
    }
    private <T extends AbstractWidget> T addOrders(T widget){
        orderWidgets.add(widget);
        return addRenderableWidget(widget);
    }
    private void showPage(int target){
        page=target;
        for(AbstractWidget widget:rackWidgets)widget.visible=page==0;
        for(AbstractWidget widget:workerWidgets)widget.visible=page==1;
        for(AbstractWidget widget:orderWidgets)widget.visible=page==2;
        sendQuery();
    }
    private String selectedItemId = "";
    private JsonArray racks=new JsonArray(), contents=new JsonArray(),
            ptps=new JsonArray(), stations=new JsonArray();
    private int selectedPtpIndex;
    private Button ptpNextButton,ptpModeButton,ptpWrapButton,highlightButton;
    private int selectedX=Integer.MIN_VALUE, selectedY,selectedZ;
    private int totalRacks, totalItems, scroll;
    private String status="Connecting to racks...";
    private String transferPointStatus="";
    private String scannerStatus="";
    private Button golemButton;
    private int shelfCount,dockedGolems;
    private boolean golemSorting;
    private int savedQueryLength;

    public RackComputerScreen(BlockPos computer) {
        super(Component.literal("Rack Computer"));
        this.computer=computer;
    }

    @Override protected void init() {
        loadDraft();
        String typed=search==null?"":search.getValue();
        rackWidgets.clear();
        workerWidgets.clear();
        orderWidgets.clear();
        int tabLeft=Math.max(210,width-353);
        addRenderableWidget(Button.builder(Component.literal("RACKS & PTPs"),
                b->showPage(0)).bounds(tabLeft,13,107,20).build());
        addRenderableWidget(Button.builder(Component.literal("STORAGE GOLEMS"),
                b->showPage(1)).bounds(tabLeft+110,13,119,20).build());
        addRenderableWidget(Button.builder(Component.literal("ORDER LIST"),
                b->showPage(2)).bounds(tabLeft+232,13,114,20).build());
        int left=22, y=45;
        search=addRack(new EditBox(font,left,y,
                Math.max(90,width-270),23,Component.literal("Search rack network")));
        search.setMaxLength(60);
        search.setHint(Component.literal("Item name, item ID, rack, or aisle"));
        search.setValue(typed);
        addRack(Button.builder(Component.literal("Search"),
                b->sendQuery()).bounds(width-225,y,90,23).build());
        addRack(Button.builder(Component.literal("Refresh"),
                b->sendQuery()).bounds(width-128,y,90,23).build());
        ptpNextButton=addRack(Button.builder(Component.literal("NEXT PTP"),
                b->{if(!ptps.isEmpty()){selectedPtpIndex=(selectedPtpIndex+1)%ptps.size();
                    updatePtpControls();}})
                .bounds(22,130,92,22).build());
        ptpModeButton=addRack(Button.builder(Component.literal("MODE"),
                b->sendPtpControl(0)).bounds(120,130,90,22).build());
        ptpWrapButton=addRack(Button.builder(Component.literal("WRAP"),
                b->sendPtpControl(1)).bounds(216,130,100,22).build());
        updatePtpControls();
        int orderInputX=Math.max(22,width-230);
        golemButton=addRack(Button.builder(
                Component.literal("GOLEMS: AUTO"),btn->{
                    ClientPlayNetworking.send(new RackComputerNet.GolemControl(
                            computer,!golemSorting));
                }).bounds(22,height-63,Math.max(40,orderInputX-30),22).build());
        // Put the order controls in their own full-width footer so they
        // remain clickable at high Minecraft GUI Scale settings.
        orderQuantity=addRack(new EditBox(font,orderInputX,height-63,
                56,22,Component.literal("Order quantity")));
        orderQuantity.setMaxLength(4);
        orderQuantity.setValue("64");
        int orderButtonX=orderInputX+64;
        addRack(Button.builder(Component.literal("ADD TO LIST"),
                b->addToOrderList()).bounds(orderButtonX,height-63,
                        Math.max(60,width-orderButtonX-20),22).build());
        addRack(Button.builder(Component.literal("Close"),
                b->onClose()).bounds(width-99,height-34,80,22).build());
        highlightButton=addRack(Button.builder(Component.literal("HIGHLIGHT • 10s"),
                button->highlightSelected()).bounds(90,height-34,155,22).build());
        highlightButton.active=false;
        addRack(Button.builder(Component.literal("▲"),
                b->{scroll=Math.max(0,scroll-1);})
                .bounds(22,height-34,26,22).build());
        addRack(Button.builder(Component.literal("▼"),
                b->{scroll=Math.min(Math.max(0,racks.size()-1),scroll+1);})
                .bounds(51,height-34,26,22).build());
        int column=Math.max(80,(width-54)/4);
        workersToggleButton=addWorkers(Button.builder(Component.literal("SORT: AUTO"),
                b->ClientPlayNetworking.send(new RackComputerNet.GolemControl(
                        computer,!golemSorting)))
                .bounds(22,72,column,23).build());
        addWorkers(Button.builder(Component.literal("RUN QUEUE"),
                b->requestGolem(2)).bounds(26+column,72,column,23).build());
        addWorkers(Button.builder(Component.literal("ENLIST 4"),
                b->requestGolem(0)).bounds(30+2*column,72,column,23).build());
        addWorkers(Button.builder(Component.literal("RECALL ALL"),
                b->requestGolem(1)).bounds(34+3*column,72,column,23).build());
        addWorkers(Button.builder(Component.literal("REFRESH TASKS"),
                b->sendQuery()).bounds(22,103,122,22).build());
        addWorkers(Button.builder(Component.literal("▲"),
                b->workerScroll=Math.max(0,workerScroll-1))
                .bounds(width-80,height-34,25,22).build());
        addWorkers(Button.builder(Component.literal("▼"),
                b->workerScroll=Math.min(Math.max(0,workers.size()-1),workerScroll+1))
                .bounds(width-51,height-34,25,22).build());
        addOrders(Button.builder(Component.literal("SUBMIT ORDER LIST"),
                b->submitOrderList()).bounds(22,height-69,Math.max(122,width/3),23).build());
        addOrders(Button.builder(Component.literal("CLEAR LIST"),
                b->{orderList.clear();saveDraft();orderStatus="Order list cleared.";})
                .bounds(27+Math.max(122,width/3),height-69,104,23).build());
        addOrders(Button.builder(Component.literal("REMOVE LAST"),
                b->removeLastOrder()).bounds(22,height-41,108,21).build());
        addOrders(Button.builder(Component.literal("▲"),
                b->orderScroll=Math.max(0,orderScroll-1))
                .bounds(width-81,height-41,25,21).build());
        addOrders(Button.builder(Component.literal("▼"),
                b->orderScroll=Math.min(Math.max(0,orderList.size()-1),orderScroll+1))
                .bounds(width-51,height-41,25,21).build());
        showPage(page);
    }

    private void sendQuery() {
        if(search==null)return;
        savedQueryLength=search.getValue().length();
        ClientPlayNetworking.send(new RackComputerNet.Query(computer,
                search.getValue().strip(),
                selectedX==Integer.MIN_VALUE?BlockPos.ZERO
                        :new BlockPos(selectedX,selectedY,selectedZ)));
    }

    private void highlightSelected(){
        if(selectedX==Integer.MIN_VALUE)return;
        ClientPlayNetworking.send(new RackComputerNet.HighlightRequest(
                computer,new BlockPos(selectedX,selectedY,selectedZ)));
        status="Requested 10-second glow for the selected pallet (all players).";
    }

    private void requestGolem(int action){
        ClientPlayNetworking.send(new RackComputerNet.GolemWork(computer,action));
        workerStatus="Work request sent. Awaiting server confirmation...";
    }

    private void sendPtpControl(int action){
        if(ptps.isEmpty()||selectedPtpIndex>=ptps.size())return;
        JsonObject ptp=ptps.get(selectedPtpIndex).getAsJsonObject();
        ClientPlayNetworking.send(new RackComputerNet.PtpControl(computer,
                new BlockPos(ptp.get("x").getAsInt(),
                        ptp.get("y").getAsInt(),ptp.get("z").getAsInt()),action));
        sendQuery(); // preserve the current search and selected rack on refresh
    }
    private void updatePtpControls(){
        if(ptpNextButton==null||ptpModeButton==null||ptpWrapButton==null)return;
        boolean present=!ptps.isEmpty();
        ptpNextButton.active=present&&ptps.size()>1;
        if(!present){
            ptpModeButton.active=false;ptpWrapButton.active=false;
            ptpModeButton.setMessage(Component.literal("NO PTP"));
            ptpWrapButton.setMessage(Component.literal("NO WRAP"));
            return;
        }
        selectedPtpIndex=Math.clamp(selectedPtpIndex,0,ptps.size()-1);
        JsonObject point=ptps.get(selectedPtpIndex).getAsJsonObject();
        boolean busy=point.get("hauling").getAsBoolean()
                ||point.get("orders").getAsInt()>0;
        ptpModeButton.active=!busy;
        ptpModeButton.setMessage(Component.literal(point.get("input").getAsBoolean()
                ?"SET OUTPUT":"SET INPUT"));
        ptpWrapButton.active=point.get("wrap").getAsBoolean()
                &&point.get("pallet").getAsBoolean()&&!busy;
        ptpWrapButton.setMessage(Component.literal(point.get("sealed").getAsBoolean()
                ?"UNWRAP":"WRAP"));
    }

    private void addToOrderList() {
        if(selectedItemId.isBlank()){status="Click an item in the inspected rack first.";return;}
        int count;
        try{count=Integer.parseInt(orderQuantity.getValue().strip());}
        catch(NumberFormatException e){status="Enter a quantity from 1 to 4096.";return;}
        if(count<1||count>4096){status="Order quantity must be 1–4096.";return;}
        int total=orderList.getOrDefault(selectedItemId,0)+count;
        if(total>4096){status="At most 4096 per item.";return;}
        if(!orderList.containsKey(selectedItemId)&&orderList.size()>=16){
            status="Maximum 16 different items per order.";return;
        }
        orderList.put(selectedItemId,total);
        saveDraft();
        status="Added to list: "+selectedItemId+" x"+total
                +". Open ORDER LIST to review and submit.";
        orderStatus=status;
    }
    private void removeLastOrder(){
        if(orderList.isEmpty())return;
        String last="";
        for(String id:orderList.keySet())last=id;
        orderList.remove(last);
        saveDraft();
        orderStatus="Removed "+last;
    }
    private void submitOrderList(){
        if(orderList.isEmpty()){orderStatus="Add at least one item.";return;}
        StringBuilder rows=new StringBuilder();
        orderList.forEach((id,count)->{
            if(!rows.isEmpty())rows.append('\n');
            rows.append(id).append('|').append(count);
        });
        ClientPlayNetworking.send(new RackComputerNet.OrderListRequest(
                computer,rows.toString()));
        orderStatus="Submitted list; waiting for server confirmation...";
    }

    public void acceptOrderReply(String message){
        status=message;workerStatus=message;orderStatus=message;
        if(message.startsWith("Order submitted:")){
            orderList.clear();
            saveDraft();
        }
    }

    public void accept(BlockPos pos,String raw) {
        if(!computer.equals(pos))return;
        try {
            JsonObject obj=JsonParser.parseString(raw).getAsJsonObject();
            racks=obj.getAsJsonArray("racks");
            ptps=obj.has("ptps")?obj.getAsJsonArray("ptps"):new JsonArray();
            stations=obj.has("stations")?obj.getAsJsonArray("stations"):new JsonArray();
            workers=obj.has("workers")?obj.getAsJsonArray("workers"):new JsonArray();
            activeGolems=obj.has("activeGolems")?obj.get("activeGolems").getAsInt():0;
            igcCount=obj.has("industrialConcrete")
                    ?obj.get("industrialConcrete").getAsInt():0;
            updatePtpControls();
            contents=obj.has("contents")?obj.getAsJsonArray("contents"):new JsonArray();
            if(highlightButton!=null){
                boolean loaded=false;
                for(var row:racks){
                    JsonObject rack=row.getAsJsonObject();
                    if(rack.get("x").getAsInt()==selectedX
                            &&rack.get("y").getAsInt()==selectedY
                            &&rack.get("z").getAsInt()==selectedZ
                            &&rack.get("pallet").getAsBoolean()){loaded=true;break;}
                }
                highlightButton.active=loaded;
            }
            totalRacks=obj.get("networkSize").getAsInt();
            totalItems=obj.get("totalItems").getAsInt();
            shelfCount=obj.has("shelfCount")?obj.get("shelfCount").getAsInt():0;
            dockedGolems=obj.has("dockedGolems")?obj.get("dockedGolems").getAsInt():0;
            golemSorting=obj.has("golemSortingEnabled")
                    &&obj.get("golemSortingEnabled").getAsBoolean();
            if(workersToggleButton!=null){
                workersToggleButton.active=shelfCount>0;
                workersToggleButton.setMessage(Component.literal(golemSorting
                        ?"SORT: AUTO":"SORT: PAUSED"));
            }
            if(golemButton!=null){
                golemButton.active=shelfCount>0;
                golemButton.setMessage(Component.literal("GOLEMS: "
                        +(golemSorting?"AUTO":"PAUSED")+" ("+dockedGolems+")"));
            }
            transferPointStatus=obj.has("ptpSummary")
                    ?obj.get("ptpSummary").getAsString():"";
            scannerStatus=obj.has("scannerSummary")
                    ?obj.get("scannerSummary").getAsString():"";
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
        if(page==1){
            drawWorkers(graphics);
            return;
        }
        if(page==2){
            drawOrderList(graphics);
            return;
        }
        graphics.text(font,Component.literal(compact(status,
                Math.max(8,(width-250)/6))),22,77,0xff9fedaf,false);
        graphics.text(font,Component.literal("Total stored: "+totalItems+" items"),
                width-197,77,0xffa4d5b7,false);
        if(!transferPointStatus.isBlank())
            graphics.text(font,Component.literal(compact(transferPointStatus,
                    Math.max(16,(width-42)/6))),22,88,0xffa4d5b7,false);
        if(!scannerStatus.isBlank())
            graphics.text(font,Component.literal(compact(scannerStatus,
                    Math.max(16,(width-42)/6))),22,99,0xff83eadd,false);
        if(!ptps.isEmpty()){
            JsonObject ptp=ptps.get(selectedPtpIndex).getAsJsonObject();
            String summary="PTP "+(selectedPtpIndex+1)+"/"+ptps.size()+" @ "
                    +ptp.get("x").getAsInt()+","+ptp.get("y").getAsInt()
                    +","+ptp.get("z").getAsInt()
                    +(ptp.get("hopper").getAsBoolean()?" +HOPPER":"")
                    +(ptp.get("hauling").getAsBoolean()?" • HAULING":"");
            graphics.text(font,Component.literal(compact(summary,Math.max(16,(width-42)/6))),
                    22,116,0xff83eadd,false);
        }else graphics.text(font,Component.literal("No attached PTP detected"),
                    22,116,0xffa4d5b7,false);
        int splitter=Math.max(200,width/2);
        graphics.fill(20,160,splitter-5,height-45,0xff21392e);
        graphics.fill(splitter,160,width-20,height-45,0xff22382f);
        graphics.text(font,Component.literal("AISLES / RACKS"),29,167,0xffb3f9c1,false);
        graphics.text(font,Component.literal("SELECTED RACK • CONTENTS"),splitter+11,167,0xffb3f9c1,false);
        int rows=Math.max(0,(height-253)/25);
        for(int i=scroll;i<racks.size()&&i<scroll+rows;i++){
            JsonObject rack=racks.get(i).getAsJsonObject();
            int y=183+(i-scroll)*25;
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
        int remaining=Math.max(0,(height-283)/17);
        for(int i=0;i<contents.size()&&i<remaining;i++){
            JsonObject item=contents.get(i).getAsJsonObject();
            int y=186+i*17;
            if(item.get("id").getAsString().equals(selectedItemId))
                graphics.fill(splitter+7,y-2,width-27,y+14,0xff43835a);
            else if(i%2==0)graphics.fill(splitter+7,y-2,width-27,y+14,0xff2b4538);
            graphics.text(font,Component.literal(compact(item.get("name").getAsString(),
                    Math.max(9,(width-splitter-95)/6))),
                    splitter+13,y,0xffd8f5de,false);
            String count="x"+item.get("count").getAsInt();
            graphics.text(font,Component.literal(count),width-74,y,0xffa8e4c5,false);
        }

    }

    private void drawOrderList(GuiGraphicsExtractor graphics){
        graphics.text(font,Component.literal("ORDER BUILDER  •  "+orderList.size()
                +" / 16 item types"),22,50,0xffb3f9c1,false);
        graphics.text(font,Component.literal(compact(orderStatus,
                Math.max(20,(width-45)/6))),22,75,0xff9fedaf,false);
        graphics.text(font,Component.literal(
                "Select stock in RACKS, ADD TO LIST. Click a row here to remove it."),
                22,99,0xffc5dbce,false);
        int connected=0,empty=0,packing=0,ready=0;
        for(var entry:stations){
            JsonObject row=entry.getAsJsonObject();
            if(row.get("connected").getAsBoolean())connected++;
            empty+=row.get("empty").getAsInt();
            packing+=row.get("packing").getAsInt();
            ready+=row.get("ready").getAsInt();
        }
        graphics.text(font,Component.literal("SHULKER STATIONS: "+connected
                +"/"+stations.size()+" attached | "+empty+" empty | "
                +packing+" packing | "+ready+" ready"),22,114,0xff83eadd,false);
        graphics.fill(20,125,width-20,height-83,0xff21392e);
        graphics.text(font,Component.literal("ITEM"),30,133,0xffb3f9c1,false);
        graphics.text(font,Component.literal("QUANTITY"),width-114,133,
                0xffb3f9c1,false);
        int rows=Math.max(0,(height-251)/25);
        int position=0;
        for(var entry:orderList.entrySet()){
            if(position>=orderScroll&&position<orderScroll+rows){
                int y=150+(position-orderScroll)*25;
                graphics.fill(26,y,width-26,y+23,
                        position%2==0?0xff2e4a3b:0xff294236);
                graphics.text(font,Component.literal(compact(entry.getKey(),
                        Math.max(12,(width-180)/6))),34,y+6,
                        0xffd8f5de,false);
                graphics.text(font,Component.literal("x"+entry.getValue()),
                        width-105,y+6,0xffa8e4c5,false);
            }
            position++;
        }
    }

    private void drawWorkers(GuiGraphicsExtractor graphics){
        graphics.text(font,Component.literal("Network: "+shelfCount+" shelves / "
                +dockedGolems+" docked / "+activeGolems+" active / "
                +igcCount+" IGC blocks"),22,51,0xffaee9c2,false);
        graphics.text(font,Component.literal(compact(workerStatus,
                Math.max(18,(width-45)/6))),22,135,0xff9fedaf,false);
        graphics.fill(20,151,width-20,height-45,0xff21392e);
        graphics.text(font,Component.literal("DEPLOYED WORKERS • LIVE TASK TRACKER"),
                29,159,0xffb3f9c1,false);
        int rows=Math.max(0,(height-258)/31);
        if(workers.isEmpty())
            graphics.text(font,Component.literal(
                    "No active workers loaded. Docked Golems remain on shelves."),
                    32,185,0xffb2d4bf,false);
        for(int i=workerScroll;i<workers.size()&&i<workerScroll+rows;i++){
            JsonObject worker=workers.get(i).getAsJsonObject();
            int y=181+(i-workerScroll)*31;
            graphics.fill(27,y,width-28,y+29,i%2==0?0xff2e4a3b:0xff294236);
            String coordinates=worker.get("x").getAsInt()+","
                    +worker.get("y").getAsInt()+","+worker.get("z").getAsInt();
            String heading=(i+1)+". "+worker.get("work").getAsString();
            graphics.text(font,Component.literal(compact(heading,
                    Math.max(14,(width-58)/6))),32,y+3,0xffd2f5dc,false);
            String owner=worker.get("owner").getAsString();
            String line="Cargo: "+worker.get("cargo").getAsString()+" | "+coordinates
                    +(owner.isBlank()?"":" | Player "+owner.substring(0,Math.min(8,owner.length())));
            graphics.text(font,Component.literal(compact(line,
                    Math.max(14,(width-58)/6))),32,y+16,0xff99cbb2,false);
        }

    }

    @Override public void tick(){
        super.tick();
        if(page==1&&++refreshTicks%60==0)sendQuery();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(page==2){
            int mx=(int)event.x(),my=(int)event.y();
            int rows=Math.max(0,(height-251)/25);
            if(mx>=26&&mx<width-26&&my>=150&&my<150+rows*25){
                int index=orderScroll+(my-150)/25;
                if(index>=0&&index<orderList.size()){
                    String selected=new java.util.ArrayList<>(orderList.keySet()).get(index);
                    orderList.remove(selected);
                    saveDraft();
                    orderStatus="Removed "+selected+" from order.";
                    orderScroll=Math.min(orderScroll,Math.max(0,orderList.size()-1));
                    return true;
                }
            }
            return super.mouseClicked(event,doubleClick);
        }
        if(page!=0)return super.mouseClicked(event,doubleClick);
        int split=Math.max(200,width/2);
        int mx=(int)event.x(),my=(int)event.y();
        if(mx>=split+7&&mx<width-27 && my>=184 && my<height-95){
            int index=(my-184)/17;
            if(index>=0&&index<contents.size()){
                selectedItemId=contents.get(index).getAsJsonObject().get("id").getAsString();
                status="Selected "+selectedItemId+". Enter quantity and request.";
                return true;
            }
        }
        int rows=Math.max(0,(height-236)/25);
        if(mx>=25&&mx<split-11&&my>=183&&my<183+rows*25) {
            int index=scroll+(my-183)/25;
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
