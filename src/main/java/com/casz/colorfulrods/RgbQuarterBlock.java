package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbQuarterBlock extends Block {
    public enum Center implements StringRepresentable {
        NONE, DOWN, UP, NORTH, SOUTH, WEST, EAST;
        @Override public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
        public static Center of(Direction d){return switch(d){case DOWN->DOWN;case UP->UP;case NORTH->NORTH;case SOUTH->SOUTH;case WEST->WEST;case EAST->EAST;};}
    }

    public static final MapCodec<RgbQuarterBlock> CODEC=simpleCodec(RgbQuarterBlock::new);
    public static final EnumProperty<Center> CENTER=EnumProperty.create("center",Center.class);
    public static final BooleanProperty NWD=BooleanProperty.create("nwd"), NED=BooleanProperty.create("ned"),
        SWD=BooleanProperty.create("swd"), SED=BooleanProperty.create("sed"),
        NWU=BooleanProperty.create("nwu"), NEU=BooleanProperty.create("neu"),
        SWU=BooleanProperty.create("swu"), SEU=BooleanProperty.create("seu");
    private static final BooleanProperty[] CORNERS={NWD,NED,SWD,SED,NWU,NEU,SWU,SEU};

    public RgbQuarterBlock(BlockBehaviour.Properties p){
        super(p);
        var s=stateDefinition.any().setValue(CENTER,Center.NONE);
        for(var prop:CORNERS)s=s.setValue(prop,false);
        registerDefaultState(s);
    }
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CENTER,NWD,NED,SWD,SED,NWU,NEU,SWU,SEU);}

    private static Vec3 relative(BlockPlaceContext ctx){
        var p=ctx.getClickedPos();var h=ctx.getClickLocation();
        return new Vec3(h.x-p.getX(),h.y-p.getY(),h.z-p.getZ());
    }
    private static boolean middle(double v){return v>=.25&&v<=.75;}
    private static boolean centerHit(Direction face,Vec3 r){
        return switch(face.getAxis()){
            case X -> middle(r.y)&&middle(r.z);
            case Y -> middle(r.x)&&middle(r.z);
            case Z -> middle(r.x)&&middle(r.y);
        };
    }
    private static BooleanProperty corner(Vec3 r){
        boolean e=r.x>=.5,u=r.y>=.5,s=r.z>=.5;
        if(!u&&!s)return e?NED:NWD;
        if(!u)return e?SED:SWD;
        if(!s)return e?NEU:NWU;
        return e?SEU:SWU;
    }
    private static boolean anyCorner(BlockState state){for(var p:CORNERS)if(state.getValue(p))return true;return false;}
    private static boolean allCorners(BlockState state){for(var p:CORNERS)if(!state.getValue(p))return false;return true;}

    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext ctx){
        if(state.getValue(CENTER)!=Center.NONE||allCorners(state))return false;
        return !state.getValue(corner(relative(ctx)));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var existing=ctx.getLevel().getBlockState(ctx.getClickedPos());
        var rel=relative(ctx);
        if(existing.is(this)){
            if(existing.getValue(CENTER)!=Center.NONE)return null;
            var target=corner(rel);
            return existing.getValue(target)?null:existing.setValue(target,true);
        }
        var state=defaultBlockState();
        if(centerHit(ctx.getClickedFace(),rel))return state.setValue(CENTER,Center.of(ctx.getClickedFace()));
        return state.setValue(corner(rel),true);
    }

    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        var center=s.getValue(CENTER);
        if(center!=Center.NONE)return switch(center){
            case DOWN->box(4,0,4,12,8,12); case UP->box(4,8,4,12,16,12);
            case NORTH->box(4,4,0,12,12,8); case SOUTH->box(4,4,8,12,12,16);
            case WEST->box(0,4,4,8,12,12); case EAST->box(8,4,4,16,12,12);
            default->Shapes.empty();
        };
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
