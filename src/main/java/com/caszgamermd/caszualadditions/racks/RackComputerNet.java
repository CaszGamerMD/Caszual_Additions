package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Authoritative, loaded-chunk-only rack search and inspection protocol. */
public final class RackComputerNet {
    public record Open(BlockPos computer) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(CaszualAdditions.id("rack_computer_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,Open::computer,Open::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Query(BlockPos computer,String query,BlockPos selected) implements CustomPacketPayload {
        public static final Type<Query> TYPE=new Type<>(CaszualAdditions.id("rack_computer_query"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Query> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,Query::computer,
                        ByteBufCodecs.stringUtf8(70),Query::query,
                        BlockPos.STREAM_CODEC,Query::selected,Query::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Snapshot(BlockPos computer,String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(CaszualAdditions.id("rack_computer_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,Snapshot::computer,
                        ByteBufCodecs.stringUtf8(120000),Snapshot::json,Snapshot::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record OrderRequest(BlockPos computer,String itemId,int quantity)
            implements CustomPacketPayload {
        public static final Type<OrderRequest> TYPE=new Type<>(CaszualAdditions.id("rack_order_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OrderRequest> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,OrderRequest::computer,
                        ByteBufCodecs.stringUtf8(128),OrderRequest::itemId,
                        ByteBufCodecs.INT,OrderRequest::quantity,OrderRequest::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** Bounded newline list: registry-id|quantity (max 16 lines).
     * One click on SUBMIT sends the entire shopping list. */
    public record OrderListRequest(BlockPos computer,String lines)
            implements CustomPacketPayload {
        public static final Type<OrderListRequest> TYPE=new Type<>(
                CaszualAdditions.id("rack_order_list_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OrderListRequest> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,OrderListRequest::computer,
                        ByteBufCodecs.stringUtf8(4096),OrderListRequest::lines,
                        OrderListRequest::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record OrderReply(String message) implements CustomPacketPayload {
        public static final Type<OrderReply> TYPE=new Type<>(CaszualAdditions.id("rack_order_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OrderReply> CODEC=
                StreamCodec.composite(ByteBufCodecs.stringUtf8(220),OrderReply::message,OrderReply::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record GolemControl(BlockPos computer,boolean enabled)
            implements CustomPacketPayload {
        public static final Type<GolemControl> TYPE=new Type<>(
                CaszualAdditions.id("storage_golem_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf,GolemControl> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,GolemControl::computer,
                        ByteBufCodecs.BOOL,GolemControl::enabled,GolemControl::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record GolemWork(BlockPos computer,int action) implements CustomPacketPayload {
        public static final Type<GolemWork> TYPE=new Type<>(
                CaszualAdditions.id("rack_golem_work"));
        public static final StreamCodec<RegistryFriendlyByteBuf,GolemWork> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,GolemWork::computer,
                        ByteBufCodecs.INT,GolemWork::action,GolemWork::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record PtpControl(BlockPos computer,BlockPos point,int action)
            implements CustomPacketPayload {
        public static final Type<PtpControl> TYPE=new Type<>(
                CaszualAdditions.id("rack_ptp_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PtpControl> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,PtpControl::computer,
                        BlockPos.STREAM_CODEC,PtpControl::point,
                        ByteBufCodecs.INT,PtpControl::action,PtpControl::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** Exactly one server-authorized pallet highlight across all viewers.
     * The renderer uses the existing sea-lantern-style emissive rack edges. */
    public record HighlightRequest(BlockPos computer,BlockPos rack)
            implements CustomPacketPayload {
        public static final Type<HighlightRequest> TYPE=new Type<>(
                CaszualAdditions.id("warehouse_highlight_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,HighlightRequest> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,HighlightRequest::computer,
                        BlockPos.STREAM_CODEC,HighlightRequest::rack,HighlightRequest::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record HighlightChanged(BlockPos rack,int ticks) implements CustomPacketPayload {
        public static final Type<HighlightChanged> TYPE=new Type<>(
                CaszualAdditions.id("warehouse_highlight_changed"));
        public static final StreamCodec<RegistryFriendlyByteBuf,HighlightChanged> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,HighlightChanged::rack,
                        ByteBufCodecs.INT,HighlightChanged::ticks,HighlightChanged::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }

    // Highlight roots are returned to the requesting client in Snapshot,
    // rather than sending particles to every player on the server.
    private RackComputerNet() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Query.TYPE,Query.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrderRequest.TYPE,OrderRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrderListRequest.TYPE,OrderListRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrderReply.TYPE,OrderReply.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GolemControl.TYPE,GolemControl.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PtpControl.TYPE,PtpControl.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GolemWork.TYPE,GolemWork.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HighlightRequest.TYPE,HighlightRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HighlightChanged.TYPE,HighlightChanged.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Query.TYPE,(payload,context)->
                context.server().execute(()->answer(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(OrderRequest.TYPE,(payload,context)->
                context.server().execute(()->acceptOrder(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(OrderListRequest.TYPE,(payload,context)->
                context.server().execute(()->acceptOrderList(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(GolemControl.TYPE,(payload,context)->
                context.server().execute(()->setGolemSorting(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(PtpControl.TYPE,(payload,context)->
                context.server().execute(()->controlPtp(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(GolemWork.TYPE,(payload,context)->
                context.server().execute(()->requestGolemWork(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(HighlightRequest.TYPE,(payload,context)->
                context.server().execute(()->requestHighlight(context.player(),payload)));

    }

    private static void reply(ServerPlayer player,String message) {
        ServerPlayNetworking.send(player,new OrderReply(message));
    }

    static List<StorageGolemShelfEntity> nearbyShelves(
            ServerLevel level,BlockPos computer) {
        return WarehouseConnections.shelvesForComputer(level,computer);
    }
    private static void controlPtp(ServerPlayer player,PtpControl control){
        ServerLevel level=player.level();
        BlockPos computer=control.computer();
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                computer.getZ()+.5)>100||!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))return;
        List<PtpBlockEntity> linked=StoragePtpNetwork.attached(
                level,computer,nearbyShelves(level,computer));
        PtpBlockEntity target=null;
        for(PtpBlockEntity ptp:linked)
            if(ptp.getBlockPos().equals(control.point())){target=ptp;break;}
        if(target==null){reply(player,"PTP no longer connected to this workstation.");return;}
        boolean success=switch(control.action()){
            case 0->target.changeModeFromComputer();
            case 1->target.changeWrapFromComputer();
            default->false;
        };
        reply(player,success
                ?"PTP updated at "+target.getBlockPos().toShortString()
                :"PTP operation blocked: check pallet, Wrap Unit, pending orders or active haul.");
    }

    private static void requestHighlight(ServerPlayer player,HighlightRequest request){
        ServerLevel level=player.level();
        BlockPos computer=request.computer();
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER)
                ||player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                        computer.getZ()+.5)>100)return;
        RackBlockEntity selection=null;
        for(RackBlockEntity rack:WarehouseNetwork.fromComputer(level,computer).racks()){
            if(rack.getBlockPos().equals(request.rack())&&rack.hasPallet()){
                selection=rack;break;
            }
        }
        if(selection==null){
            reply(player,"Highlight needs a loaded pallet in this connected warehouse.");
            return;
        }
        // Send one replacement to EVERY online viewer, even viewers at other
        // computers. Other dimensions receive 0 ticks to clear old selections.
        for(ServerPlayer online:level.getServer().getPlayerList().getPlayers()){
            int ticks=online.level()==level?200:0;
            ServerPlayNetworking.send(online,
                    new HighlightChanged(selection.getBlockPos(),ticks));
        }
        reply(player,"Pallet "+selection.getBlockPos().toShortString()
                +" highlighted for all players for 10 seconds.");
    }

    private static void requestGolemWork(ServerPlayer player,GolemWork request){
        ServerLevel level=player.level();
        BlockPos computer=request.computer();
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER)
                ||player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                        computer.getZ()+.5)>100)return;
        String result;
        switch(request.action()){
            case 0 -> result=StoragePalletCrew.enlist(level,player,computer);
            case 1 -> result=StoragePalletCrew.dismissWarehouse(level,computer);
            case 2 -> {
                int deployed=0;
                for(StorageGolemShelfEntity shelf:nearbyShelves(level,computer)){
                    if(!shelf.sortingEnabled()||shelf.docked()==0)continue;
                    int before=shelf.docked();
                    StorageGolemDispatch.tryDispatch(level,shelf);
                    deployed+=before-shelf.docked();
                }
                result=deployed>0?"Dispatched "+deployed+" Storage Golem task(s)."
                        :"No eligible queued input/output tasks found. Check AUTO, stock and PTPs.";
            }
            default -> result="Unknown Golem command.";
        }
        reply(player,result);
        answer(player,new Query(computer,"",BlockPos.ZERO));
    }

    private static void setGolemSorting(ServerPlayer player,GolemControl control){
        ServerLevel level=player.level();
        BlockPos at=control.computer();
        if(player.distanceToSqr(at.getX()+.5,at.getY()+.5,at.getZ()+.5)>100
                ||!level.getBlockState(at).is(RackContent.RACK_COMPUTER))return;
        List<StorageGolemShelfEntity> shelves=nearbyShelves(level,at);
        if(shelves.isEmpty()){reply(player,"Connect a Storage Golem Shelf to this computer.");return;}
        for(StorageGolemShelfEntity s:shelves)s.setSortingEnabled(control.enabled());
        reply(player,control.enabled()?"Storage Golem sorting enabled.":"Storage Golem sorting paused.");
        answer(player,new Query(at,"",BlockPos.ZERO));
    }

    private static void acceptOrderList(ServerPlayer player,OrderListRequest order){
        ServerLevel level=player.level();
        BlockPos computer=order.computer();
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER)
                ||player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                        computer.getZ()+.5)>100)return;
        String[] rows=order.lines().split("\\n",-1);
        if(rows.length==0||rows.length>16){
            reply(player,"An order can contain 1–16 distinct items.");return;
        }
        var racks=RackNetwork.fromComputer(level,computer);
        if(racks.isEmpty()){reply(player,"No connected racks.");return;}
        java.util.LinkedHashMap<String,Integer> requested=new java.util.LinkedHashMap<>();
        for(String row:rows){
            String[] pair=row.split("\\|",-1);
            if(pair.length!=2){reply(player,"Invalid order-list entry.");return;}
            var id=net.minecraft.resources.Identifier.tryParse(pair[0]);
            int count;
            try{count=Integer.parseInt(pair[1]);}
            catch(NumberFormatException exception){
                reply(player,"Invalid order quantity.");return;
            }
            if(id==null||count<1||count>4096||requested.containsKey(id.toString())){
                reply(player,"Duplicate/invalid item or quantity (1–4096).");return;
            }
            var item=BuiltInRegistries.ITEM.getValue(id);
            if(item==null||!BuiltInRegistries.ITEM.getKey(item).equals(id)){
                reply(player,"Unknown item "+pair[0]);return;
            }
            boolean found=false;
            for(RackBlockEntity rack:racks){
                for(int slot=0;slot<rack.getContainerSize();slot++)
                    if(rack.getItem(slot).is(item)){found=true;break;}
                if(found)break;
            }
            if(!found){reply(player,"Item not stocked: "+pair[0]);return;}
            requested.put(id.toString(),count);
        }
        if(requested.isEmpty()){reply(player,"Order list is empty.");return;}
        List<StorageGolemShelfEntity> shelves=nearbyShelves(level,computer);
        if(shelves.isEmpty()){reply(player,"Connect a Storage Golem Shelf.");return;}
        PtpBlockEntity output=StorageGolemDispatch.outputPoint(level,shelves,computer);
        if(output==null){
            reply(player,"Connect a PTP set to OUTPUT with a pallet.");return;
        }
        java.util.List<PtpBlockEntity.OutputOrder> lines=new java.util.ArrayList<>();
        requested.forEach((id,count)->lines.add(new PtpBlockEntity.OutputOrder(id,count)));
        if(!output.queueOutputList(lines)){
            reply(player,"Output queue full or busy (16 items max); nothing was added.");
            return;
        }
        reply(player,"Order submitted: "+requested.size()
                +" item types. Storage Golems will collect them for the Shulker Station.");
        answer(player,new Query(computer,"",BlockPos.ZERO));
    }

    private static void acceptOrder(ServerPlayer player,OrderRequest order) {
        ServerLevel level=player.level();
        BlockPos computer=order.computer();
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,computer.getZ()+.5)>100
                || !level.getBlockState(computer).is(RackContent.RACK_COMPUTER))return;
        if(order.quantity()<1||order.quantity()>4096) {
            reply(player,"Quantity must be 1–4096.");return;
        }
        var id=net.minecraft.resources.Identifier.tryParse(order.itemId());
        if(id==null) {reply(player,"Choose an item from a rack inventory.");return;}
        var item=BuiltInRegistries.ITEM.getValue(id);
        if(item==null||!BuiltInRegistries.ITEM.getKey(item).equals(id)) {
            reply(player,"Unknown item.");return;
        }
        List<RackBlockEntity> racks=RackNetwork.fromComputer(level,computer);
        if(racks.isEmpty()){reply(player,"Computer is not connected to any rack.");return;}
        boolean found=false;
        for(RackBlockEntity rack:racks){
            for(int i=0;i<rack.getContainerSize();i++){
                if(rack.getItem(i).is(item)){found=true;break;}
            }
            if(found)break;
        }
        if(!found){reply(player,"Item not found in connected racks.");return;}
        List<StorageGolemShelfEntity> shelves=nearbyShelves(level,computer);
        PtpBlockEntity output=StorageGolemDispatch.outputPoint(level,shelves,computer);
        if(output!=null){
            if(shelves.isEmpty()){
                reply(player,"Attach a Storage Golem Shelf to this Rack Computer.");return;
            }
            if(!output.queueOutput(order.itemId(),order.quantity())){
                reply(player,"Output PTP order queue is full (16 max).");return;
            }
            reply(player,"Storage Golems queued "+order.quantity()+" × "
                    +new ItemStack(item).getHoverName().getString()
                    +(output.readyForOutput()?" for output.":" — awaiting an unsealed output pallet."));
            return;
        }
        reply(player,"No OUTPUT Pallet Transfer Point found. Attach a 3x3 PTP "
                +"to the Rack Computer or its linked Storage Golem Shelves, "
                +"set it to OUTPUT, and dock Storage Golems.");
    }

    private static void answer(ServerPlayer player,Query query) {
        ServerLevel level=player.level();
        BlockPos computer=query.computer();
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,computer.getZ()+.5)>100
                || !level.getBlockState(computer).is(RackContent.RACK_COMPUTER))return;
        List<RackBlockEntity> racks=RackNetwork.fromComputer(level,computer);
        String term=query.query().strip().toLowerCase(java.util.Locale.ROOT);
        JsonObject root=new JsonObject();
        JsonArray list=new JsonArray();
        JsonArray highlights=new JsonArray();
        root.addProperty("query",term);
        root.addProperty("networkSize",racks.size());
        List<StorageGolemShelfEntity> shelves=nearbyShelves(level,computer);
        root.addProperty("shelfCount",shelves.size());
        JsonArray transferPoints=new JsonArray();
        for(PtpBlockEntity point:StoragePtpNetwork.attached(level,computer,shelves)){
            JsonObject cell=new JsonObject();
            cell.addProperty("x",point.getBlockPos().getX());
            cell.addProperty("y",point.getBlockPos().getY());
            cell.addProperty("z",point.getBlockPos().getZ());
            cell.addProperty("input",point.input());
            cell.addProperty("pallet",point.hasPallet());
            cell.addProperty("sealed",point.sealed());
            cell.addProperty("wrap",point.hasWrap());
            cell.addProperty("hopper",point.hasAdapter());
            cell.addProperty("orders",point.pendingOutputOrders());
            cell.addProperty("hauling",point.hauling());
            transferPoints.add(cell);
        }
        root.add("ptps",transferPoints);
        JsonArray stations=new JsonArray();
        for(ShulkerTankEntity station:WarehouseNetwork.fromComputer(level,computer).tanks()){
            JsonObject row=new JsonObject();
            row.addProperty("x",station.getBlockPos().getX());
            row.addProperty("y",station.getBlockPos().getY());
            row.addProperty("z",station.getBlockPos().getZ());
            row.addProperty("empty",station.count());
            row.addProperty("packing",station.activeCount());
            row.addProperty("ready",station.finishedCount());
            row.addProperty("connected",station.attachedOutput(level)!=null);
            stations.add(row);
        }
        root.add("stations",stations);
        root.addProperty("dockedGolems",shelves.stream()
                .mapToInt(StorageGolemShelfEntity::docked).sum());
        root.addProperty("golemSortingEnabled",
                !shelves.isEmpty()&&shelves.stream().anyMatch(StorageGolemShelfEntity::sortingEnabled));
        root.addProperty("industrialConcrete",
                WarehouseNetwork.fromComputer(level,computer).concreteBlocks());
        // Track shelf-assigned workers both locally and around online players.
        // A player carrying a pallet may be far from the computer.
        Set<BlockPos> homes=new HashSet<>();
        for(StorageGolemShelfEntity shelf:shelves)homes.add(shelf.getBlockPos());
        Map<java.util.UUID,StorageGolem> active=new LinkedHashMap<>();
        java.util.function.Consumer<StorageGolem> include=worker->{
            if(homes.contains(worker.shelfHome())&&active.size()<96)
                active.putIfAbsent(worker.getUUID(),worker);
        };
        for(StorageGolem worker:level.getEntitiesOfClass(StorageGolem.class,
                new net.minecraft.world.phys.AABB(computer).inflate(96)))
            include.accept(worker);
        for(ServerPlayer online:level.players())
            for(StorageGolem worker:level.getEntitiesOfClass(StorageGolem.class,
                    new net.minecraft.world.phys.AABB(online.blockPosition()).inflate(48)))
                include.accept(worker);
        JsonArray workerRows=new JsonArray();
        for(StorageGolem worker:active.values()){
            JsonObject job=new JsonObject();
            job.addProperty("id",worker.getStringUUID());
            job.addProperty("work",worker.currentWork());
            job.addProperty("cargo",worker.carriedDescription());
            job.addProperty("owner",worker.assignedOwner());
            job.addProperty("x",worker.blockPosition().getX());
            job.addProperty("y",worker.blockPosition().getY());
            job.addProperty("z",worker.blockPosition().getZ());
            job.addProperty("target",worker.workTarget().toShortString());
            workerRows.add(job);
        }
        // One MovingPallet represents four docked-origin workers while
        // carrying. Report one rig, not four independently pathing Golems.
        for(ServerPlayer online:level.players()){
            for(MovingPallet rig:level.getEntitiesOfClass(MovingPallet.class,
                    new net.minecraft.world.phys.AABB(online.blockPosition()).inflate(96))){
                if(!rig.fromWarehouse(shelves))continue;
                JsonObject task=new JsonObject();
                task.addProperty("id",rig.getStringUUID());
                task.addProperty("work",rig.loaded()
                        ?"4-GOLEM PALLET TEAM / FOLLOWING PLAYER"
                        :"4-GOLEM TEAM / DETACHING");
                task.addProperty("cargo",rig.loaded()?"216-SLOT WRAPPED PALLET":"EMPTY");
                task.addProperty("owner","TEAM");
                task.addProperty("x",rig.blockPosition().getX());
                task.addProperty("y",rig.blockPosition().getY());
                task.addProperty("z",rig.blockPosition().getZ());
                task.addProperty("target","PLAYER");
                workerRows.add(task);
            }
        }
        root.add("workers",workerRows);
        root.addProperty("activeGolems",workerRows.size());
        // Linked shelves can also reach an output PTP without requiring
        // its controller to sit immediately against the computer.
        PtpBlockEntity outbound=StorageGolemDispatch.outputPoint(level,shelves,computer);
        if(outbound!=null)
            root.addProperty("ptpSummary","PTP OUTPUT / "+
                    (outbound.hasPallet()?"pallet loaded":"no pallet")
                    +(outbound.sealed()?" / SEALED":"")
                    +" / "+outbound.pendingOutputOrders()+" queued order(s)"
                    +" / "+outbound.pendingOutputItems()+" outstanding items");
        for(net.minecraft.core.Direction side:net.minecraft.core.Direction.values()){
            BlockPos adjacent=computer.relative(side);
            if(!level.hasChunkAt(adjacent))continue;
            PtpBlockEntity ptp=PtpBlock.controller(level,adjacent);
            if(ptp==null)continue;
            if(outbound==null)
                root.addProperty("ptpSummary","PTP "+(ptp.input()?"INPUT":"OUTPUT")
                        +" / "+(ptp.hasPallet()?"pallet loaded":"no pallet")
                        +(ptp.sealed()?" / SEALED":"")
                        +(ptp.hasAdapter()?" / hopper adapter":""));
            break;
        }
        int matches=0;
        int totalItems=0;
        RackBlockEntity selected=null;
        for(RackBlockEntity rack:racks){
            if(rack.getBlockPos().equals(query.selected())) selected=rack;
            totalItems+=(int)Math.min(Integer.MAX_VALUE-rack.totalItems(),rack.totalItems());
            if(list.size()>=RackNetwork.LIMIT)continue;
            boolean hit=term.isEmpty()
                    || RackNetwork.label(level,rack).toLowerCase(java.util.Locale.ROOT).contains(term)
                    || RackNetwork.aisle(level,rack).toLowerCase(java.util.Locale.ROOT).contains(term);
            if(!hit) {
                for(int i=0;i<rack.getContainerSize();i++){
                    ItemStack stack=rack.getItem(i);
                    if(!stack.isEmpty() && (stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(term)
                            || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(term))) {
                        hit=true;break;
                    }
                }
            }
            if(!hit)continue;
            matches++;
            JsonObject row=new JsonObject();
            row.addProperty("name",RackNetwork.label(level,rack));
            row.addProperty("aisle",RackNetwork.aisle(level,rack));
            row.addProperty("x",rack.getBlockPos().getX());
            row.addProperty("y",rack.getBlockPos().getY());
            row.addProperty("z",rack.getBlockPos().getZ());
            row.addProperty("slots",rack.occupiedSlots());
            row.addProperty("capacity",rack.getContainerSize());
            row.addProperty("items",rack.totalItems());
            row.addProperty("pallet",rack.hasPallet());
            row.addProperty("filter",rack.filter());
            list.add(row);
            if(!term.isEmpty()&&matches<=RackNetwork.LIMIT){
                JsonObject position=new JsonObject();
                position.addProperty("x",rack.getBlockPos().getX());
                position.addProperty("y",rack.getBlockPos().getY());
                position.addProperty("z",rack.getBlockPos().getZ());
                highlights.add(position);
            }
        }
        root.add("racks",list);
        root.add("highlights",highlights);
        root.addProperty("matches",matches);
        root.addProperty("totalItems",totalItems);
        if(selected!=null){
            JsonArray contents=new JsonArray();
            Map<String,Integer> combined=new LinkedHashMap<>();
            for(int i=0;i<selected.getContainerSize();i++){
                ItemStack stack=selected.getItem(i);
                if(stack.isEmpty())continue;
                String name=stack.getHoverName().getString();
                String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()+"|"+name;
                combined.merge(key,stack.getCount(),Integer::sum);
            }
            for(var e:combined.entrySet()){
                if(contents.size()>=96)break;
                String[] parts=e.getKey().split("\\|",2);
                JsonObject row=new JsonObject();
                row.addProperty("id",parts[0]);
                row.addProperty("name",parts.length==2?parts[1]:parts[0]);
                row.addProperty("count",e.getValue());
                contents.add(row);
            }
            root.add("contents",contents);
        }
        ServerPlayNetworking.send(player,new Snapshot(computer,root.toString()));
    }
}
