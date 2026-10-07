package com.caszgamermd.caszualadditions;

import dev.casz.utils.CaszUtils;
import com.casz.colorfulrods.ColorfulRods;
import net.fabricmc.api.ModInitializer;
import com.caszgamermd.caszualadditions.xp.XpBlocks;
import com.caszgamermd.caszualadditions.xp.XpBlockEntities;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import net.minecraft.resources.Identifier;

public final class CaszualAdditions implements ModInitializer {
    public static final String MOD_ID = "caszual_additions";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CaszUtils.initialize();
        ColorfulRods.initialize();
        XpBlocks.initialize();
        XpBlockEntities.initialize();
        QuarterBlocks.initialize();
        CaszualItemGroups.register();
    }
}