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
        if(data==null)return "";
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
                    if(status.startsWith("Four Golems dispatched")
                            ||status.startsWith("INVALID:")){
                        clearPickup(scanner);
                        player.sendSystemMessage(Component.literal(status));
                    }
                }
            }
        });
    }

    /** Called from BOTH the computer block interaction and item useOn.
     * Linking a scanner must never depend on already having four workers. */
    public static void onComputerClick(ServerLevel level,ServerPlayer player,
            ItemStack scanner,BlockPos computer,boolean audit){
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                computer.getZ()+.5)>64
                ||!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))return;
        if(audit){
            for(String row:WarehouseDiagnostics.audit(level,computer))
                player.sendSystemMessage(Component.literal(row));
            return;
        }
        BlockPos home=linkedComputer(scanner,level);
        boolean sameWarehouse=computer.equals(home);
        String current=StoragePalletCrew.status(level,player);
        if(sameWarehouse&&!current.startsWith("No linked crew")){
            player.sendSystemMessage(Component.literal(
                    StoragePalletCrew.dismiss(level,player,computer)));
            return;
        }
        // Keep an existing four-Golem assignment bound to its own
        // warehouse until the crew is dismissed there.
        if(!sameWarehouse&&!current.startsWith("No linked crew")){
            if(StoragePalletCrew.assignedToComputer(level,player,computer)){
                bind(scanner,level,computer);
                player.sendSystemMessage(Component.literal(
                        "Scanner linked to your already-deployed warehouse Golem crew. "
                        +"Right-click a sealed PTP pallet to start pickup."));
            }else player.sendSystemMessage(Component.literal(
                    "This crew belongs to another warehouse. Return and dismiss it first."));
            return;
        }
        if(sameWarehouse){
            // A second normal click is an explicit request for a MANUAL crew.
            // Never spend all four docked workers merely to link the Scanner:
            // remote pickup needs those same four workers at their shelves.
            player.sendSystemMessage(Component.literal(
                    StoragePalletCrew.enlist(level,player,computer)));
            return;
        }
        bind(scanner,level,computer);
        List<StorageGolemShelfEntity> shelves=RackComputerNet.nearbyShelves(level,computer);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        player.sendSystemMessage(Component.literal("Scanner linked to warehouse at "
                +computer.toShortString()+". Linked shelves: "+shelves.size()
                +", docked Storage Golems: "+docked+"/4 required. "
                +"Golems remain docked for remote pickup: sneak-right-click a "
                +"glass-wrapped ground pallet to request an automatic exchange. "
                +"Right-click this Computer again ONLY for manual hauling."));
    }

    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        Level level=context.getLevel();
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer user))return InteractionResult.PASS;
        BlockPos clicked=context.getClickedPos();
        ItemStack scanner=context.getItemInHand();
        BlockPos home=linkedComputer(scanner,server);

        if(level.getBlockState(clicked).is(RackContent.RACK_COMPUTER)){
            onComputerClick(server,user,scanner,clicked,player.isShiftKeyDown());
            return InteractionResult.SUCCESS;
        }
        // The linked warehouse may be in an unloaded chunk while the
        // player and their real team carry a pallet to a distant location.
        if(home==null){
            user.sendSystemMessage(Component.literal(
                    "Link scanner first: right-click your warehouse Rack Computer."));
            return InteractionResult.SUCCESS;
        }
        if(player.isShiftKeyDown()
                &&server.getBlockState(clicked).getBlock()
                    instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock){
            var old=StorageGroundPalletTransfer.wrappedController(server,clicked);
            if(old==null){
                user.sendSystemMessage(Component.literal(
                        "Remote pickup needs an intact, glass-wrapped 2x2 ground pallet. "
                        +"Wrap it with the portable wrapper before requesting pickup."));
                return InteractionResult.SUCCESS;
            }
            queuePickup(scanner,old.getBlockPos());
            String result=pendingPickup(scanner,server,user);
            if(result.startsWith("Four Golems dispatched")){
                clearPickup(scanner);
                user.sendSystemMessage(Component.literal(result));
            }else user.sendSystemMessage(Component.literal(
                    "Remote pickup queued on Scanner: "+result
                    +" Keep the warehouse and route loaded for physical transport; "
                    +"the queued job will retry without losing its target."));
            return InteractionResult.SUCCESS;
        }
        if(server.getBlockState(clicked).getBlock()
                instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock){
            user.sendSystemMessage(Component.literal(
                    StoragePalletCrew.pickupGround(server,user,clicked)));
            return InteractionResult.SUCCESS;
        }
        PtpBlockEntity point=PtpBlock.controller(server,clicked);
        if(point!=null){
            user.sendSystemMessage(Component.literal(StoragePalletCrew.pickup(server,user,point)));
            return InteractionResult.SUCCESS;
        }
        RackBlockEntity rack=RackBlock.controller(server,clicked);
        if(rack!=null){
            user.sendSystemMessage(Component.literal(StoragePalletCrew.interactRack(server,user,rack)));
            return InteractionResult.SUCCESS;
        }
        // Place on the block face the player clicked; only a 2x2 clear,
        // replaceable horizontal footprint is accepted by the leader.
        BlockPos placement=clicked.relative(context.getClickedFace());
        user.sendSystemMessage(Component.literal(
                StoragePalletCrew.deliverGround(server,user,placement)));
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer user){
            ItemStack stack=player.getItemInHand(hand);
            BlockPos home=linkedComputer(stack,server);
            if(home!=null&&player.isShiftKeyDown()){
                user.sendSystemMessage(Component.literal(
                        StoragePalletCrew.returnLoadedCrew(server,user,home)));
            }else{
                user.sendSystemMessage(Component.literal(
                        (home==null?"Scanner not linked.":"Warehouse at "+home.toShortString()+". ")
                                +StoragePalletCrew.status(server,user)
                                +(home==null?"":" Sneak-use in air to return your loaded crew.")));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
