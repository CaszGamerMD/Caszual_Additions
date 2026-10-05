package com.caszgamermd.caszualadditions;

import net.fabricmc.api.ModInitializer;

public final class CaszualAdditions implements ModInitializer {
    public static final String MOD_ID = "caszual_additions";

    @Override
    public void onInitialize() {
        CaszualItemGroups.register();
    }
}
