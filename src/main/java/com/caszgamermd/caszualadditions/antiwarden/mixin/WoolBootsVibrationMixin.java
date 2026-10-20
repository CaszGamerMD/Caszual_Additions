package com.caszgamermd.caszualadditions.antiwarden.mixin;

import com.caszgamermd.caszualadditions.antiwarden.AntiWardenContent;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only sculk/warden receivers within seven blocks of the wearer go quiet.
 * Other players and out-of-radius listeners continue working normally. */
@Mixin(VibrationSystem.Listener.class)
public abstract class WoolBootsVibrationMixin {
    @Shadow public abstract PositionSource getListenerSource();
    private boolean caszual_additions$silent(ServerLevel level,
            GameEvent.Context context){
        // A triggered Warden Snare has a 9-block acoustic dead zone.
        // Sensors and shriekers INSIDE it must not schedule vibrations,
        // regardless of who or what emitted the game event.
        if(getListenerSource().getPosition(level)
                .map(position->com.caszgamermd.caszualadditions.antiwarden
                        .WardenTrapSilence.near(level,position))
                .orElse(false))return true;
        if(!(context.sourceEntity() instanceof Player player)
                ||!player.getItemBySlot(EquipmentSlot.FEET).is(AntiWardenContent.WOOL_BOOTS))
            return false;
        return getListenerSource().getPosition(level)
                .map(position->position.distanceToSqr(player.position())<=49.0)
                .orElse(false);
    }
    @Inject(method="handleGameEvent",at=@At("HEAD"),cancellable=true)
    private void caszual_additions$stopVibration(ServerLevel level,
            Holder<GameEvent> event,GameEvent.Context context,Vec3 source,
            CallbackInfoReturnable<Boolean> cir){
        if(caszual_additions$silent(level,context))cir.setReturnValue(false);
    }
    @Inject(method="forceScheduleVibration",at=@At("HEAD"),cancellable=true)
    private void caszual_additions$stopDirectSculkActivation(ServerLevel level,
            Holder<GameEvent> event,GameEvent.Context context,Vec3 source,
            CallbackInfo ci){
        if(caszual_additions$silent(level,context))ci.cancel();
    }
}
