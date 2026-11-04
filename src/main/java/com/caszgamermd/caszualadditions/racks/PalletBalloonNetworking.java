package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative input validation for the balloon pilot. */
public final class PalletBalloonNetworking {
    private PalletBalloonNetworking(){}
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(
                PalletBalloonPilotPayload.TYPE,PalletBalloonPilotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PalletBalloonPilotPayload.TYPE,
                (payload,context)->context.server().execute(()->{
                    ServerPlayer player=context.player();
                    if(!(player.getVehicle() instanceof PalletBalloon balloon)
                            ||balloon.getFirstPassenger()!=player)return;
                    balloon.setServerPilotInput(payload.forward(),payload.turn(),payload.lift());
                }));
    }
}
