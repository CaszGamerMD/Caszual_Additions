package dev.casz.utils.cosmetics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
public final class FootstepEffects {
 private FootstepEffects(){}
 public static void initialize(){ServerTickEvents.END_SERVER_TICK.register(server->{for(var level:server.getAllLevels())for(var p:level.players()){
  if(p.tickCount%3!=0||p.getDeltaMovement().horizontalDistanceSqr()<.0025)continue;
  var feet=Cosmetics.get(p,EquipmentSlot.FEET);
  if(feet.is(Items.ECHO_SHARD))level.sendParticles(ParticleTypes.SCULK_SOUL,p.getX(),p.getY()+.08,p.getZ(),1,.18,.04,.18,.01);
  else if(feet.is(Items.SLIME_BALL))level.sendParticles(ParticleTypes.ITEM_SLIME,p.getX(),p.getY()+.08,p.getZ(),2,.18,.04,.18,.02);
  else if(feet.is(Items.GUNPOWDER))level.sendParticles(ParticleTypes.SMOKE,p.getX(),p.getY()+.08,p.getZ(),2,.16,.04,.16,.01);
 }});}
}
