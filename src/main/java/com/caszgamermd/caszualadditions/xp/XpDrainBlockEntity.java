package com.caszgamermd.caszualadditions.xp;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class XpDrainBlockEntity extends BlockEntity {
 public XpDrainBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.DRAIN,p,s);}
 public static void tick(ServerLevel level,BlockPos pos,XpDrainBlockEntity drain){
  if(XpNetwork.space(level,pos)<=0)return;
  var box=new AABB(pos).inflate(.75,.5,.75);
  for(Player p:level.getEntitiesOfClass(Player.class,box)){
   if(!p.isCrouching())continue;int available=XpPlayer.total(p);if(available<=0)continue;int moved=XpNetwork.insert(level,pos,Math.min(available,20));
   if(moved>0)XpPlayer.remove(p,moved);if(XpNetwork.space(level,pos)<=0)return;
  }
  for(ExperienceOrb orb:level.getEntitiesOfClass(ExperienceOrb.class,box)){
   int value=orb.getValue();int moved=XpNetwork.insert(level,pos,value);if(moved==value)orb.discard();
   else if(moved>0){orb.discard();level.addFreshEntity(new ExperienceOrb(level,orb.getX(),orb.getY(),orb.getZ(),value-moved));}
   if(XpNetwork.space(level,pos)<=0)return;
  }
  for(ItemEntity e:level.getEntitiesOfClass(ItemEntity.class,box)){
   var stack=e.getItem();if(!stack.is(Items.EXPERIENCE_BOTTLE))continue;
   while(!stack.isEmpty()&&XpNetwork.space(level,pos)>=7){int moved=XpNetwork.insert(level,pos,7);if(moved<7)break;stack.shrink(1);}
   if(stack.isEmpty())e.discard();else e.setItem(stack);if(XpNetwork.space(level,pos)<=0)return;
  }
 }
}
