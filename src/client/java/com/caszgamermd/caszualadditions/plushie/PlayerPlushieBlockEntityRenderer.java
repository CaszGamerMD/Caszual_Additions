package com.caszgamermd.caszualadditions.plushie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieBlockEntityRenderer
        implements BlockEntityRenderer<PlayerPlushieBlockEntity, PlayerPlushieRenderState> {
    private static final Identifier WHITE_WOOL = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float SCALE = 0.42F;
    private static final float ANCHOR_Y = 0.63F;
    private static final Identifier BOOK_COVER = Identifier.withDefaultNamespace("textures/block/red_wool.png");
    private static final Identifier BOOK_PAGES = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final Identifier BOOK_SPINE = Identifier.withDefaultNamespace("textures/block/brown_terracotta.png");
    private static final ModelPart OPEN_BOOK = makeOpenBook();

    /**
     * Small 3D open book: independent angled covers, inset cream pages and a
     * raised spine. No flat item sprite clipping through the plushie's hands.
     * Pages are separated from the covers by 0.04 model pixels.
     */
    private static ModelPart makeOpenBook() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("left_cover",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.70F, -3.35F, -0.46F, 4.32F, 6.70F, 0.46F),
                PartPose.offsetAndRotation(-0.30F, 0, 0, 0, 0.22F, 0));
        root.addOrReplaceChild("right_cover",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.38F, -3.35F, -0.46F, 4.32F, 6.70F, 0.46F),
                PartPose.offsetAndRotation(0.30F, 0, 0, 0, -0.22F, 0));
        root.addOrReplaceChild("left_pages",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.37F, -3.02F, -0.85F, 3.88F, 6.04F, 0.35F),
                PartPose.offsetAndRotation(-0.30F, 0, 0, 0, 0.22F, 0));
        root.addOrReplaceChild("right_pages",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.49F, -3.02F, -0.85F, 3.88F, 6.04F, 0.35F),
                PartPose.offsetAndRotation(0.30F, 0, 0, 0, -0.22F, 0));
        root.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.48F, -3.36F, -0.23F, 0.96F, 6.72F, 0.78F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static void drawOpenBook(PoseStack pose, SubmitNodeCollector collector, int light) {
        pose.pushPose();
        // Both arms are bent toward this shared center; the book is in the
        // hands rather than stuck to the torso or floating at head level.
        pose.translate(0, 0.58F, -0.49F);
        pose.mulPose(Axis.XP.rotationDegrees(15.0F));
        pose.scale(1.0F / 16, 1.0F / 16, 1.0F / 16);
        for (String name : new String[]{"left_cover", "right_cover"}) {
            collector.order(2).submitModelPart(OPEN_BOOK.getChild(name), pose,
                    RenderTypes.entityCutout(BOOK_COVER), light,
                    OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        }
        for (String name : new String[]{"left_pages", "right_pages"}) {
            collector.order(3).submitModelPart(OPEN_BOOK.getChild(name), pose,
                    RenderTypes.entityCutout(BOOK_PAGES), light,
                    OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        }
        collector.order(4).submitModelPart(OPEN_BOOK.getChild("spine"), pose,
                RenderTypes.entityCutout(BOOK_SPINE), light,
                OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        pose.popPose();
    }

    private final PlayerSkinRenderCache skinCache;
    private final ItemModelResolver itemModelResolver;
    private final PosedPlushieModel wideModel;
    private final PosedPlushieModel slimModel;
    private final PosedPlushieModel woolBodyModel;
    private final SkullModelBase playerHeadModel;

    public PlayerPlushieBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        skinCache = context.playerSkinRenderCache();
        itemModelResolver = context.itemModelResolver();
        wideModel = createModel(false, true);
        slimModel = createModel(true, true);
        woolBodyModel = createModel(false, false);
        playerHeadModel = SkullBlockRenderer.createModel(context.entityModelSet(), SkullBlock.Types.PLAYER);
    }

    private static PosedPlushieModel createModel(boolean slim, boolean showHead) {
        var root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
        root.getChild("head").visible = showHead;

        root.getChild("right_arm").xRot = -0.20F;
        root.getChild("left_arm").xRot = -0.20F;
        root.getChild("right_arm").zRot = 0.10F;
        root.getChild("left_arm").zRot = -0.10F;
        root.getChild("right_leg").xRot = -0.18F;
        root.getChild("left_leg").xRot = -0.18F;

        return new PosedPlushieModel(root, showHead);
    }

    @Override
    public PlayerPlushieRenderState createRenderState() {
        return new PlayerPlushieRenderState();
    }

    @Override
    public void extractRenderState(
            PlayerPlushieBlockEntity blockEntity,
            PlayerPlushieRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.profile = blockEntity.profile();
        state.facing = blockEntity.getBlockState().getValue(PlayerPlushieBlock.FACING);
        state.pose = blockEntity.pose();
        ItemStack prop = switch (state.pose) {
            case 3 -> new ItemStack(Items.IRON_SWORD);
            case 4 -> new ItemStack(Items.IRON_AXE);
            case 5 -> new ItemStack(Items.IRON_PICKAXE);
            case 6 -> new ItemStack(Items.IRON_HOE);
            case 7 -> new ItemStack(Items.SPYGLASS);
            default -> ItemStack.EMPTY;
        };
        state.prop.clear();
        if (!prop.isEmpty()) {
            itemModelResolver.updateForTopItem(state.prop, prop,
                    ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(
            PlayerPlushieRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, ANCHOR_Y, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        if (state.pose == 9) {
            // Lay on the back: after the arm/body scale, +90deg points the
            // plushie's face UP (the old -88deg pointed it at the floor).
            poseStack.translate(0.0F, -0.37F, 0.06F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(-SCALE, -SCALE, SCALE);

        PlayerSkinRenderCache.RenderInfo resolved = resolvedSkin(state);
        if (resolved != null) {
            PosedPlushieModel model = resolved.playerSkin().model() == PlayerModelType.SLIM ? slimModel : wideModel;
            collector.submitModel(
                    model,
                    state.pose,
                    poseStack,
                    resolved.renderType(),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    state.breakProgress
            );
        } else {
            collector.submitModel(
                    woolBodyModel,
                    state.pose,
                    poseStack,
                    WHITE_WOOL,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    state.breakProgress
            );

            if (playerHeadModel != null) {
                var headRenderType = state.profile != null
                        ? skinCache.getOrDefault(state.profile).renderType()
                        : SkullBlockRenderer.getSkullRenderType(SkullBlock.Types.PLAYER, null);
                SkullBlockRenderer.submitSkull(
                        0.0F,
                        poseStack,
                        collector,
                        state.lightCoords,
                        playerHeadModel,
                        headRenderType,
                        0,
                        state.breakProgress
                );
            }
        }

        if (state.pose == 2) {
            drawOpenBook(poseStack, collector, state.lightCoords);
        } else if (state.pose >= 3 && state.pose <= 6) {
            // Tool origin follows the raised right hand. A half-turn around
            // Z fixes the inverted blade/head from the old FIXED transform;
            // pitch makes the weapon/tool project forward as if being used.
            poseStack.pushPose();
            poseStack.translate(-0.31F, 0.23F, -0.66F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-22));
            poseStack.mulPose(Axis.XP.rotationDegrees(-55));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.scale(0.48F, 0.48F, 0.48F);
            state.prop.submit(poseStack, collector, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        } else if (state.pose == 7) {
            // Spyglass remains by the eye instead of inheriting a mining pose.
            poseStack.pushPose();
            poseStack.translate(-0.31F, -0.01F, -0.39F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-76));
            poseStack.scale(0.34F, 0.34F, 0.34F);
            state.prop.submit(poseStack, collector, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /**
     * The collector defers rendering and calls Model.setupAnim for each queued
     * submission. Model.Simple.setupAnim resets the pose, which used to erase
     * every plushie animation before the mesh was rendered. Carry the pose as
     * immutable render state and apply it AFTER the vanilla reset instead.
     *
     * A shared model can now render adjacent plushies with different poses
     * without the last submission overriding every other plushie's limbs.
     */
    private static final class PosedPlushieModel extends Model<Integer> {
        private PosedPlushieModel(ModelPart root, boolean showHead) {
            super(root, showHead ? RenderTypes::entityTranslucent : RenderTypes::entityCutout);
        }

        @Override
        public void setupAnim(Integer pose) {
            super.setupAnim(pose);
            poseModel(root(), pose == null ? 0 : pose);
        }
    }

    /** Reset every part before posing because the models are shared between plushies. */
    private static void poseModel(ModelPart root, int pose) {
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        ModelPart right = root.getChild("right_arm");
        ModelPart left = root.getChild("left_arm");
        ModelPart rightLeg = root.getChild("right_leg");
        ModelPart leftLeg = root.getChild("left_leg");
        head.xRot = head.yRot = head.zRot = 0;
        body.xRot = body.yRot = body.zRot = 0;
        right.xRot = left.xRot = -0.20F;
        right.zRot = 0.10F;
        left.zRot = -0.10F;
        right.yRot = left.yRot = 0;
        rightLeg.xRot = leftLeg.xRot = -0.18F;
        rightLeg.yRot = leftLeg.yRot = 0;
        rightLeg.zRot = leftLeg.zRot = 0;
        switch (pose) {
            case 1 -> {
                rightLeg.xRot = leftLeg.xRot = -1.35F;
                right.xRot = left.xRot = -0.50F;
            }
            case 2 -> {
                head.xRot = 0.34F;
                right.xRot = left.xRot = -0.89F;
                right.yRot = -0.39F;
                left.yRot = 0.39F;
                right.zRot = 0.19F;
                left.zRot = -0.19F;
                body.xRot = 0.06F;
            }
            case 3, 4, 5, 6 -> {
                // Active swing: tool-hand driven forward, other arm for balance.
                head.xRot = 0.18F;
                head.yRot = -0.10F;
                body.xRot = 0.10F;
                right.xRot = -1.43F;
                right.yRot = -0.08F;
                right.zRot = 0.14F;
                left.xRot = -0.66F;
                left.yRot = 0.14F;
                left.zRot = -0.24F;
                rightLeg.xRot = -0.34F;
                leftLeg.xRot = 0.17F;
            }
            case 7 -> {
                head.xRot = -0.10F;
                right.xRot = -1.72F;
                right.yRot = -0.34F;
            }
            case 8 -> {
                right.xRot = -1.05F;
                left.xRot = 0.95F;
                rightLeg.xRot = 1.00F;
                leftLeg.xRot = -1.00F;
                body.xRot = 0.17F;
            }
            case 9 -> {
                head.xRot = 0;
                right.xRot = left.xRot = -0.06F;
                right.zRot = 0.16F;
                left.zRot = -0.16F;
                rightLeg.xRot = leftLeg.xRot = 0.02F;
            }
            case 10 -> {
                right.xRot = -2.55F;
                right.zRot = 0.40F;
                head.yRot = 0.18F;
            }
            case 11 -> {
                head.xRot = 0.68F;
                right.xRot = left.xRot = -1.40F;
                right.yRot = -0.20F;
                left.yRot = 0.20F;
            }
            default -> {}
        }
    }

    private @Nullable PlayerSkinRenderCache.RenderInfo resolvedSkin(PlayerPlushieRenderState state) {
        if (state.profile == null) return null;
        if (state.profile.skinPatch().body().isPresent()) return skinCache.getOrDefault(state.profile);

        Optional<PlayerSkinRenderCache.RenderInfo> loaded =
                skinCache.lookup(state.profile).getNow(Optional.empty());
        return loaded.orElse(null);
    }
}
