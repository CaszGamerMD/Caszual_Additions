package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
public final class XpDrainBlock extends Block implements EntityBlock {
 private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE=Block.box(0,0,0,16,1,16);
 public static final MapCodec<XpDrainBlock> CODEC=simpleCodec(XpDrainBlock::new);
 public XpDrainBlock(BlockBehaviour.Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return SHAPE;}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return SHAPE;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
         BlockPos pos,Player player,BlockHitResult hit){
  if(level instanceof net.minecraft.server.level.ServerLevel server){
   if(XpNetwork.capacity(server,pos)==0){
    ((net.minecraft.server.level.ServerPlayer)player).sendSystemMessage(Component.literal(
            "XP Drain: connect an XP Tank to any side to store XP."),true);
   }else{
    int moved=player.isCrouching()?
            XpDrainBlockEntity.transferPlayer(server,pos,player):0;
    String status=moved>0?"Deposited "+moved+" XP. "
            :player.isCrouching()&&XpNetwork.space(server,pos)==0?
                    "XP Tank network is full. ":"";
    ((net.minecraft.server.level.ServerPlayer)player).sendSystemMessage(Component.literal(status+"XP Drain: "+
            XpNetwork.stored(server,pos)+" / "+XpNetwork.capacity(server,pos)+" XP"),true);
   }
  }
  return InteractionResult.SUCCESS;
 }
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new XpDrainBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
  if(level.isClientSide())return null;return type==XpBlockEntities.DRAIN?(l,p,s,be)->XpDrainBlockEntity.tick((net.minecraft.server.level.ServerLevel)l,p,(XpDrainBlockEntity)be):null;
 }
}
