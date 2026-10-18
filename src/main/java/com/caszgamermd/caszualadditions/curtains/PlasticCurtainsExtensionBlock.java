package com.caszgamermd.caszualadditions.curtains;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Single-block extensions for PVC curtains. An old two-high curtain remains
 * the anchored lower pair; each additional use adds one freely selectable
 * upper section. Only the column's highest segment carries the steel header.
 */
public final class PlasticCurtainsExtensionBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty TOP_RAIL=BooleanProperty.create("top_rail");

    public PlasticCurtainsExtensionBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING,Direction.NORTH).setValue(TOP_RAIL,true));
    }
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING,TOP_RAIL);
    }
    private static boolean supports(BlockState below,Direction facing){
        if(below.getBlock() instanceof PlasticCurtainsExtensionBlock)
            return below.getValue(FACING)==facing;
        return below.getBlock() instanceof PlasticCurtainsBlock
                &&below.getValue(DoorBlock.FACING)==facing
                &&below.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER;
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,
            ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos neighborPos,
            BlockState neighbor,RandomSource random){
        if(side==Direction.DOWN&&!supports(neighbor,state.getValue(FACING)))
            return Blocks.AIR.defaultBlockState();
        if(side==Direction.UP)
            return state.setValue(TOP_RAIL,!(neighbor.getBlock() instanceof PlasticCurtainsExtensionBlock
                    &&neighbor.getValue(FACING)==state.getValue(FACING)));
        return state;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return PlasticCurtainsBlock.sheet(state);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        if(context instanceof EntityCollisionContext entityContext){
            var entity=entityContext.getEntity();
            if(entity!=null&&!PlasticCurtainsBlock.hostile(entity))
                return Shapes.empty();
        }
        return PlasticCurtainsBlock.sheet(state);
    }
    @Override protected VoxelShape getVisualShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return PlasticCurtainsBlock.sheet(state);
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(held.is(PlasticCurtainsContent.ITEM))
            return PlasticCurtainsBlock.addHeight(world,pos,player,held);
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        return InteractionResult.PASS;
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        // Each manually added section costs and returns one curtain item.
        return List.of(new ItemStack(PlasticCurtainsContent.ITEM));
    }
}
