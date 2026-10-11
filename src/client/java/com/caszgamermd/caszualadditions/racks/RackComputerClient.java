package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class RackComputerClient {
    private RackComputerClient() {}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(RackComputerNet.Open.TYPE,
                (payload,context)->context.client().execute(()->
                        Minecraft.getInstance().gui.setScreen(new RackComputerScreen(payload.computer()))));
        ClientPlayNetworking.registerGlobalReceiver(RackComputerNet.OrderReply.TYPE,
                (payload,context)->context.client().execute(()->{
                    if(Minecraft.getInstance().gui.screen() instanceof RackComputerScreen screen)
                        screen.acceptOrderReply(payload.message());
                }));
        ClientPlayNetworking.registerGlobalReceiver(RackComputerNet.Snapshot.TYPE,
                (payload,context)->context.client().execute(()->{
                    if(Minecraft.getInstance().gui.screen() instanceof RackComputerScreen screen)
                        screen.accept(payload.computer(),payload.json());
                }));
    }
}
