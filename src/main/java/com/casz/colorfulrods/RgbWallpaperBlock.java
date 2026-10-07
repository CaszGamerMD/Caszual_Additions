package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbWallpaperBlock extends Block {
    public static final MapCodec<RgbWallpaperBlock> CODEC=simpleCodec(RgbWallpaperBlock::new);
    public static final BooleanProperty NORTH=BooleanProperty.create("north");
    public static final BooleanProperty SOUTH=BooleanProperty.create("south");
    public static final BooleanProperty WEST=BooleanProperty.create("west");
    public static final BooleanProperty EAST=BooleanProperty.create("east");

    private static final VoxelShape NORTH_SHAPE=box(0,0,0,16,16,1);
    private static final VoxelShape SOUTH_SHAPE=box(0,0,15,16,16,16);
    private static final VoxelShape WEST_SHAPE=box(0,0,0,1,16,16);
    private static final VoxelShape EAST_SHAPE=box(15,0,0,16,16,16);

    public RgbWallpaperBlock(BlockBehaviour.Properties p){
        super(p);
        registerDefaultState(stateDefinition.any().setValue(NORTH,false).setValue(SOUTH,false).setValue(WEST,false).setValue(EAST,false));
    }

    @Override protected MapCodec<? extends Block> codec(){return CODEC;}

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(NORTH,SOUTH,WEST,EAST);
    }

    private static BooleanProperty property(Direction d){
        return switch(d){
            case NORTH->NORTH;
            case SOUTH->SOUTH;
            case WEST->WEST;
            case EAST->EAST;
            default->null;
        };
    }

    private static BooleanProperty target(BlockPlaceContext ctx){
        var face=ctx.getClickedFace();
        if(!face.getAxis().isHorizontal()) return null;
        return property(face.getOpposite());
    }

    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext ctx){
        if(!ctx.getItemInHand().is(asItem())) return false;
        var p=target(ctx);
        return p!=null&&!state.getValue(p);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var p=target(ctx);
        if(p==null) return null;
        var existing=ctx.getLevel().getBlockState(ctx.getClickedPos());
        if(existing.is(this)) return existing.getValue(p)?null:existing.setValue(p,true);
        return defaultBlockState().setValue(p,true);
    }

    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        VoxelShape shape=Shapes.empty();
        if(s.getValue(NORTH)) shape=Shapes.or(shape,NORTH_SHAPE);
        if(s.getValue(SOUTH)) shape=Shapes.or(shape,SOUTH_SHAPE);
        if(s.getValue(WEST)) shape=Shapes.or(shape,WEST_SHAPE);
        if(s.getValue(EAST)) shape=Shapes.or(shape,EAST_SHAPE);
        return shape;
    }
}
