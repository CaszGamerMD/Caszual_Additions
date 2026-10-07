package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbVerticalStairsBlock extends HorizontalDirectionalBlock {
    public enum Side implements StringRepresentable {
        LEFT, RIGHT;
        @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }

    public static final MapCodec<RgbVerticalStairsBlock> CODEC=simpleCodec(RgbVerticalStairsBlock::new);
    public static final EnumProperty<Side> SIDE=EnumProperty.create("side",Side.class);

    // A normal stair rotated 90 degrees around its forward axis.
    private static final VoxelShape NORTH_LEFT=Shapes.or(box(0,0,0,8,16,16),box(8,0,8,16,16,16));
    private static final VoxelShape NORTH_RIGHT=Shapes.or(box(8,0,0,16,16,16),box(0,0,8,8,16,16));
    private static final VoxelShape SOUTH_LEFT=Shapes.or(box(8,0,0,16,16,16),box(0,0,0,8,16,8));
    private static final VoxelShape SOUTH_RIGHT=Shapes.or(box(0,0,0,8,16,16),box(8,0,0,16,16,8));
    private static final VoxelShape EAST_LEFT=Shapes.or(box(0,0,0,16,16,8),box(8,0,8,16,16,16));
    private static final VoxelShape EAST_RIGHT=Shapes.or(box(0,0,8,16,16,16),box(8,0,0,16,16,8));
    private static final VoxelShape WEST_LEFT=Shapes.or(box(0,0,8,16,16,16),box(0,0,0,8,16,8));
    private static final VoxelShape WEST_RIGHT=Shapes.or(box(0,0,0,16,16,8),box(0,0,8,8,16,16));

    public RgbVerticalStairsBlock(BlockBehaviour.Properties p){
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(SIDE,Side.LEFT));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){
        b.add(FACING,SIDE);
    }

    private static Side sideFor(Direction facing,double x,double z){
        return switch(facing){
            case NORTH -> x<.5?Side.LEFT:Side.RIGHT;
            case SOUTH -> x>.5?Side.LEFT:Side.RIGHT;
            case EAST -> z<.5?Side.LEFT:Side.RIGHT;
            case WEST -> z>.5?Side.LEFT:Side.RIGHT;
            default -> Side.LEFT;
        };
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var face=ctx.getClickedFace();
        var facing=face.getAxis().isHorizontal()?face.getOpposite():ctx.getHorizontalDirection().getOpposite();
        var hit=ctx.getClickLocation();
        double x=hit.x-Math.floor(hit.x),z=hit.z-Math.floor(hit.z);
        return defaultBlockState().setValue(FACING,facing).setValue(SIDE,sideFor(facing,x,z));
    }

    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        var left=s.getValue(SIDE)==Side.LEFT;
        return switch(s.getValue(FACING)){
            case NORTH -> left?NORTH_LEFT:NORTH_RIGHT;
            case SOUTH -> left?SOUTH_LEFT:SOUTH_RIGHT;
            case EAST -> left?EAST_LEFT:EAST_RIGHT;
            case WEST -> left?WEST_LEFT:WEST_RIGHT;
            default -> NORTH_LEFT;
        };
    }
}
