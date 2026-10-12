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
    // Highlight roots are returned to the requesting client in Snapshot,
    // rather than sending particles to every player on the server.
    private RackComputerNet() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Query.TYPE,Query.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OrderRequest.TYPE,OrderRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrderReply.TYPE,OrderReply.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GolemControl.TYPE,GolemControl.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PtpControl.TYPE,PtpControl.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Query.TYPE,(payload,context)->
                context.server().execute(()->answer(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(OrderRequest.TYPE,(payload,context)->
                context.server().execute(()->acceptOrder(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(GolemControl.TYPE,(payload,context)->
                context.server().execute(()->setGolemSorting(context.player(),payload)));
        ServerPlayNetworking.registerGlobalReceiver(PtpControl.TYPE,(payload,context)->
                context.server().execute(()->controlPtp(context.player(),payload)));

    }

    private static void reply(ServerPlayer player,String message) {
        ServerPlayNetworking.send(player,new OrderReply(message));
    }

    static List<StorageGolemShelfEntity> nearbyShelves(
            ServerLevel level,BlockPos computer) {
        List<StorageGolemShelfEntity> found=new ArrayList<>();
        Set<BlockPos> seen=new HashSet<>();
        for(net.minecraft.core.Direction side:net.minecraft.core.Direction.values()){
            BlockPos at=computer.relative(side);
            if(!level.hasChunkAt(at)
                    ||!level.getBlockState(at).is(RackContent.STORAGE_SHELF))continue;
            for(StorageGolemShelfEntity shelf:StorageGolemShelfEntity.linked(level,at))
                if(seen.add(shelf.getBlockPos()))found.add(shelf);
        }
        return found;
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
        OrderHubEntity hub=OrderHubEntity.nextToComputer(level,computer);
        if(hub==null) {
            for(net.minecraft.core.Direction side:net.minecraft.core.Direction.values()){
                BlockPos candidate=computer.relative(side);
                if(!level.hasChunkAt(candidate))continue;
                PtpBlockEntity point=PtpBlock.controller(level,candidate);
                if(point!=null){
                    reply(player,"Set a transfer point to OUTPUT and connect "
                            +"a Storage Golem Shelf to this computer.");
                    return;
                }
            }
            reply(player,"Attach a collector hub or a 3x3 Pallet Transfer Point.");
            return;
        }
        if(hub.collectors()==0){reply(player,"Insert up to 3 Order Collectors into the hub.");return;}
        if(!hub.queue(order.itemId(),order.quantity())){
            reply(player,"Hub's order queue is full (16 orders max).");return;
        }
        reply(player,"Order queued: "+order.quantity()+" × "+new ItemStack(item).getHoverName().getString()
                +(hub.hasPallet()?"":" — waiting for a pallet on the hub."));
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
        root.addProperty("dockedGolems",shelves.stream()
                .mapToInt(StorageGolemShelfEntity::docked).sum());
        root.addProperty("golemSortingEnabled",
                !shelves.isEmpty()&&shelves.stream().anyMatch(StorageGolemShelfEntity::sortingEnabled));
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
        String scannerSummary=StorageScanner.reportForComputer(level,computer,shelves);
        if(!scannerSummary.isBlank())root.addProperty("scannerSummary",scannerSummary);
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
