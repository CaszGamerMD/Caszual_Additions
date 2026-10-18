package com.caszgamermd.caszualadditions;

import com.caszgamermd.caszualadditions.headvending.HeadVendingClient;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntityRenderer;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.racks.RackContent;
import com.caszgamermd.caszualadditions.racks.RackBlockEntityRenderer;
import com.caszgamermd.caszualadditions.racks.PtpRenderer;
import com.caszgamermd.caszualadditions.racks.RackComputerClient;
import com.caszgamermd.caszualadditions.racks.StorageGolemRenderer;
import com.caszgamermd.caszualadditions.racks.StorageGolemShelfRenderer;
import com.caszgamermd.caszualadditions.racks.StorageGolemModel;
import com.caszgamermd.caszualadditions.racks.MovingPalletRenderer;
import com.caszgamermd.caszualadditions.racks.MovingPalletModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import com.caszgamermd.caszualadditions.pallet.PalletScreen;
import com.caszgamermd.caszualadditions.funbarrel.FunBarrelContent;
import com.caszgamermd.caszualadditions.funbarrel.FunBarrelScreen;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushieBlockEntityRenderer;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushieSpecialRenderer;
import com.caszgamermd.caszualadditions.plushie.PlayerPlushies;
import com.caszgamermd.caszualadditions.quarter.GenericQuarterBlockEntityRenderer;
import com.caszgamermd.caszualadditions.quarter.QuarterBlockSpecialRenderer;
import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.caszgamermd.caszualadditions.utils.CaszUtilsClient;
import com.caszgamermd.caszualadditions.utils.mixin.SpecialModelRenderersAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class CaszualAdditionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialModelRenderersAccessor.caszual_additions$getIdMapper().put(
                CaszualAdditions.id("quarter_block"),
                QuarterBlockSpecialRenderer.Unbaked.MAP_CODEC
        );
        SpecialModelRenderersAccessor.caszual_additions$getIdMapper().put(
                CaszualAdditions.id("player_plushie"),
                PlayerPlushieSpecialRenderer.Unbaked.MAP_CODEC
        );

        CaszUtilsClient.initialize();
        HeadVendingClient.initialize();
        RackComputerClient.initialize();
        com.caszgamermd.caszualadditions.transport.TransitComputerClient.initialize();
        EntityRenderers.register(com.caszgamermd.caszualadditions.transport.TransportContent.VEHICLE,com.caszgamermd.caszualadditions.transport.TransportRenderer::new);
        ModelLayerRegistry.registerModelLayer(StorageGolemRenderer.LAYER,StorageGolemModel::layer);
        EntityRenderers.register(RackContent.STORAGE_GOLEM_TYPE,StorageGolemRenderer::new);
        ModelLayerRegistry.registerModelLayer(MovingPalletRenderer.LAYER,MovingPalletModel::layer);
        EntityRenderers.register(RackContent.MOVING_PALLET_TYPE,MovingPalletRenderer::new);

        BlockEntityRenderers.register(
                QuarterBlocks.QUARTER_BLOCK_ENTITY,
                GenericQuarterBlockEntityRenderer::new
        );
        BlockEntityRenderers.register(
                PlayerPlushies.PLAYER_PLUSHIE_ENTITY,
                PlayerPlushieBlockEntityRenderer::new
        );
        BlockEntityRenderers.register(
                PalletContent.PALLET_BLOCK_ENTITY,
                PalletBlockEntityRenderer::new
        );
        BlockEntityRenderers.register(RackContent.RACK_ENTITY,RackBlockEntityRenderer::new);
        BlockEntityRenderers.register(RackContent.PTP_ENTITY,PtpRenderer::new);
        BlockEntityRenderers.register(RackContent.STORAGE_SHELF_ENTITY,StorageGolemShelfRenderer::new);

        MenuScreens.register(PalletContent.PALLET_MENU, PalletScreen::new);
        MenuScreens.register(FunBarrelContent.MENU, FunBarrelScreen::new);
    }
}
