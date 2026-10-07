package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbVerticalStairsBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RgbVerticalStairsBlock> CODEC=simpleCodec(RgbVerticalStairsBlock::new);
    private static final VoxelShape NORTH=Shapes.or(box(0,0,0,16,16,8),box(0,0,8,16,8,16));
    private static final VoxelShape SOUTH=Shapes.or(box(0,0,8,16,16,16),box(0,0,0,16,8,8));
    private static final VoxelShape WEST=Shapes.or(box(0,0,0,8,16,16),box(8,0,0,16,8,16));
    private static final VoxelShape EAST=Shapes.or(box(8,0,0,16,16,16),box(0,0,0,8,8,16));
    public RgbVerticalStairsBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        return switch(s.getValue(FACING)){case NORTH->NORTH;case SOUTH->SOUTH;case WEST->WEST;default->EAST;};
    }
}
