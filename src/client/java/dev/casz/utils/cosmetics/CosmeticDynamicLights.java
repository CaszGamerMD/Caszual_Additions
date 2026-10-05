package dev.casz.utils.cosmetics;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
public final class CosmeticDynamicLights implements DynamicLightsInitializer {
    private static final RodLuminance RODS=new RodLuminance();
    private static final EntityLuminance.Type TYPE=EntityLuminance.Type.registerSimple(Identifier.fromNamespaceAndPath("caszutils","cosmetic_end_rods"),RODS);
    @Override public void onInitializeDynamicLights(DynamicLightsContext context){context.entityLightSourceManager().onRegisterEvent().register(registration->registration.register(EntityTypes.PLAYER,RODS));}
    private static final class RodLuminance implements EntityLuminance {
        @Override public Type type(){return TYPE;}
        @Override public int getLuminance(ItemLightSourceManager items,Entity entity){
            if(!(entity instanceof Player player)||player.isSpectator()) return 0;
            for(var slot:Cosmetics.SLOTS) if(SpecialCosmetics.isEndRod(Cosmetics.get(player,slot))) return 14;
            return 0;
        }
    }
}