package com.caszgamermd.caszualadditions.antiwarden.mixin;

import com.caszgamermd.caszualadditions.antiwarden.WardenTrapSilence;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/** Only Warden / sculk-trigger SOUND EVENTS around an ACTIVE snare are muted.
 * Gameplay sound elsewhere remains audible. This is separate from vibration
 * receivers, which the wool-boots listener mixin also suppresses. */
@Mixin(ServerLevel.class)
public abstract class WardenTrapSoundMixin {
    private boolean caszual$mute(Holder<SoundEvent> sound,Vec3 position){
        String id=BuiltInRegistries.SOUND_EVENT.getKey(sound.value()).toString();
        return WardenTrapSilence.sculkOrWardenSound(id)
                &&WardenTrapSilence.near((ServerLevel)(Object)this,position);
    }
    @Inject(method="playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at=@At("HEAD"),cancellable=true,require=0)
    private void caszual$mutePosition(@Nullable Entity except,double x,double y,
            double z,Holder<SoundEvent> sound,SoundSource source,float volume,
            float pitch,long seed,CallbackInfo ci){
        if(caszual$mute(sound,new Vec3(x,y,z)))ci.cancel();
    }
    @Inject(method="playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at=@At("HEAD"),cancellable=true,require=0)
    private void caszual$muteEntity(@Nullable Entity except,Entity emitter,
            Holder<SoundEvent> sound,SoundSource source,float volume,
            float pitch,long seed,CallbackInfo ci){
        if(caszual$mute(sound,emitter.position()))ci.cancel();
    }
}
