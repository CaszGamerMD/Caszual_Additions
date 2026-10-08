package com.caszgamermd.caszualadditions;

import com.caszgamermd.caszualadditions.plushie.PlayerPlushieBlockEntityRenderer;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.quarter.GenericQuarterBlockEntityRenderer;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import dev.casz.utils.CaszUtilsClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class CaszualAdditionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CaszUtilsClient.initialize();
        BlockEntityRenderers.register(QuarterBlocks.QUARTER_BLOCK_ENTITY, GenericQuarterBlockEntityRenderer::new);
        BlockEntityRenderers.register(PlayerPlushies.PLAYER_PLUSHIE_ENTITY, PlayerPlushieBlockEntityRenderer::new);
    }
}
