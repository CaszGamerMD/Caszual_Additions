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
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbVerticalCarpetBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RgbVerticalCarpetBlock> CODEC=simpleCodec(RgbVerticalCarpetBlock::new);
    private static final VoxelShape NORTH=box(0,0,0,16,16,1);
    private static final VoxelShape SOUTH=box(0,0,15,16,16,16);
    private static final VoxelShape WEST=box(0,0,0,1,16,16);
    private static final VoxelShape EAST=box(15,0,0,16,16,16);

    public RgbVerticalCarpetBlock(BlockBehaviour.Properties p){
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){
        b.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var face=ctx.getClickedFace();
        var facing=face.getAxis().isHorizontal()?face.getOpposite():ctx.getHorizontalDirection();
        return defaultBlockState().setValue(FACING,facing);
    }

    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        return switch(s.getValue(FACING)){
            case NORTH->NORTH;
            case SOUTH->SOUTH;
            case WEST->WEST;
            default->EAST;
        };
    }
}
