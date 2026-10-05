package com.caszgamermd.caszualadditions.xp;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
public final class XpShowerBlockEntity extends BlockEntity {
 private UUID playerId;
 public XpShowerBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.SHOWER,p,s);}
 public void toggle(ServerPlayer player){if(player.getUUID().equals(playerId)){playerId=null;setChanged();return;}playerId=player.getUUID();setChanged();}
 public static void tick(ServerLevel level,BlockPos pos,XpShowerBlockEntity shower){
  if(shower.playerId==null)return;ServerPlayer p=level.getServer().getPlayerList().getPlayer(shower.playerId);
  if(p==null||!p.isAlive()||p.level()!=level||p.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)>16.0){shower.playerId=null;shower.setChanged();return;}
  int moved=XpNetwork.extract(level,pos,1);if(moved<=0){shower.playerId=null;shower.setChanged();return;}p.giveExperiencePoints(moved);if(level.getGameTime()%2==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,pos.getX()+0.5,pos.getY()+0.25,pos.getZ()+0.5,2,.18,.05,.18,.02);
 }
}
