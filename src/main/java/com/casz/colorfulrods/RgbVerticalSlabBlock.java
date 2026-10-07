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

public final class RgbVerticalSlabBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RgbVerticalSlabBlock> CODEC=simpleCodec(RgbVerticalSlabBlock::new);
    private static final VoxelShape NORTH=box(0,0,0,16,16,8), SOUTH=box(0,0,8,16,16,16);
    private static final VoxelShape WEST=box(0,0,0,8,16,16), EAST=box(8,0,0,16,16,16);
    public RgbVerticalSlabBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FACING);}
    private static Direction topBottomFacing(BlockPlaceContext ctx){
        var h=ctx.getClickLocation();
        double x=h.x-Math.floor(h.x)-.5,z=h.z-Math.floor(h.z)-.5;
        if(Math.abs(x)>.16||Math.abs(z)>.16){
            if(Math.abs(x)>Math.abs(z))return x>0?Direction.EAST:Direction.WEST;
            return z>0?Direction.SOUTH:Direction.NORTH;
        }
        return ctx.getHorizontalDirection().getOpposite();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var face=ctx.getClickedFace();
        var facing=face.getAxis().isHorizontal()?face.getOpposite():topBottomFacing(ctx);
        return defaultBlockState().setValue(FACING,facing);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        return switch(s.getValue(FACING)){case NORTH->NORTH;case SOUTH->SOUTH;case WEST->WEST;default->EAST;};
    }
}
