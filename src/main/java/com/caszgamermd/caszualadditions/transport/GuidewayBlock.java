package com.caszgamermd.caszualadditions.transport;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Nine occupied cells per module. No overhanging, non-colliding side artwork. */
public final class GuidewayBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,8);
    public final String kind;
    private boolean removing;
    public GuidewayBlock(String kind,Properties properties){super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,4));}
    @Override protected MapCodec<? extends Block> codec(){return MapCodec.unit(this);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
    public static BlockPos cell(BlockPos root,Direction facing,int part){return root.relative(facing.getClockWise(),part%3-1).relative(facing,1-part/3);}
    public static BlockPos root(BlockPos pos,BlockState s){return pos.relative(s.getValue(FACING).getClockWise(),1-s.getValue(PART)%3).relative(s.getValue(FACING),s.getValue(PART)/3-1);}
    public boolean rail(){return kind.startsWith("pallet_rail");}
    public boolean bay(){return kind.equals("drone_parking_bay");}
    public int rise(){return kind.endsWith("_up")?1:kind.endsWith("_down")?-1:0;}
    public Direction exit(Direction entry){return kind.endsWith("_left")?entry.getCounterClockWise():kind.endsWith("_right")?entry.getClockWise():entry;}
    public static boolean complete(Level l,BlockPos p){
        BlockState s=l.getBlockState(p);
        if(!(s.getBlock() instanceof GuidewayBlock)||s.getValue(PART)!=4)return false;
        for(int i=0;i<9;i++){
            BlockPos c=cell(p,s.getValue(FACING),i);
            if(!l.hasChunkAt(c))return false;
            BlockState a=l.getBlockState(c);
            if(a.getBlock()!=s.getBlock()||a.getValue(PART)!=i||a.getValue(FACING)!=s.getValue(FACING))return false;
        }return true;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        Direction d=c.getHorizontalDirection();BlockPos p=c.getClickedPos();
        for(int i=0;i<9;i++){
            BlockPos q=cell(p,d,i);
            if(!c.getLevel().hasChunkAt(q)||c.getLevel().isOutsideBuildHeight(q)||!c.getLevel().getWorldBorder().isWithinBounds(q)||!c.getLevel().getBlockState(q).canBeReplaced())return null;
        }return defaultBlockState().setValue(FACING,d);
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){
        super.setPlacedBy(l,p,s,e,stack);
        if(!l.isClientSide())for(int i=0;i<9;i++)if(i!=4)l.setBlock(cell(p,s.getValue(FACING),i),s.setValue(PART,i),3);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){
        if(!removing){removing=true;try{
            BlockPos r=root(p,s);
            for(int i=0;i<9;i++){BlockPos q=cell(r,s.getValue(FACING),i);if(l.getBlockState(q).is(this))l.removeBlock(q,false);}
        }finally{removing=false;}}
        super.affectNeighborsAfterRemoval(s,l,p,moved);
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder loot){return List.of(new ItemStack(this));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        // Five small treads approximate the continuous 1:3 ramp surface.
        if(rise()==0)return Block.box(0,0,0,16,4,16);
        VoxelShape shape=Shapes.empty();
        int row=s.getValue(PART)/3;
        for(int i=0;i<8;i++){
            double h=4+rise()*((2-row)*16+(8-i)*2)/3.0;
            double z=i*2; Direction d=s.getValue(FACING);
            VoxelShape piece=switch(d){
                case NORTH->Block.box(0,Math.min(0,h-2),z,16,Math.max(1,h),z+2);
                case SOUTH->Block.box(0,Math.min(0,h-2),14-z,16,Math.max(1,h),16-z);
                case EAST->Block.box(14-z,Math.min(0,h-2),0,16-z,Math.max(1,h),16);
                default->Block.box(z,Math.min(0,h-2),0,z+2,Math.max(1,h),16);
            };shape=Shapes.or(shape,piece);
        }return shape;
    }
}
