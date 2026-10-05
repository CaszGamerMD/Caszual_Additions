package com.caszgamermd.caszualadditions;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public final class CaszualAdditions implements ModInitializer {
    public static final String MOD_ID = "caszual_additions";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CaszualItemGroups.register();
    }
}
