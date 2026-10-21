package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Portable Warehouse Scanner. The scanner stores only its warehouse link;
 * a real Storage Golem leader is the single owner of a lifted pallet.
 */
public final class StorageScannerItem extends Item {
    public StorageScannerItem(Properties props){super(props);}

    static BlockPos linkedComputer(ItemStack scanner,ServerLevel world){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return null;
        CompoundTag tag=data.copyTag();
        if(!tag.getStringOr("warehouse_dimension","").equals(world.dimension().toString()))
            return null;
        if(!tag.contains("warehouse_x")||!tag.contains("warehouse_y")
                ||!tag.contains("warehouse_z"))return null;
        return new BlockPos(tag.getIntOr("warehouse_x",0),
                tag.getIntOr("warehouse_y",0),tag.getIntOr("warehouse_z",0));
    }
    private static void bind(ItemStack scanner,ServerLevel world,BlockPos computer){
        CustomData existing=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=existing==null?new CompoundTag():existing.copyTag();
        tag.putString("warehouse_dimension",world.dimension().toString());
        tag.putInt("warehouse_x",computer.getX());
        tag.putInt("warehouse_y",computer.getY());
        tag.putInt("warehouse_z",computer.getZ());
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    /** Removing a warehouse link must clear the full saved location and any
     * queued Remote request. Scanner mode is intentionally retained.
     * The same action can be used to recover a scanner bound to a destroyed
     * computer; no old Computer needs to be loaded.
     */
    private static void unlink(ItemStack scanner){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return;
        CompoundTag tag=data.copyTag();
        for(String key:new String[]{"warehouse_dimension","warehouse_x",
                "warehouse_y","warehouse_z","remote_pickup_pending",
                "remote_pickup_x","remote_pickup_y","remote_pickup_z"})
            tag.remove(key);
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    /** A single saved mode belongs to the SCANNER ItemStack, so switching
     * modes cannot silently dispatch four warehouse workers later. */
    public enum Mode { REMOTE, DIRECT, FORKLIFT, RECALL }
    public static Mode mode(ItemStack scanner){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return Mode.REMOTE;
        String name=data.copyTag().getStringOr("scanner_mode","REMOTE");
        try{return Mode.valueOf(name);}
        catch(IllegalArgumentException e){return Mode.REMOTE;}
    }
    private static Mode toggleMode(ItemStack scanner){
        Mode next=Mode.values()[(mode(scanner).ordinal()+1)%Mode.values().length];
        CustomData existing=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=existing==null?new CompoundTag():existing.copyTag();
        tag.putString("scanner_mode",next.name());
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(next!=Mode.REMOTE)clearPickup(scanner);
        return next;
    }
    /** Per-stack pallet selection for the Forklift mode. Dimension encoded so
     * a selection never crosses worlds. This is ONLY a reference, not cargo. */
    private static void selectForkliftSource(ItemStack scanner,ServerLevel world,BlockPos pos){
        CustomData current=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=current==null?new CompoundTag():current.copyTag();
        tag.putString("forklift_source_dimension",world.dimension().toString());
        tag.putInt("forklift_source_x",pos.getX());
        tag.putInt("forklift_source_y",pos.getY());
        tag.putInt("forklift_source_z",pos.getZ());
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static BlockPos forkliftSource(ItemStack scanner,ServerLevel world){
        CustomData current=scanner.get(DataComponents.CUSTOM_DATA);
        if(current==null)return null;
        CompoundTag tag=current.copyTag();
        if(!tag.getStringOr("forklift_source_dimension","").equals(world.dimension().toString())
                ||!tag.contains("forklift_source_x"))return null;
        return new BlockPos(tag.getIntOr("forklift_source_x",0),
                tag.getIntOr("forklift_source_y",0),tag.getIntOr("forklift_source_z",0));
    }
    private static void clearForkliftSource(ItemStack scanner){
        CustomData current=scanner.get(DataComponents.CUSTOM_DATA);
        if(current==null)return;
        CompoundTag tag=current.copyTag();
        for(String key:new String[]{"forklift_source_dimension","forklift_source_x",
                "forklift_source_y","forklift_source_z"})tag.remove(key);
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static WarehouseForklift availableForklift(ServerLevel world,BlockPos at,
            BlockPos warehouse){
        java.util.Set<BlockPos> shelves=new java.util.HashSet<>();
        for(StorageGolemShelfEntity shelf:WarehouseNetwork.fromComputer(world,warehouse).shelves())
            shelves.add(shelf.getBlockPos());
        WarehouseForklift best=null;
        double nearest=Double.POSITIVE_INFINITY;
        for(WarehouseForklift forklift:world.getEntitiesOfClass(WarehouseForklift.class,
                new net.minecraft.world.phys.AABB(at).inflate(64),f->!f.busy())){
            if(!forklift.hasWarehouseDriver(world,shelves))continue;
            double distance=forklift.blockPosition().distSqr(at);
            if(distance<nearest){nearest=distance;best=forklift;}
        }
        return best;
    }
    public static void onForkliftClick(ServerLevel world,ServerPlayer player,
            ItemStack scanner,WarehouseForklift lift){
        if(mode(scanner)!=Mode.FORKLIFT)return;
        if(lift.busy()){
            message(player,"Forklift is working: "+lift.status());
            return;
        }
        if(lift.hasDriver(world)){
            message(player,lift.requestDriverDismissal(world));
            return;
        }
        BlockPos home=linkedComputer(scanner,world);
        if(home==null){message(player,"Scanner not linked to a warehouse.");return;}
        message(player,lift.boardEscort(world,player.getUUID(),home));
    }
    private static String forkliftTarget(ServerLevel level,ServerPlayer user,
            ItemStack scanner,BlockPos clicked,BlockPos home){
        return forkliftTarget(level,user,scanner,clicked,clicked,home);
    }
    private static String forkliftTarget(ServerLevel level,ServerPlayer user,
            ItemStack scanner,BlockPos clicked,BlockPos landing,BlockPos home){
        RackBlockEntity rack=RackBlock.controller(level,clicked);
        var pallet=StorageGroundPalletTransfer.wrappedController(level,clicked);
        BlockPos source=forkliftSource(scanner,level);
        if(source==null){
            if(rack!=null&&rack.hasPallet()){
                selectForkliftSource(scanner,level,rack.getBlockPos());
                return "FORKLIFT: rack pallet selected; now click a destination.";
            }
            if(pallet!=null){
                selectForkliftSource(scanner,level,pallet.getBlockPos());
                return "FORKLIFT: ground pallet selected; now click a rack or ground destination.";
            }
            return "FORKLIFT: select a wrapped pallet on a rack or ground first.";
        }
        WarehouseForklift forklift=availableForklift(level,clicked,home);
        if(forklift==null)return "FORKLIFT: need a free Golem-driven forklift in this warehouse.";
        if(rack!=null){
            if(rack.hasPallet())return "FORKLIFT: destination rack must be empty.";
            boolean started=level.getBlockEntity(source) instanceof RackBlockEntity from
                    &&from.hasPallet()?forklift.orderRackToRack(level,from,rack)
                    :forklift.order(level,rack,source);
            if(started)clearForkliftSource(scanner);
            return started?"FORKLIFT: pallet ordered into rack "+rack.getBlockPos().toShortString()
                    :"FORKLIFT: cannot start. Check source pallet, aisle and mast clearance.";
        }
        boolean started=forklift.orderGround(level,source,landing,user.getDirection());
        if(started)clearForkliftSource(scanner);
        return started?"FORKLIFT: pallet ordered to ground "+landing.toShortString()
                :"FORKLIFT: need a clear solid 2x2 ground destination.";
    }
    private record RecallTap(java.util.UUID target,long tick){}
    private static final java.util.Map<java.util.UUID,RecallTap> RECALL_CLICKS=
            new java.util.HashMap<>();
    public static boolean recallDoubleClick(ServerLevel world,ServerPlayer player,
            java.util.UUID target){
        long tick=world.getGameTime();
        RecallTap earlier=RECALL_CLICKS.put(player.getUUID(),
                new RecallTap(target,tick));
        return earlier!=null&&earlier.target().equals(target)
                &&tick-earlier.tick()>=0&&tick-earlier.tick()<=12;
    }
    private static boolean dispatched(String response){
        return response.startsWith("Three Golems dispatched")
                ||response.startsWith("Four Golems dispatched");
    }
    private static void message(ServerPlayer player,String text){
        // Keep routine Scanner output in the action bar. Rack Computer
        // DEBUG can opt in to persistent chat while multi-job queue stays.
        ItemStack hand=player.getMainHandItem();
        BlockPos computer=hand.is(RackContent.STORAGE_SCANNER_ITEM)
                ?linkedComputer(hand,player.level()):null;
        boolean chat=computer!=null
                &&RackComputerNet.nearbyShelves(player.level(),computer).stream()
                        .anyMatch(StorageGolemShelfEntity::debugChat);
        if(chat)player.sendSystemMessage(Component.literal(text));
        else WarehouseGolemDebug.actionBar(player,Component.literal(text));
    }

    /** Persistent per-scanner FIFO. Every requested pallet represents a
     * separate four-Golem crew, never an additional order for one crew. */
    private static final int MAX_PENDING_REMOTE=64;

    private static List<BlockPos> queuedPickups(CompoundTag tag){
        List<BlockPos> targets=new java.util.ArrayList<>();
        // Old worlds store a single pending pickup. Migrate it in memory and
        // drop the legacy keys on the next successful queue write.
        if(tag.getBooleanOr("remote_pickup_pending",false))
            targets.add(new BlockPos(tag.getIntOr("remote_pickup_x",0),
                    tag.getIntOr("remote_pickup_y",0),
                    tag.getIntOr("remote_pickup_z",0)));
        int size=Math.clamp(tag.getIntOr("remote_pickup_queue_size",0),
                0,MAX_PENDING_REMOTE);
        for(int i=0;i<size;i++){
            BlockPos target=new BlockPos(tag.getIntOr("remote_pickup_"+i+"_x",0),
                    tag.getIntOr("remote_pickup_"+i+"_y",0),
                    tag.getIntOr("remote_pickup_"+i+"_z",0));
            if(!targets.contains(target))targets.add(target);
        }
        return targets;
    }

    private static void storePickups(ItemStack scanner,List<BlockPos> targets){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=data==null?new CompoundTag():data.copyTag();
        tag.remove("remote_pickup_pending");
        tag.remove("remote_pickup_x");
        tag.remove("remote_pickup_y");
        tag.remove("remote_pickup_z");
        int prior=Math.clamp(tag.getIntOr("remote_pickup_queue_size",0),
                0,MAX_PENDING_REMOTE);
        for(int i=0;i<Math.max(prior,targets.size());i++){
            tag.remove("remote_pickup_"+i+"_x");
            tag.remove("remote_pickup_"+i+"_y");
            tag.remove("remote_pickup_"+i+"_z");
        }
        tag.putInt("remote_pickup_queue_size",targets.size());
        for(int i=0;i<targets.size();i++){
            BlockPos target=targets.get(i);
            tag.putInt("remote_pickup_"+i+"_x",target.getX());
            tag.putInt("remote_pickup_"+i+"_y",target.getY());
            tag.putInt("remote_pickup_"+i+"_z",target.getZ());
        }
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }

    private static String queuePickup(ItemStack scanner,BlockPos target){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        List<BlockPos> targets=queuedPickups(
                data==null?new CompoundTag():data.copyTag());
        if(targets.contains(target))
            return "REMOTE: target "+target.toShortString()+" is already queued.";
        if(targets.size()>=MAX_PENDING_REMOTE)
            return "REMOTE: scanner queue full ("+MAX_PENDING_REMOTE+" targets).";
        targets.add(target.immutable());
        storePickups(scanner,targets);
        return "REMOTE: queued "+target.toShortString()+"; "
                +targets.size()+" job(s) awaiting dispatch.";
    }

    private static void clearPickup(ItemStack scanner){
        if(scanner.get(DataComponents.CUSTOM_DATA)!=null)
            storePickups(scanner,java.util.List.of());
    }

    /** Try all queued targets. Failure to dispatch one crew must not block
     * independent jobs with clear aisles and their own four free workers.
     * The four shelf tokens are consumed atomically by the dispatcher and
     * returned on spawn failure. No partial (1-3 worker) team runs a job. */
    private static String dispatchPending(ItemStack scanner,ServerLevel world,
            ServerPlayer player,boolean includeWaiting){
        if(mode(scanner)!=Mode.REMOTE)return "";
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return "";
        List<BlockPos> pending=queuedPickups(data.copyTag());
        if(pending.isEmpty())return "";
        BlockPos computer=linkedComputer(scanner,world);
        if(computer==null){
            clearPickup(scanner);
            return "INVALID: scanner link lost; queued jobs cleared.";
        }
        WarehouseRemoteTickets.keep(world,computer);
        List<BlockPos> remaining=new java.util.ArrayList<>();
        List<String> reports=new java.util.ArrayList<>();
        String waitReason="";
        for(int i=0;i<pending.size();i++){
            BlockPos target=pending.get(i);
            String result=RemotePalletDispatch.request(world,player,computer,target);
            if(dispatched(result)||result.startsWith("INVALID:")){
                reports.add(result);
            }else{
                remaining.add(target);
                if(waitReason.isEmpty())waitReason=result;
                // All queued jobs use the same warehouse. Without four
                // docked Golems there is no full crew for a later job.
                if(result.equals("WAITING: four docked Golems required.")){
                    remaining.addAll(pending.subList(i+1,pending.size()));
                    break;
                }
            }
        }
        if(!remaining.equals(pending))storePickups(scanner,remaining);
        if(includeWaiting&&!remaining.isEmpty())
            reports.add("REMOTE: "+remaining.size()+" job(s) queued. "+waitReason);
        return String.join("\n",reports);
    }

    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%100!=0)return;
            for(ServerPlayer player:server.getPlayerList().getPlayers()){
                if(!(player.level() instanceof ServerLevel level))continue;
                var inventory=player.getInventory();
                for(int i=0;i<inventory.getContainerSize();i++){
                    ItemStack scanner=inventory.getItem(i);
                    if(!scanner.is(RackContent.STORAGE_SCANNER_ITEM))continue;
                    String status=dispatchPending(scanner,level,player,false);
                    if(!status.isEmpty())player.sendSystemMessage(Component.literal(status));
                }
            }
        });
    }

