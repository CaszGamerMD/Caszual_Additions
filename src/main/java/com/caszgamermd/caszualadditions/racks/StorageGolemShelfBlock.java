package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** One rack-adjacent shelf stores two Storage Golems, links to neighbors. */
public final class StorageGolemShelfBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<StorageGolemShelfBlock> codec=MapCodec.unit(this);
    public StorageGolemShelfBlock(BlockBehaviour.Properties props){
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new StorageGolemShelfEntity(pos,state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide())return null;
        return (l,p,s,be)->{
            if(be instanceof StorageGolemShelfEntity shelf &&
                    l instanceof net.minecraft.server.level.ServerLevel server)
                StorageGolemShelfEntity.tick(server,shelf);
        };
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(held.is(RackContent.STORAGE_SCANNER_ITEM)){
            if(level instanceof net.minecraft.server.level.ServerLevel server
                    &&player instanceof net.minecraft.server.level.ServerPlayer user)
                StorageScannerItem.onShelfClick(server,user,held,pos);
            return InteractionResult.SUCCESS;
        }
        if(!held.is(RackContent.STORAGE_GOLEM_ITEM))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof StorageGolemShelfEntity be){
            if(be.addNamed(WarehouseGolemNames.fromToken(held))){
                if(!player.getAbilities().instabuild)held.shrink(1);
                WarehouseGolemDebug.message(player,pos,Component.literal(
                        "Golem "+be.docked()+"/2 docked: "+be.nextName()));
            }else WarehouseGolemDebug.message(player,pos,Component.literal(
                    "Storage Golem Shelf is full (2/2)."));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof StorageGolemShelfEntity be){
            if(player.isShiftKeyDown()){
                String name=be.takeNamed();
                if(name!=null)player.getInventory().placeItemBackInInventory(
                        WarehouseGolemNames.namedToken(name));
            }else{
                int linked=StorageGolemShelfEntity.linked(level,pos).stream()
                        .mapToInt(StorageGolemShelfEntity::docked).sum();
                WarehouseGolemDebug.message(player,pos,Component.literal("Storage Golem Shelf: "
                        +be.docked()+"/2 docked | "+linked+" available across linked shelves."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof StorageGolemShelfEntity be
                &&!player.getAbilities().instabuild){
            int count=be.docked();
            for(int i=0;i<count;i++){
                String name=be.takeNamed();
                Block.popResource(level,pos,WarehouseGolemNames.namedToken(name));
            }
            Block.popResource(level,pos,new ItemStack(this));
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){
        return List.of();
    }
}
