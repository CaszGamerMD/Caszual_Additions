package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
public final class XpTankBlock extends Block implements EntityBlock {
 public static final MapCodec<XpTankBlock> CODEC=simpleCodec(XpTankBlock::new);
 public XpTankBlock(BlockBehaviour.Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new XpTankBlockEntity(pos,state);}
}
