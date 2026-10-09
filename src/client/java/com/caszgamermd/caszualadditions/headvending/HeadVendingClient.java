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

        ClientPlayNetworking.registerGlobalReceiver(
                HeadVendingContent.CustomResults.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().screen instanceof HeadVendingScreen screen) {
                        screen.acceptCustomResults(payload.pos(), payload.json());
                    }
                })
        );
    }
}
