package com.caszgamermd.caszualadditions.quarter;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class GenericQuarterBlockEntityRenderer
        implements BlockEntityRenderer<GenericQuarterBlockEntity, GenericQuarterBlockEntityRenderState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public GenericQuarterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public GenericQuarterBlockEntityRenderState createRenderState() {
        return new GenericQuarterBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
            GenericQuarterBlockEntity blockEntity,
            GenericQuarterBlockEntityRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        var blockState = blockEntity.getBlockState();
        for (int i = 0; i < 8; i++) {
            var material = blockEntity.material(i);
            boolean occupied = blockState.getValue(GenericQuarterBlock.CORNERS[i]) && material != null;
            state.occupied[i] = occupied;
            if (occupied) {
                blockModelResolver.update(state.quarters[i], material, DISPLAY_CONTEXT);
            } else {
                state.quarters[i].clear();
            }
        }
    }

    @Override
    public void submit(
            GenericQuarterBlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        for (int i = 0; i < 8; i++) {
            if (!state.occupied[i]) continue;

            double x = (i & 1) != 0 ? 0.5 : 0.0;
            double y = i >= 4 ? 0.5 : 0.0;
            double z = (i & 2) != 0 ? 0.5 : 0.0;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            state.quarters[i].submit(
                    poseStack,
                    collector,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }
    }
}
