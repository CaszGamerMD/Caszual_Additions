package com.caszgamermd.caszualadditions.pallet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
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

        state.root = blockEntity.isRoot();
        state.facing = blockEntity.getBlockState().getValue(PalletBlock.FACING);
        state.legacy = blockEntity.getBlockState().getValue(PalletBlock.LEGACY);
        if (state.root) {
            Block defaultMaterial = materialFor(blockEntity);
            boolean mixedWood = blockEntity.getBlockState().getBlock() instanceof PalletBlock pallet
                    && pallet.kind() == PalletKind.WOOD;
            boolean plastic = blockEntity.getBlockState().getBlock() instanceof PalletBlock pallet
                    && pallet.kind() == PalletKind.PLASTIC;

            for (int i = 0; i < 4; i++) {
                Block material = mixedWood ? blockEntity.woodForBoard(i) : defaultMaterial;
                blockModelResolver.update(state.boards[i], material.defaultBlockState(), DISPLAY_CONTEXT);
                state.boards[i].tintLayers().clear();
                if (plastic) state.boards[i].tintLayers().add(blockEntity.plasticColor());
            }
            blockModelResolver.update(state.carton,
                    PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(), DISPLAY_CONTEXT);
        }
        state.visibleCount = 0;

        // Slots are independent from display positions. Pack the first visible
        // nonempty slots into the limited display grid, then stop; remaining
        // stacks stay in storage and are not submitted to the renderer.
        if (state.root) {
            for (int slot = 0;
                    slot < blockEntity.getContainerSize()
                            && state.visibleCount < PalletBlockEntityRenderState.DISPLAY_CAPACITY;
                    slot++) {
                ItemStack stack = blockEntity.getItem(slot);
                if (stack.isEmpty()) continue;

                int displayIndex = state.visibleCount++;
                if (stack.getItem() instanceof BlockItem blockItem) {
                    state.boxed[displayIndex] = false;
                    // A block item in FIXED display context is transformed like
                    // an inventory object, with an off-center pivot. That made
                    // cargo float and spread apart. Real block models instead
                    // occupy exactly the half-block cargo cell.
                    blockModelResolver.update(state.cargoBlocks[displayIndex],
                            blockItem.getBlock().defaultBlockState(), DISPLAY_CONTEXT);
                    state.items[displayIndex].clear();
                } else {
                    state.boxed[displayIndex] = true;
                    itemModelResolver.updateForTopItem(
                            state.items[displayIndex], stack, ItemDisplayContext.GUI,
                            blockEntity.getLevel(), null, slot);
                }
            }
        }

        // Prevent stale objects remaining visible when slots are emptied or
        // this block entity is no longer the root of the 2x2 pallet.
        for (int i = state.visibleCount; i < state.items.length; i++) {
            state.items[i].clear();
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
        // Render ONE continuous 2×2 pallet from the controller. The three
        // satellite block entities never submit overlapping quarter-pallets.
        if (!state.root) return;
        if (!state.legacy) {
            poseStack.pushPose();
            // Existing board/cargo geometry is a 2x2 square extending in
            // local +X/+Z. Rotate its axes so +X points to the player's right
            // and the length of the pallet points forward from the home spot.
            // The home corner stays inside the block originally clicked.
            poseStack.translate(.5, 0, .5);
            float degrees = switch (state.facing) {
                case NORTH -> 0f;
                case EAST -> -90f;
                case SOUTH -> 180f;
                case WEST -> 90f;
                default -> 0f;
            };
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
            poseStack.translate(-.5, 0, -.5);
            poseStack.translate(0, 0, -1);
        }

        submitPalletBase(state, poseStack, collector);

        int layerSize = PalletBlockEntityRenderState.DISPLAY_COLUMNS
                * PalletBlockEntityRenderState.DISPLAY_ROWS;
        for (int i = 0; i < state.visibleCount; i++) {
            int layer = i / layerSize;
            int local = i % layerSize;
            int col = local % PalletBlockEntityRenderState.DISPLAY_COLUMNS;
            int row = local / PalletBlockEntityRenderState.DISPLAY_COLUMNS;

            // Four 8-pixel cubes plus three 1-pixel gaps need 35 pixels
            // across a 32-pixel pallet. A tiny 1.5px overhang on each side
            // allows cargo to fill the pallet instead of leaving empty edges.
            // Each layer begins on top of the deck and fills left-to-right,
            // then front-to-back; removing stacks compacts every higher tier.
            double x = -0.09375 + col * 0.5625;
            double z = -0.09375 + row * 0.5625;
            double y = PalletBlockEntityRenderState.FIRST_LAYER_Y
                    + layer * PalletBlockEntityRenderState.LAYER_SPACING;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(.5f, .5f, .5f);
            if (state.boxed[i]) {
                state.carton.submit(poseStack, collector, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
                // Stamp a small item icon onto the front of each carton, not
                // a floating full-sized inventory model around the carton.
                poseStack.pushPose();
                poseStack.translate(.5f, .5f, -0.016f);
                poseStack.scale(.44f, .44f, .44f);
                state.items[i].submit(poseStack, collector, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            } else {
                state.cargoBlocks[i].submit(poseStack, collector, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
        if (!state.legacy) poseStack.popPose();
    }

    private static void submitPalletBase(
            PalletBlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        // Four 2-block-long deck boards. Each top plank's timber derives
        // from one of the recipe's four corner ingredients.
        for (int i = 0; i < 4; i++) {
            float x = .04f + i * .49f;
            submitBox(state, i, poseStack, collector, x, .19f, .015f, .43f, .105f, 1.97f);
        }

        // Transverse lower runners, using the same four craft selections.
        // Each runner continues across all four footprint blocks, not one
        // tiny runner per quadrant. The underside colors can differ from
        // the visible deck-board colors.
        submitBox(state, 2, poseStack, collector,
                .035f, .035f, .12f, 1.93f, .15f, .23f);
        submitBox(state, 3, poseStack, collector,
                .035f, .035f, .88f, 1.93f, .15f, .23f);
        submitBox(state, 0, poseStack, collector,
                .035f, .035f, 1.65f, 1.93f, .15f, .23f);
    }

    private static void submitBox(
            PalletBlockEntityRenderState state,
            int board,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x, float y, float z,
            float sx, float sy, float sz
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(sx, sy, sz);
        state.boards[board].submit(poseStack, collector,
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
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
