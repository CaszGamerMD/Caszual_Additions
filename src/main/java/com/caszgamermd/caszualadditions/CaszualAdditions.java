package com.caszgamermd.caszualadditions;

import com.caszgamermd.caszualadditions.rods.ColorfulRods;
import com.caszgamermd.caszualadditions.headvending.HeadVendingContent;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.funbarrel.FunBarrelContent;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.caszgamermd.caszualadditions.unbreakable.UnbreakableContent;
import com.caszgamermd.caszualadditions.xp.XpBlockEntities;
import com.caszgamermd.caszualadditions.xp.XpBlocks;
import com.caszgamermd.caszualadditions.utils.CaszUtils;
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
        HeadVendingContent.initialize();
        FunBarrelContent.initialize();

        CaszualItemGroups.register();
    }
}
