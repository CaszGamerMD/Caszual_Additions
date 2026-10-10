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
            // Respect occupied blocks above the deck: do not render cargo
            // through a roof, wall or an adjacent built structure.
            int maxVisible = blockEntity.getLevel() == null
                    ? PalletBlockEntityRenderState.DISPLAY_CAPACITY
                    : PalletCargoCollisionBlock.visibleCount(
                            blockEntity.getLevel(), blockEntity.getBlockPos(),
                            blockEntity.getBlockState(), blockEntity);
            for (int slot = 0;
                    slot < blockEntity.getContainerSize()
                            && state.visibleCount < maxVisible;
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
                    // Raw item models have complete geometry on all faces.
                    // Terrain block models may cull internal faces when used
                    // outside the world renderer and appear hollow.
                    itemModelResolver.updateForTopItem(
                            state.items[displayIndex], stack, ItemDisplayContext.NONE,
                            blockEntity.getLevel(), null, slot);
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
        // Only the home controller renders the whole 2x2 pallet.
        if (!state.root) return;

        // Never mirror a rendered mesh with a negative scale. Negative
        // determinant transforms flip face winding, causing exterior faces
        // to vanish and leaving hollow-looking blocks. Use a genuine Y
        // rotation for the wooden/metal deck. Cargo cell positions are
        // transformed separately so the clicked block remains the home corner.
        poseStack.pushPose();
        if (!state.legacy) {
            switch (state.facing) {
                case NORTH -> {
                    poseStack.translate(0, 0, 1);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                }
                case EAST -> {
                    // The home block is the south-west corner of this square.
                }
                case SOUTH -> {
                    poseStack.translate(1, 0, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case WEST -> {
                    poseStack.translate(1, 0, 1);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
                default -> {}
            }
        }
        submitPalletBase(state, poseStack, collector);
        poseStack.popPose();

        int layerSize = PalletBlockEntityRenderState.DISPLAY_COLUMNS
                * PalletBlockEntityRenderState.DISPLAY_ROWS;
        for (int i = 0; i < state.visibleCount; i++) {
            int layer = i / layerSize;
            int local = i % layerSize;
            int col = local % PalletBlockEntityRenderState.DISPLAY_COLUMNS;
            int row = local / PalletBlockEntityRenderState.DISPLAY_COLUMNS;

            // Place the cargo in world-relative cells rather than reflecting
            // item meshes. This retains correct face winding on all four
            // orientations and aligns exactly with cargo collision shapes.
            double cell = PalletBlockEntityRenderState.ITEM_SCALE;
            double x, z;
            if (state.legacy) {
                x = col * cell;
                z = row * cell;
            } else {
                switch (state.facing) {
                    case NORTH -> {
                        x = col * cell;
                        z = 1.0 - (row + 1) * cell;
                    }
                    case EAST -> {
                        x = row * cell;
                        z = col * cell;
                    }
                    case SOUTH -> {
                        x = 1.0 - (col + 1) * cell;
                        z = row * cell;
                    }
                    case WEST -> {
                        x = 1.0 - (row + 1) * cell;
                        z = 1.0 - (col + 1) * cell;
                    }
                    default -> {
                        x = col * cell;
                        z = row * cell;
                    }
                }
            }
            double y = PalletBlockEntityRenderState.FIRST_LAYER_Y
                    + layer * PalletBlockEntityRenderState.LAYER_SPACING;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(PalletBlockEntityRenderState.ITEM_SCALE,
                    PalletBlockEntityRenderState.ITEM_SCALE,
                    PalletBlockEntityRenderState.ITEM_SCALE);
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
                state.items[i].submit(poseStack, collector, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }

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
            // Tile one model per block of length instead of smearing
            // 16 pixels across both blocks.
            submitBox(state, i, poseStack, collector, x, .19f, .015f, .43f, .105f, .985f);
            submitBox(state, i, poseStack, collector, x, .19f, 1.0f, .43f, .105f, .985f);
        }

        // Transverse lower runners, using the same four craft selections.
        // Each runner continues across all four footprint blocks, not one
        // tiny runner per quadrant. The underside colors can differ from
        // the visible deck-board colors.
        for (float x : new float[]{.035f, 1.0f}) {
            submitBox(state, 2, poseStack, collector, x, .035f, .12f, .965f, .15f, .23f);
            submitBox(state, 3, poseStack, collector, x, .035f, .88f, .965f, .15f, .23f);
            submitBox(state, 0, poseStack, collector, x, .035f, 1.65f, .965f, .15f, .23f);
        }
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
