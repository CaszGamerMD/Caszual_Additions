package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbQuarterBlock extends Block {
    public static final MapCodec<RgbQuarterBlock> CODEC=simpleCodec(RgbQuarterBlock::new);
    public static final BooleanProperty NWD=BooleanProperty.create("nwd"), NED=BooleanProperty.create("ned"),
        SWD=BooleanProperty.create("swd"), SED=BooleanProperty.create("sed"),
        NWU=BooleanProperty.create("nwu"), NEU=BooleanProperty.create("neu"),
        SWU=BooleanProperty.create("swu"), SEU=BooleanProperty.create("seu");
    private static final BooleanProperty[] CORNERS={NWD,NED,SWD,SED,NWU,NEU,SWU,SEU};

    public RgbQuarterBlock(BlockBehaviour.Properties p){
        super(p);
        var s=stateDefinition.any();
        for(var prop:CORNERS)s=s.setValue(prop,false);
        registerDefaultState(s);
    }
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(NWD,NED,SWD,SED,NWU,NEU,SWU,SEU);}

    private static Vec3 relative(BlockPlaceContext ctx){
        var p=ctx.getClickedPos();var h=ctx.getClickLocation();
        return new Vec3(h.x-p.getX(),h.y-p.getY(),h.z-p.getZ());
    }
    private static BooleanProperty corner(Vec3 r){
        boolean e=r.x>=.5,u=r.y>=.5,s=r.z>=.5;
        if(!u&&!s)return e?NED:NWD;
        if(!u)return e?SED:SWD;
        if(!s)return e?NEU:NWU;
        return e?SEU:SWU;
    }
    private static boolean allCorners(BlockState state){for(var p:CORNERS)if(!state.getValue(p))return false;return true;}

    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext ctx){
        if(allCorners(state))return false;
        return !state.getValue(corner(relative(ctx)));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var existing=ctx.getLevel().getBlockState(ctx.getClickedPos());
        var target=corner(relative(ctx));
        if(existing.is(this))return existing.getValue(target)?null:existing.setValue(target,true);
        return defaultBlockState().setValue(target,true);
    }

    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        VoxelShape shape=Shapes.empty();
        if(s.getValue(NWD))shape=Shapes.or(shape,box(0,0,0,8,8,8));
        if(s.getValue(NED))shape=Shapes.or(shape,box(8,0,0,16,8,8));
        if(s.getValue(SWD))shape=Shapes.or(shape,box(0,0,8,8,8,16));
        if(s.getValue(SED))shape=Shapes.or(shape,box(8,0,8,16,8,16));
        if(s.getValue(NWU))shape=Shapes.or(shape,box(0,8,0,8,16,8));
        if(s.getValue(NEU))shape=Shapes.or(shape,box(8,8,0,16,16,8));
        if(s.getValue(SWU))shape=Shapes.or(shape,box(0,8,8,8,16,16));
        if(s.getValue(SEU))shape=Shapes.or(shape,box(8,8,8,16,16,16));
        return shape;
    }
}
