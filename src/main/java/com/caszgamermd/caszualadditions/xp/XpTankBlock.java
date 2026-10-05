package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class XpTankBlock extends Block implements EntityBlock {
 public static final MapCodec<XpTankBlock> CODEC=simpleCodec(XpTankBlock::new);
 public static final IntegerProperty FILL=IntegerProperty.create("fill",0,10);
 public XpTankBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FILL,0));}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FILL);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new XpTankBlockEntity(pos,state);}
}
