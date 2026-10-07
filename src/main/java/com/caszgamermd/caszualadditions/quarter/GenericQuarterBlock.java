package com.caszgamermd.caszualadditions.quarter;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class GenericQuarterBlock extends Block implements EntityBlock {
    public static final MapCodec<GenericQuarterBlock> CODEC=simpleCodec(GenericQuarterBlock::new);
    public static final BooleanProperty NWD=BooleanProperty.create("nwd"),NED=BooleanProperty.create("ned"),
        SWD=BooleanProperty.create("swd"),SED=BooleanProperty.create("sed"),
        NWU=BooleanProperty.create("nwu"),NEU=BooleanProperty.create("neu"),
        SWU=BooleanProperty.create("swu"),SEU=BooleanProperty.create("seu");
    public static final BooleanProperty[] CORNERS={NWD,NED,SWD,SED,NWU,NEU,SWU,SEU};

    public GenericQuarterBlock(BlockBehaviour.Properties properties){
        super(properties);
        var s=stateDefinition.any();
        for(var p:CORNERS)s=s.setValue(p,false);
        registerDefaultState(s);
    }

    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(NWD,NED,SWD,SED,NWU,NEU,SWU,SEU);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}

    private static Vec3 relative(BlockPlaceContext ctx){
        var p=ctx.getClickedPos();var h=ctx.getClickLocation();
        return new Vec3(h.x-p.getX(),h.y-p.getY(),h.z-p.getZ());
    }

    public static int cornerIndex(Vec3 r){
        boolean e=r.x>=.5,u=r.y>=.5,s=r.z>=.5;
        if(!u&&!s)return e?1:0;
        if(!u)return e?3:2;
        if(!s)return e?5:4;
        return e?7:6;
    }

    private static boolean allCorners(BlockState state){
        for(var p:CORNERS)if(!state.getValue(p))return false;
        return true;
    }

    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext ctx){
        if(!ctx.getItemInHand().is(QuarterBlocks.QUARTER_BLOCK_ITEM)||allCorners(state))return false;
        return !state.getValue(CORNERS[cornerIndex(relative(ctx))]);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        int index=cornerIndex(relative(ctx));
        var existing=ctx.getLevel().getBlockState(ctx.getClickedPos());
        if(existing.is(this))return existing.getValue(CORNERS[index])?null:existing.setValue(CORNERS[index],true);
        return defaultBlockState().setValue(CORNERS[index],true);
    }

    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!(level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity be))return;
        var source=QuarterBlocks.source(stack);
        for(int i=0;i<8;i++){
            if(state.getValue(CORNERS[i])&&be.material(i)==null){
                be.setMaterial(i,source);
                break;
            }
        }
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new GenericQuarterBlockEntity(pos,state);
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

    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        List<ItemStack> drops=new ArrayList<>();
        if(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GenericQuarterBlockEntity be){
            for(int i=0;i<8;i++)if(state.getValue(CORNERS[i])){
                var source=be.material(i);
                if(source!=null)drops.add(QuarterBlocks.textured(source,1));
            }
        }
        return drops;
    }

    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData){
        if(level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity be){
            for(int i=0;i<8;i++){
                var source=be.material(i);
                if(state.getValue(CORNERS[i])&&source!=null)return QuarterBlocks.textured(source,1);
            }
        }
        return QuarterBlocks.textured(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),1);
    }
}
