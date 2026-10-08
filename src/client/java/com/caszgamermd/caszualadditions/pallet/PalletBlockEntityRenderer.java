package com.caszgamermd.caszualadditions.pallet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PalletBlockEntityRenderer
        implements BlockEntityRenderer<PalletBlockEntity, PalletBlockEntityRenderState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;

    public PalletBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public PalletBlockEntityRenderState createRenderState() {
        return new PalletBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
            PalletBlockEntity blockEntity,
            PalletBlockEntityRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        Block material = materialFor(blockEntity);
        blockModelResolver.update(state.base, material.defaultBlockState(), DISPLAY_CONTEXT);

        if (blockEntity.getBlockState().getBlock() instanceof PalletBlock pallet
                && pallet.kind() == PalletKind.PLASTIC) {
            state.base.tintLayers().clear();
            state.base.tintLayers().add(blockEntity.plasticColor());
        }

        state.root = blockEntity.isRoot();
        for (int i = 0; i < state.items.length; i++) {
            ItemStack stack = state.root ? blockEntity.getItem(i) : ItemStack.EMPTY;
            state.occupied[i] = !stack.isEmpty();

            if (state.occupied[i]) {
                itemModelResolver.updateForTopItem(
                        state.items[i],
                        stack,
                        ItemDisplayContext.GROUND,
                        blockEntity.getLevel(),
                        null,
                        i
                );
            } else {
                state.items[i].clear();
            }
        }
    }

    private static Block materialFor(PalletBlockEntity blockEntity) {
        if (!(blockEntity.getBlockState().getBlock() instanceof PalletBlock pallet)) {
            return Blocks.OAK_PLANKS;
        }

        return switch (pallet.kind()) {
            case WOOD -> blockEntity.woodForPart();
            case PLASTIC -> PalletContent.PALLET_RENDER_PROXY;
            case IRON -> Blocks.IRON_BLOCK;
            case COPPER -> PalletContent.copperTextureBlock(pallet.copperAge());
        };
    }

    @Override
    public void submit(
            PalletBlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        submitPalletBase(state, poseStack, collector);

        if (!state.root) return;

        for (int i = 0; i < state.items.length; i++) {
            if (!state.occupied[i]) continue;

            int layer = i / 108;
            int local = i % 108;
            int col = local % 12;
            int row = local / 12;

            double x = (col + 0.5) * (2.0 / 12.0);
            double z = (row + 0.5) * (2.0 / 9.0);
            double y = 0.31 + layer * 0.12;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees((i * 37) % 360));
            poseStack.scale(0.17F, 0.17F, 0.17F);
            state.items[i].submit(
                    poseStack,
                    collector,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
            poseStack.popPose();
        }
    }

    private static void submitPalletBase(
            PalletBlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        submitBox(state, poseStack, collector, 0.02F, 0.12F, 0.02F, 0.30F, 0.12F, 0.96F);
        submitBox(state, poseStack, collector, 0.35F, 0.12F, 0.02F, 0.30F, 0.12F, 0.96F);
        submitBox(state, poseStack, collector, 0.68F, 0.12F, 0.02F, 0.30F, 0.12F, 0.96F);

        submitBox(state, poseStack, collector, 0.11F, 0.02F, 0.08F, 0.18F, 0.10F, 0.84F);
        submitBox(state, poseStack, collector, 0.71F, 0.02F, 0.08F, 0.18F, 0.10F, 0.84F);
    }

    private static void submitBox(
            PalletBlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x,
            float y,
            float z,
            float sx,
            float sy,
            float sz
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(sx, sy, sz);
        state.base.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
