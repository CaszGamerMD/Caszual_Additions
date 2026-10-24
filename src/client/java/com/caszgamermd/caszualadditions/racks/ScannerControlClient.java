package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Present server-vetted scanner settings on the handheld control display. */
public final class ScannerControlClient {
    private ScannerControlClient(){}
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(ScannerControlNet.Open.TYPE,
            (payload,context)->context.client().execute(()->{
                var minecraft=Minecraft.getInstance();
                minecraft.gui.setScreen(new ScannerScreen(payload.json()));
            }));
    }
}
