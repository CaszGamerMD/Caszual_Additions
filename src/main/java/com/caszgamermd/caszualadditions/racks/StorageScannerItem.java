package com.caszgamermd.caszualadditions.racks;

import java.util.List;
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
        // First link the handheld tool to the clicked computer. Enlisting
        // four workers is a separate operation with its own requirements.
        bind(scanner,level,computer);
        List<StorageGolemShelfEntity> shelves=RackComputerNet.nearbyShelves(level,computer);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        player.sendSystemMessage(Component.literal("Scanner linked to warehouse at "
                +computer.toShortString()+". Linked shelves: "+shelves.size()
                +", docked Storage Golems: "+docked+"/4 required."));
        String result=StoragePalletCrew.enlist(level,player,computer);
        player.sendSystemMessage(Component.literal(result));
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
            user.sendSystemMessage(Component.literal(StoragePalletCrew.deliverRack(server,user,rack)));
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
            user.sendSystemMessage(Component.literal(
                    (home==null?"Scanner not linked.":"Warehouse at "+home.toShortString()+". ")
                            +StoragePalletCrew.status(server,user)));
        }
        return InteractionResult.SUCCESS;
    }
}
