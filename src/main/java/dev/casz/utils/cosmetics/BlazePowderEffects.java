package dev.casz.utils.cosmetics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
public final class BlazePowderEffects {
 private BlazePowderEffects(){}
 public static void initialize(){
  ServerTickEvents.END_SERVER_TICK.register(server->{
   for(var level:server.getAllLevels())for(var p:level.players()){
    var head=Cosmetics.get(p,EquipmentSlot.HEAD);
    if(head.is(Items.BLAZE_POWDER)&&p.tickCount%2==0){
     level.sendParticles(ParticleTypes.FLAME,p.getX(),p.getY()+p.getBbHeight()-.18,p.getZ(),2,.22,.16,.22,.006);
     if(p.tickCount%6==0)level.sendParticles(ParticleTypes.SMALL_FLAME,p.getX(),p.getY()+p.getBbHeight()-.05,p.getZ(),2,.28,.12,.28,.01);
    }
    var feet=Cosmetics.get(p,EquipmentSlot.FEET);
    if(feet.is(Items.BLAZE_POWDER)&&p.getDeltaMovement().horizontalDistanceSqr()>.0025&&p.tickCount%2==0){
     level.sendParticles(ParticleTypes.SMALL_FLAME,p.getX(),p.getY()+.08,p.getZ(),2,.20,.06,.20,.015);
     if(p.tickCount%6==0)level.sendParticles(ParticleTypes.LAVA,p.getX(),p.getY()+.08,p.getZ(),1,.12,.03,.12,.005);
    }
   }
  });
 }
}
