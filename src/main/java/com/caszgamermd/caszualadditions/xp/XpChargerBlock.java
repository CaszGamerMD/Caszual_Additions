package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class XpChargerBlock extends Block implements EntityBlock {
 public static final MapCodec<XpChargerBlock> CODEC=simpleCodec(XpChargerBlock::new);
 public XpChargerBlock(BlockBehaviour.Properties p){super(p);}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new XpChargerBlockEntity(pos,state);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof XpChargerBlockEntity charger){
   if(!charger.item().isEmpty()){
    var out=charger.removeItem(0,1);if(!player.getInventory().add(out))player.drop(out,false);
   }
   if(level instanceof ServerLevel sl)status(player,sl,pos,charger);
  } return InteractionResult.SUCCESS;
 }
 @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit){
  if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof XpChargerBlockEntity charger){
   if(charger.item().isEmpty()&&XpRepair.hasMending(held)&&held.isDamaged()){var one=held.copyWithCount(1);charger.setItem(one);held.shrink(1);}
   if(level instanceof ServerLevel sl)status(player,sl,pos,charger);
  } return InteractionResult.SUCCESS;
 }
 private static void status(Player p,ServerLevel l,BlockPos pos,XpChargerBlockEntity c){
  int stored=XpNetwork.stored(l,pos),cap=XpNetwork.capacity(l,pos);
  String item=c.item().isEmpty()?"empty":c.item().getHoverName().getString()+(c.item().isDamaged()?" ("+c.item().getDamageValue()+" damage)":" (repaired)");
  if(p instanceof ServerPlayer sp)sp.sendSystemMessage(Component.literal("XP Charger: "+item+" | XP "+stored+" / "+cap),true);
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){if(level.isClientSide())return null;return type==XpBlockEntities.CHARGER?(l,p,s,be)->XpRepair.tick((ServerLevel)l,(XpChargerBlockEntity)be):null;}
}
