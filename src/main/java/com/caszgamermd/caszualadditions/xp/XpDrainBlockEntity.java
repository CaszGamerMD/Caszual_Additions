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
 /** Player XP uses a broad pad detector so crouch-walking over the thin
  * drain grate works even when the player is moving between block cells.
  * We still require horizontal overlap with the plate's footprint. */
 public static int transferPlayer(ServerLevel level,BlockPos pos,Player player){
  if(!player.isCrouching()||player.isSpectator())return 0;
  if(player.getBoundingBox().maxY<pos.getY()||player.getBoundingBox().minY>pos.getY()+1.6)return 0;
  if(player.getBoundingBox().maxX<=pos.getX()+.125
          ||player.getBoundingBox().minX>=pos.getX()+.875
          ||player.getBoundingBox().maxZ<=pos.getZ()+.125
          ||player.getBoundingBox().minZ>=pos.getZ()+.875)return 0;
  // Take only points that actually exist in the visible XP level/bar.
  // A transfer is bounded by the network's remaining capacity to avoid
  // negative balances even when several players stand on the same grate.
  int available=XpPlayer.total(player);
  int requested=Math.min(40,Math.min(available,XpNetwork.space(level,pos)));
  if(requested<=0)return 0;
  int removed=XpPlayer.remove(player,requested);
  if(removed<=0)return 0;
  int stored=XpNetwork.insert(level,pos,removed);
  // Defensive refund if another mod changes storage while this executes:
  // never destroy XP or deposit XP that the player did not lose.
  if(stored<removed)XpPlayer.setTotal(player,
          (int)Math.min(Integer.MAX_VALUE,(long)XpPlayer.total(player)+removed-stored));
  return stored;
 }
 public static void tick(ServerLevel level,BlockPos pos,XpDrainBlockEntity drain){
  if(XpNetwork.space(level,pos)<=0)return;
  var box=new AABB(pos).inflate(.75,.5,.75);
  var playerPad=new AABB(pos.getX()-.25,pos.getY()-.25,pos.getZ()-.25,
          pos.getX()+1.25,pos.getY()+1.75,pos.getZ()+1.25);
  for(Player p:level.getEntitiesOfClass(Player.class,playerPad)){
   transferPlayer(level,pos,p);
   if(XpNetwork.space(level,pos)<=0)return;
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
