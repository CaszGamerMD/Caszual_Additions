package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class XpChargerBlock extends Block implements EntityBlock {
 public static final MapCodec<XpChargerBlock> CODEC=simpleCodec(XpChargerBlock::new);
 public XpChargerBlock(BlockBehaviour.Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new XpChargerBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){if(level.isClientSide())return null;return type==XpBlockEntities.CHARGER?(l,p,s,be)->XpRepair.tick((net.minecraft.server.level.ServerLevel)l,(XpChargerBlockEntity)be):null;}
}
