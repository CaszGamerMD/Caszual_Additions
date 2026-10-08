package com.caszgamermd.caszualadditions;

import com.caszgamermd.caszualadditions.plushie.PlayerPlushieBlockEntityRenderer;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushieSpecialRenderer;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.quarter.GenericQuarterBlockEntityRenderer;
import com.caszgamermd.caszualadditions.quarter.QuarterBlockSpecialRenderer;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import dev.casz.utils.CaszUtilsClient;
import dev.casz.utils.mixin.SpecialModelRenderersAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class CaszualAdditionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialModelRenderersAccessor.caszutils$getIdMapper().put(
                CaszualAdditions.id("quarter_block"),
                QuarterBlockSpecialRenderer.Unbaked.MAP_CODEC
        );
        SpecialModelRenderersAccessor.caszutils$getIdMapper().put(
                CaszualAdditions.id("player_plushie"),
                PlayerPlushieSpecialRenderer.Unbaked.MAP_CODEC
        );

        CaszUtilsClient.initialize();
        BlockEntityRenderers.register(QuarterBlocks.QUARTER_BLOCK_ENTITY, GenericQuarterBlockEntityRenderer::new);
        BlockEntityRenderers.register(PlayerPlushies.PLAYER_PLUSHIE_ENTITY, PlayerPlushieBlockEntityRenderer::new);
    }
}
