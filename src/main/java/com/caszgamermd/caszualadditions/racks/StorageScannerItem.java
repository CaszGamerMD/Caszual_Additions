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

    private static BlockPos linkedComputer(ItemStack scanner,ServerLevel world){
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

    /** A single saved mode belongs to the SCANNER ItemStack, so switching
     * modes cannot silently dispatch four warehouse workers later. */
    public enum Mode { REMOTE, DIRECT }
    public static Mode mode(ItemStack scanner){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return Mode.REMOTE;
        String name=data.copyTag().getStringOr("scanner_mode","REMOTE");
        return "DIRECT".equals(name)?Mode.DIRECT:Mode.REMOTE;
    }
    private static Mode toggleMode(ItemStack scanner){
        Mode next=mode(scanner)==Mode.REMOTE?Mode.DIRECT:Mode.REMOTE;
        CustomData existing=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=existing==null?new CompoundTag():existing.copyTag();
        tag.putString("scanner_mode",next.name());
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        if(next==Mode.DIRECT)clearPickup(scanner);
        return next;
    }
    private static boolean dispatched(String response){
        return response.startsWith("Three Golems dispatched")
                ||response.startsWith("Four Golems dispatched");
    }
    private static void message(ServerPlayer player,String text){
        player.sendSystemMessage(Component.literal(text));
    }

    /** Persist remote pickup requests on the linked scanner item. */
    private static void queuePickup(ItemStack scanner,BlockPos target){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=data==null?new CompoundTag():data.copyTag();
        tag.putBoolean("remote_pickup_pending",true);
        tag.putInt("remote_pickup_x",target.getX());
        tag.putInt("remote_pickup_y",target.getY());
        tag.putInt("remote_pickup_z",target.getZ());
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static void clearPickup(ItemStack scanner){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null)return;
        CompoundTag tag=data.copyTag();
        tag.remove("remote_pickup_pending");
        tag.remove("remote_pickup_x");
        tag.remove("remote_pickup_y");
        tag.remove("remote_pickup_z");
        scanner.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static String pendingPickup(ItemStack scanner,ServerLevel world,
            ServerPlayer player){
        CustomData data=scanner.get(DataComponents.CUSTOM_DATA);
        if(data==null||mode(scanner)!=Mode.REMOTE)return "";
        CompoundTag tag=data.copyTag();
        if(!tag.getBooleanOr("remote_pickup_pending",false))return "";
        BlockPos computer=linkedComputer(scanner,world);
        if(computer==null)return "INVALID: scanner link lost.";
        WarehouseRemoteTickets.keep(world,computer);
        BlockPos target=new BlockPos(tag.getIntOr("remote_pickup_x",0),
                tag.getIntOr("remote_pickup_y",0),tag.getIntOr("remote_pickup_z",0));
        return RemotePalletDispatch.request(world,player,computer,target);
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
                    String status=pendingPickup(scanner,level,player);
                    if(dispatched(status)||status.startsWith("INVALID:")){
                        clearPickup(scanner);
                        player.sendSystemMessage(Component.literal(status));
                    }
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
        if(previous!=null&&!previous.equals(computer)
                &&!StoragePalletCrew.status(level,player).startsWith("No linked crew")
                &&!StoragePalletCrew.assignedToComputer(level,player,computer)){
            message(player,"Current direct Golem crew is linked to another warehouse. "
                    +"Deliver its pallet and dismiss it before relinking.");
            return;
        }
        // Rebinding always cancels an older queued remote job to prevent
        // accidentally dispatching a different warehouse after switching.
        if(previous==null||!previous.equals(computer))clearPickup(scanner);
        bind(scanner,level,computer);
        java.util.List<StorageGolemShelfEntity> shelves=
                RackComputerNet.nearbyShelves(level,computer);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        message(player,"Scanner linked to Rack Computer "+computer.toShortString()
                +" | "+shelves.size()+" shelves, "+docked+" docked Golems"
                +" | "+mode(scanner)+" MODE. "
                +"Shift-right-click air to toggle Remote / Direct. "
                +"Diagnostics are in the Rack Computer GUI.");
    }

    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        Level level=context.getLevel();
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer user))return InteractionResult.PASS;
        BlockPos clicked=context.getClickedPos();
        ItemStack scanner=context.getItemInHand();
        // Right-clicking a computer without crouching opens the terminal
        // (handled by RackComputerBlock); only crouching alters the link.
        if(server.getBlockState(clicked).is(RackContent.RACK_COMPUTER)){
            if(player.isShiftKeyDown())onComputerClick(server,user,scanner,clicked);
            else net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    user,new RackComputerNet.Open(clicked));
            return InteractionResult.SUCCESS;
        }

        BlockPos home=linkedComputer(scanner,server);
        if(home==null){
            message(user,"Scanner not linked. Shift-right-click a Rack Computer to link.");
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
                queuePickup(scanner,old.getBlockPos());
                String result=pendingPickup(scanner,server,user);
                if(dispatched(result)||result.startsWith("INVALID:")){
                    clearPickup(scanner);
                    message(user,result);
                }else message(user,"REMOTE queued: "+result
                        +". Scanner retries without forgetting the target.");
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
            message(user,StoragePalletCrew.enlistFromShelf(server,user,home,clicked));
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
            message(user,StoragePalletCrew.interactRack(server,user,rack));
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
                message(user,"Scanner mode: "+next.name()+". "
                        +(next==Mode.REMOTE
                            ?"Click a wrapped ground pallet to dispatch a remote swap."
                            :"Click a linked Golem Shelf to call four workers, then "
                                +"click a pallet to pick up and a rack/block to place.")
                        +(home==null?" Link a computer first.":""));
            }else{
                message(user,"Scanner: "+mode(scanner)+" | "
                        +(home==null?"NOT LINKED. Shift-click a Rack Computer."
                            :"linked to "+home.toShortString())+". "
                        +StoragePalletCrew.status(server,user));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
