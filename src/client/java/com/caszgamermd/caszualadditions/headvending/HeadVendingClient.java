package com.caszgamermd.caszualadditions.headvending;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class HeadVendingClient {
    private HeadVendingClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                HeadVendingContent.Open.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(
                                new HeadVendingScreen(payload.pos())
                        )
                )
        );
    }
}