    /** Linking ALWAYS requires shift-right-click on the Rack Computer.
     * The computer itself handles ordinary clicks by opening its GUI.
     * Neither action summons/dismisses Golems or runs diagnostics. */
    public static void onComputerClick(ServerLevel level,ServerPlayer player,
            ItemStack scanner,BlockPos computer){
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                computer.getZ()+.5)>64
                ||!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))return;
        BlockPos previous=linkedComputer(scanner,level);
        boolean same=computer.equals(previous);
        String crew=StoragePalletCrew.status(level,player);
        // Loaded manual cargo belongs to the old warehouse. Never redirect
        // its return/delivery route to a newly selected computer.
        boolean carrying=crew.contains("carry one shared pallet")
                ||crew.contains("carrying wrapped pallet: yes")
                ||crew.contains("heading to assigned pallet placement");
        if(same){
            if(carrying){
                message(player,"Scanner still linked: deliver the carried pallet "
                        +"before unlinking, so its crew can return safely.");
                return;
            }
            // Clicking the same Warehouse Computer refreshes the link.
            // Deliberate unlinking is no longer mixed into ordinary linking.
            clearPickup(scanner);
            bind(scanner,level,computer);
            WarehouseGolemDebug.actionBar(player,Component.literal(
                    "Scanner is now linked to "+
                    WarehouseComputerBlockEntity.name(level,computer)));
            return;
        }
        if(previous!=null&&!previous.equals(computer)){
            if(carrying){
                message(player,"Scanner cannot relink while your direct crew "
                        +"carries a pallet. Deliver it first, then relink.");
                return;
            }
            // Unloaded/idle followers can safely return to their own original
            // shelves while a new warehouse takes over this scanner.
            StoragePalletCrew.recallFollowing(level,player);
        }
        clearPickup(scanner);
        bind(scanner,level,computer);
        java.util.List<StorageGolemShelfEntity> shelves=
                RackComputerNet.nearbyShelves(level,computer);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        WarehouseGolemDebug.actionBar(player,Component.literal(
                "Scanner is now linked to "+
                WarehouseComputerBlockEntity.name(level,computer)));
    }

    /** Shelf block explicitly forwards Scanner clicks here so the vanilla
     * shelf UI/insert logic cannot consume a Direct summon attempt. */
    public static void onShelfClick(ServerLevel level,ServerPlayer player,
            ItemStack scanner,BlockPos shelf){
        if(mode(scanner)!=Mode.DIRECT&&mode(scanner)!=Mode.FORKLIFT){
            message(player,"Switch Scanner to Direct or Forklift mode.");
            return;
        }
        BlockPos home=linkedComputer(scanner,level);
        if(home==null){
            message(player,"Link scanner first: right-click your Warehouse Computer.");
            return;
        }
        if(!level.hasChunkAt(shelf)
                ||!(level.getBlockEntity(shelf) instanceof StorageGolemShelfEntity)){
            message(player,"Not a valid Golem Shelf.");return;
        }
        if(mode(scanner)==Mode.FORKLIFT){
            message(player,StorageGolemForkliftEscort.summon(level,player,home,shelf));
            return;
        }
        message(player,StoragePalletCrew.enlistFromShelf(level,player,home,shelf));
    }

    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        Level level=context.getLevel();
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer user))return InteractionResult.PASS;
        BlockPos clicked=context.getClickedPos();
        // Cargo selection proxies are part of the SAME ground pallet.
        // Preserve wrapper/scanner/stack actions when clicking loaded cargo.
        if(level.getBlockState(clicked).getBlock()
                instanceof com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock){
            BlockPos owner=com.caszgamermd.caszualadditions.pallet
                    .PalletCargoCollisionBlock.owningPallet(level,clicked);
            if(owner!=null)clicked=owner;
        }
        ItemStack scanner=context.getItemInHand();
        // Right-clicking a computer without crouching opens the terminal
        // (handled by RackComputerBlock); only crouching alters the link.
        if(server.getBlockState(clicked).is(RackContent.RACK_COMPUTER)){
            onComputerClick(server,user,scanner,clicked);
            return InteractionResult.SUCCESS;
        }

        BlockPos home=linkedComputer(scanner,server);
        if(home==null){
            message(user,"Scanner not linked. Shift-right-click a Rack Computer to link.");
            return InteractionResult.SUCCESS;
        }

        if(mode(scanner)==Mode.FORKLIFT){
            message(user,forkliftTarget(server,user,scanner,clicked,
                    clicked.relative(context.getClickedFace()),home));
            return InteractionResult.SUCCESS;
        }
        if(mode(scanner)==Mode.RECALL){
            message(user,"RECALL: double right-click a carrying Golem team, "
                    +"not a block, to safely set its pallet down and return.");
            return InteractionResult.SUCCESS;
        }
        if(mode(scanner)==Mode.REMOTE){
            // Remote mode ONLY accepts a physical, sealed 2x2 ground pallet.
            // It never enlists a local team or begins direct pickup/delivery.
            if(server.getBlockState(clicked).getBlock()
                    instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock){
                var old=StorageGroundPalletTransfer.wrappedController(server,clicked);
                if(old==null){
                    message(user,"REMOTE: target must be an intact, glass-wrapped "
                            +"2x2 ground pallet. Wrap it before requesting a swap.");
                    return InteractionResult.SUCCESS;
                }
                String queued=queuePickup(scanner,old.getBlockPos());
                if(queued.startsWith("REMOTE: queued")){
                    String result=dispatchPending(scanner,server,user,true);
                    message(user,result.isEmpty()?queued:result);
                }else message(user,queued);
                return InteractionResult.SUCCESS;
            }
            message(user,"REMOTE mode only requests a swap of a glass-wrapped "
                    +"ground pallet. Shift-right-click AIR for DIRECT mode.");
            return InteractionResult.SUCCESS;
        }

        // DIRECT mode is strictly player-led:
        // (1) click a linked Golem Shelf to summon four workers
        // (2) click an installed or wrapped pallet to lift it
        // (3) click a rack or a ground placement face to deposit it.
        if(server.getBlockEntity(clicked) instanceof StorageGolemShelfEntity){
            onShelfClick(server,user,scanner,clicked);
            return InteractionResult.SUCCESS;
        }
        var groundState=server.getBlockState(clicked);
        if(groundState.getBlock()
                instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock){
            // A loaded carrier must not try to pick up a second pallet.
            // For delivery, click a different (ordinary) block's face.
            message(user,StoragePalletCrew.pickupGround(server,user,clicked));
            return InteractionResult.SUCCESS;
        }
        PtpBlockEntity point=PtpBlock.controller(server,clicked);
        if(point!=null){
            message(user,StoragePalletCrew.interactPtp(server,user,point));
            return InteractionResult.SUCCESS;
        }
        RackBlockEntity rack=RackBlock.controller(server,clicked);
        if(rack!=null){
            message(user,StoragePalletCrew.interactRack(server,user,rack,home));
            return InteractionResult.SUCCESS;
        }
        BlockPos placement=clicked.relative(context.getClickedFace());
        message(user,StoragePalletCrew.deliverGround(server,user,placement));
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer user){
            ItemStack scanner=player.getItemInHand(hand);
            BlockPos home=linkedComputer(scanner,server);
            if(player.isShiftKeyDown()){
                Mode next=toggleMode(scanner);
                WarehouseGolemDebug.actionBar(user,Component.literal(
                        "Scanner in "+next.name()+" mode. "
                        +(next==Mode.REMOTE
                            ?"Click a wrapped ground pallet to dispatch a remote swap."
                            :next==Mode.DIRECT?"Click a Golem Shelf for a crew, then a pallet and destination."
                            :next==Mode.FORKLIFT?"Select a pallet, then a destination."
                            :"Double-right-click a carrying Golem team to abort safely.")
                        +(home==null?" Link a computer first.":"")));
            }else if(mode(scanner)==Mode.FORKLIFT){
                // Retain the old controller's unobstructed 20-block rack
                // selection after removing that second handheld tool.
                var from=user.getEyePosition();
                var until=from.add(user.getViewVector(1.0f).scale(20.0));
                var hit=server.clip(new net.minecraft.world.level.ClipContext(from,until,
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,user));
                if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){
                    BlockPos at=hit.getBlockPos();
                    if(server.mayInteract(user,at))
                        message(user,forkliftTarget(server,user,scanner,at,
                                at.relative(hit.getDirection()),home));
                }else message(user,"FORKLIFT: select pallet or destination rack within 20 blocks.");
            }else{
                message(user,"Scanner: "+mode(scanner)+" | "
                        +(home==null?"NOT LINKED. Right-click a Warehouse Computer."
                            :"linked to "+home.toShortString())+". "
                        +StoragePalletCrew.status(server,user));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
