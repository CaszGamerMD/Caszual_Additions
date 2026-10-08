package com.caszgamermd.caszualadditions;

import com.casz.colorfulrods.ColorfulRods;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.caszgamermd.caszualadditions.unbreakable.UnbreakableContent;
import com.caszgamermd.caszualadditions.xp.XpBlockEntities;
import com.caszgamermd.caszualadditions.xp.XpBlocks;
import dev.casz.utils.CaszUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class CaszualAdditions implements ModInitializer {
    public static final String MOD_ID = "caszual_additions";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CaszUtils.initialize();

        QuarterBlocks.initialize();
        ColorfulRods.initialize();

        XpBlocks.initialize();
        XpBlockEntities.initialize();
        UnbreakableContent.initialize();
        PlayerPlushies.initialize();
        PalletContent.initialize();

        CaszualItemGroups.register();
    }
}
