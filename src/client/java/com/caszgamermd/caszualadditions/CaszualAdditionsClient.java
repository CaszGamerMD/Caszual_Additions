package com.caszgamermd.caszualadditions;

import dev.casz.utils.CaszUtilsClient;
import net.fabricmc.api.ClientModInitializer;

public final class CaszualAdditionsClient implements ClientModInitializer {
    @Override public void onInitializeClient() { CaszUtilsClient.initialize(); }
}