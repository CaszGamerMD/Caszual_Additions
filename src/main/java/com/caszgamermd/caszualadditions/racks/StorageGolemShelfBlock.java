package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
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
    private final MapCodec<StorageGolemShelfBlock> codec=MapCodec.unit(this);
    public StorageGolemShelfBlock(BlockBehaviour.Properties props){super(props);}
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
        if(!held.is(RackContent.STORAGE_GOLEM_ITEM))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof StorageGolemShelfEntity be){
            if(be.add()){
                if(!player.getAbilities().instabuild)held.shrink(1);
                player.sendSystemMessage(Component.literal("Storage Golems docked: "+be.docked()+"/2"));
            }else player.sendSystemMessage(Component.literal("Storage Golem Shelf is full (2/2)."));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof StorageGolemShelfEntity be){
            if(player.isShiftKeyDown()){
                if(be.take())player.getInventory().placeItemBackInInventory(
                        new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
            }else{
                int linked=StorageGolemShelfEntity.linked(level,pos).stream()
                        .mapToInt(StorageGolemShelfEntity::docked).sum();
                player.sendSystemMessage(Component.literal("Storage Golem Shelf: "
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
            be.clearDocked();
            for(int i=0;i<count;i++)
                Block.popResource(level,pos,new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
            Block.popResource(level,pos,new ItemStack(this));
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){
        return List.of();
    }
}
