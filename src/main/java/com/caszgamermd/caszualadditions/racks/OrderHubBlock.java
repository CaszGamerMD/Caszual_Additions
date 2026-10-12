package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Retired 2x2 Hub block: readable only to recover legacy saved inventories. */
public final class OrderHubBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
    private final MapCodec<OrderHubBlock> codec=MapCodec.unit(this);
    public OrderHubBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART,0));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(PART);
    }
    public static BlockPos cell(BlockPos root,int part){
        return root.offset(part%2,0,part/2);
    }
    public static BlockPos root(BlockPos pos,BlockState state){
        int part=state.getValue(PART);
        return pos.offset(-(part%2),0,-(part/2));
    }
    public static @Nullable OrderHubEntity controller(Level level,BlockPos pos){
        BlockState state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof OrderHubBlock))return null;
        return level.getBlockEntity(root(pos,state)) instanceof OrderHubEntity hub?hub:null;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        BlockPos pos=context.getClickedPos();
        for(int i=0;i<4;i++){
            BlockPos at=cell(pos,i);
            if(context.getLevel().isOutsideBuildHeight(at)
                    ||!context.getLevel().getBlockState(at).canBeReplaced()
                    ||RackBlock.reservedForAccess(context.getLevel(),at))return null;
        }
        return defaultBlockState();
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        for(int i=1;i<4;i++)level.setBlock(cell(pos,i),state.setValue(PART,i),3);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return state.getValue(PART)==0?new OrderHubEntity(pos,state):null;
    }
    // Compatibility block only: saved Hub inventories are accessible, but
    // never tick old virtual collectors or accept new workers/pallets.
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){return null;}

    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,Level level,
            BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        // No new collector/pallet installation or legacy automation.
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()&&player instanceof ServerPlayer server){
            OrderHubEntity hub=controller(level,pos);
            if(hub!=null){
                if(server.isShiftKeyDown()){
                    if(hub.hasPallet()&&hub.isEmpty()){
                        server.getInventory().placeItemBackInInventory(hub.removePallet());
                    }else if(!hub.hasPallet() && hub.collectors()>0){
                        hub.removeCollector();
                        server.getInventory().placeItemBackInInventory(new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
                    }else server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Empty the output pallet before removing it."));
                }else if(hub.hasPallet())server.openMenu(hub);
                else server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Retired Hub: salvage its pallet and "+hub.collectors()
                                +" saved collectors, or break it in Survival for 9 transfer blocks."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide()){
            BlockPos root=root(pos,state);
            OrderHubEntity hub=level.getBlockEntity(root) instanceof OrderHubEntity e?e:null;
            if(hub!=null&&!player.getAbilities().instabuild){
                Containers.dropContents(level,root,hub);
                if(hub.hasPallet())Block.popResource(level,root,hub.dropPallet());
                for(int i=0;i<hub.collectors();i++)
                    Block.popResource(level,root,new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
                // Replace the retired 2x2 block with one complete 3x3 PTP.
                Block.popResource(level,root,new ItemStack(RackContent.PTP_TRANSFER_ITEM,9));
            }
            for(int i=0;i<4;i++){
                BlockPos at=cell(root,i);
                if(!at.equals(pos)&&level.getBlockState(at).is(this))level.removeBlock(at,false);
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return List.of();}
}
