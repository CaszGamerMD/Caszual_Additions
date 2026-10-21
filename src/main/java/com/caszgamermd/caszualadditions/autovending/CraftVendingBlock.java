package com.caszgamermd.caszualadditions.autovending;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Shift-right-click = cabinet and recipe setup; right-click = dispense.
 * All actual crafting and ingredient withdrawal occur on the server.
 */
public final class CraftVendingBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CraftVendingBlock> CODEC=simpleCodec(CraftVendingBlock::new);

    public CraftVendingBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> state){
        state.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new CraftVendingBlockEntity(pos,state);
    }
    private static InteractionResult interact(Level world,BlockPos pos,Player player){
        if(world.isClientSide())return InteractionResult.SUCCESS;
        if(!(world instanceof ServerLevel level)
                ||!(level.getBlockEntity(pos) instanceof CraftVendingBlockEntity machine))
            return InteractionResult.PASS;
        if(player.isShiftKeyDown()){
            if(player instanceof ServerPlayer server)server.openMenu(machine);
        }else{
            Component result=machine.vend(level,player);
            player.sendSystemMessage(result);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return interact(level,pos,player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        return interact(level,pos,player);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()&&!player.getAbilities().instabuild
                &&level.getBlockEntity(pos) instanceof CraftVendingBlockEntity machine)
            Containers.dropContents(level,pos,machine);
        return super.playerWillDestroy(level,pos,state,player);
    }
}
