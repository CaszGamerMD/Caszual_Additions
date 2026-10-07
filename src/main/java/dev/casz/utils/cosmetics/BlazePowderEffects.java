package dev.casz.utils.cosmetics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public final class BlazePowderEffects {
 private BlazePowderEffects(){}
 public static void initialize(){
  ServerTickEvents.END_SERVER_TICK.register(server->{
   for(var level:server.getAllLevels())for(var p:level.players()){
    var head=Cosmetics.get(p,EquipmentSlot.HEAD);
    if(head.is(Items.BLAZE_POWDER)&&p.tickCount%2==0){
     double y=p.getY()+p.getBbHeight()-.38;
     level.sendParticles(ParticleTypes.FLAME,p.getX(),y,p.getZ(),5,.29,.24,.29,.012);
     level.sendParticles(ParticleTypes.SMALL_FLAME,p.getX(),y+.20,p.getZ(),3,.24,.16,.24,.018);
     if(p.tickCount%8==0)level.sendParticles(ParticleTypes.SMOKE,p.getX(),y+.28,p.getZ(),2,.20,.08,.20,.01);
    }
    if(BuiltInRegistries.ITEM.getKey(head.getItem()).getPath().equals("lightning_rod")&&p.getRandom().nextInt(180)==0){
     double y=p.getY()+p.getBbHeight()+.16;
     level.sendParticles(ParticleTypes.ELECTRIC_SPARK,p.getX(),y,p.getZ(),7,.24,.28,.24,.12);
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
