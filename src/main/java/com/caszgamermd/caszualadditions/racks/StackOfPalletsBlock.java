package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** One floor block holding twelve visible pallet variants; item data survives placement. */
public final class StackOfPalletsBlock extends Block implements EntityBlock {
    public static final IntegerProperty HEIGHT=IntegerProperty.create("height",0,4);
    private final MapCodec<StackOfPalletsBlock> codec=MapCodec.unit(this);
    public StackOfPalletsBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEIGHT,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(HEIGHT);
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new StackOfPalletsBlockEntity(pos,state);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity be)
            be.loadMagazine(stack);
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!StackOfPalletsItem.isPallet(held))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity magazine){
            if(magazine.count()>=12){
                player.sendSystemMessage(Component.literal("Pallet stack full (12/12)."));
            }else if(magazine.put(held.copyWithCount(1))){
                if(!player.getAbilities().instabuild)held.shrink(1);
                player.sendSystemMessage(Component.literal("Stored pallet ("+magazine.count()+"/12)."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity magazine){
            if(player.isShiftKeyDown()){
                ItemStack next=magazine.take();
                if(!next.isEmpty())player.getInventory().placeItemBackInInventory(next);
                player.sendSystemMessage(Component.literal("Pallet stack: "+magazine.count()+"/12 remaining."));
            }else if(player instanceof ServerPlayer server){
                server.openMenu(magazine);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity be)
            Block.popResource(level,pos,be.asItem());
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        // The whole magazine drops exactly once, with all twelve contents.
        return List.of();
    }
}
