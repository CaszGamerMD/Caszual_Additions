package com.caszgamermd.caszualadditions.antiwarden.mixin;

import com.caszgamermd.caszualadditions.antiwarden.AntiWardenContent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent the effect from ever being accepted; no client-side darkness flash. */
@Mixin(LivingEntity.class)
public abstract class WoolHelmetEffectMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at=@At("HEAD"),cancellable=true)
    private void caszual_additions$woolHelmet(MobEffectInstance effect,
            Entity source,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player player
                &&player.getItemBySlot(EquipmentSlot.HEAD).is(AntiWardenContent.WOOL_HELMET)
                &&(effect.getEffect().is(MobEffects.DARKNESS)
                        ||effect.getEffect().is(MobEffects.BLINDNESS))){
            cir.setReturnValue(false);
        }
    }
}
