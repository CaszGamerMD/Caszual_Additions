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
    private static final Identifier TELESCOPE_COPPER = Identifier.withDefaultNamespace("textures/block/copper_block.png");
    private static final Identifier TELESCOPE_TRIM = Identifier.withDefaultNamespace("textures/block/gold_block.png");
    private static final Identifier TELESCOPE_LENS = Identifier.withDefaultNamespace("textures/block/tinted_glass.png");
    private static final ModelPart SPYGLASS = makeSpyglass();

    /**
     * An actual open hardcover book, almost twice the old size. The cream
     * paper is on the +Z (plushie-facing) side of each cover, not the outer
     * side that the previous implementation accidentally exposed.
     */
    private static ModelPart makeOpenBook() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("left_cover",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.65F, -4.15F, -0.66F, 7.20F, 8.30F, 0.60F),
                PartPose.offsetAndRotation(-0.25F, 0, 0, 0, 0.31F, 0));
        root.addOrReplaceChild("right_cover",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.45F, -4.15F, -0.66F, 7.20F, 8.30F, 0.60F),
                PartPose.offsetAndRotation(0.25F, 0, 0, 0, -0.31F, 0));
        root.addOrReplaceChild("left_pages",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.22F, -3.75F, -0.01F, 6.55F, 7.46F, 0.42F),
                PartPose.offsetAndRotation(-0.25F, 0, 0, 0, 0.31F, 0));
        root.addOrReplaceChild("right_pages",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.67F, -3.75F, -0.01F, 6.55F, 7.46F, 0.42F),
                PartPose.offsetAndRotation(0.25F, 0, 0, 0, -0.31F, 0));
        root.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.54F, -4.19F, -0.76F, 1.08F, 8.38F, 1.20F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static void drawOpenBook(PoseStack pose, SubmitNodeCollector collector, int light) {
        pose.pushPose();
        // Reading hands are bent inward in pose 2; book is propped in front
        // of the torso and angled toward the plushie's eyes.
        pose.translate(0, 0.56F, -0.61F);
        pose.mulPose(Axis.XP.rotationDegrees(-23.0F));
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

    /**
     * Voxel spyglass with a full-length square barrel, stepped inner tube,
     * raised metal bands and an actual dark objective lens. A flat vanilla
     * item sprite does not have the depth that this pose needs.
     */
    private static ModelPart makeSpyglass() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("barrel",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.30F, -1.30F, -12.5F, 2.60F, 2.60F, 12.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("extension",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.03F, -1.03F, -16.6F, 2.06F, 2.06F, 4.4F),
                PartPose.ZERO);
        root.addOrReplaceChild("objective_rim",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.61F, -1.61F, -17.1F, 3.22F, 3.22F, 0.75F),
                PartPose.ZERO);
        root.addOrReplaceChild("center_band",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.52F, -1.52F, -9.8F, 3.04F, 3.04F, 0.70F),
                PartPose.ZERO);
        root.addOrReplaceChild("eyepiece",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.52F, -1.52F, -0.9F, 3.04F, 3.04F, 1.85F),
                PartPose.ZERO);
        root.addOrReplaceChild("lens",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.04F, -1.04F, -17.18F, 2.08F, 2.08F, 0.12F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static void drawSpyglass(PoseStack pose, SubmitNodeCollector collector, int light) {
        pose.pushPose();
        pose.translate(-0.23F, -0.16F, -0.32F);
        pose.mulPose(Axis.XP.rotationDegrees(-5.0F));
        pose.scale(1.0F / 16, 1.0F / 16, 1.0F / 16);
        for (String part : new String[]{"barrel", "extension"}) {
            collector.order(2).submitModelPart(SPYGLASS.getChild(part), pose,
                    RenderTypes.entityCutout(TELESCOPE_COPPER), light,
                    OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        }
        for (String part : new String[]{"objective_rim", "center_band", "eyepiece"}) {
            collector.order(3).submitModelPart(SPYGLASS.getChild(part), pose,
                    RenderTypes.entityCutout(TELESCOPE_TRIM), light,
                    OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        }
        collector.order(4).submitModelPart(SPYGLASS.getChild("lens"), pose,
                RenderTypes.entityCutout(TELESCOPE_LENS), light,
                OverlayTexture.NO_OVERLAY, null, -1, null, 0);
        pose.popPose();
    }

    /**
     * Each tool is an independent action with the grip positioned next to its
     * own posed hand. The original shared transform held every tool upside
     * down at the same sword angle.
     */
    private static void drawTool(PlayerPlushieRenderState state, PoseStack pose,
                                 SubmitNodeCollector collector) {
        pose.pushPose();
        switch (state.pose) {
            case 3 -> { // sword: a horizontal mid-strike, blade facing away
                pose.translate(-0.36F, 0.23F, -0.78F);
                pose.mulPose(Axis.YP.rotationDegrees(-38));
                pose.mulPose(Axis.XP.rotationDegrees(-28));
                pose.mulPose(Axis.ZP.rotationDegrees(72));
                pose.scale(0.70F, 0.70F, 0.70F);
            }
            case 4 -> { // axe: chopping downward from above the shoulder
                pose.translate(-0.41F, -0.26F, -0.53F);
                pose.mulPose(Axis.YP.rotationDegrees(-28));
                pose.mulPose(Axis.XP.rotationDegrees(-43));
                pose.mulPose(Axis.ZP.rotationDegrees(142));
                pose.scale(0.75F, 0.75F, 0.75F);
            }
            case 5 -> { // pickaxe: angled mining swing toward the ground
                pose.translate(-0.43F, -0.10F, -0.70F);
                pose.mulPose(Axis.YP.rotationDegrees(-38));
                pose.mulPose(Axis.XP.rotationDegrees(-48));
                pose.mulPose(Axis.ZP.rotationDegrees(118));
                pose.scale(0.74F, 0.74F, 0.74F);
            }
            case 6 -> { // hoe: working down and outward in front of the feet
                pose.translate(-0.41F, 0.60F, -0.70F);
                pose.mulPose(Axis.YP.rotationDegrees(-26));
                pose.mulPose(Axis.XP.rotationDegrees(-38));
                pose.mulPose(Axis.ZP.rotationDegrees(152));
                pose.scale(0.74F, 0.74F, 0.74F);
            }
            default -> {
                pose.popPose();
                return;
            }
        }
        state.prop.submit(pose, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0);
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
            drawTool(state, poseStack, collector);
        } else if (state.pose == 7) {
            drawSpyglass(poseStack, collector, state.lightCoords);
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
            case 3 -> { // sword: lunging through the middle of a slash
                head.xRot = 0.11F;
                head.yRot = -0.22F;
                body.yRot = -0.17F;
                right.xRot = -1.38F;
                right.yRot = -0.32F;
                right.zRot = 0.14F;
                left.xRot = -0.46F;
                left.yRot = 0.20F;
                left.zRot = -0.30F;
                rightLeg.xRot = -0.51F;
                leftLeg.xRot = 0.23F;
            }
            case 4 -> { // axe: heavy overhead woodcutting stroke
                head.xRot = 0.37F;
                body.xRot = 0.19F;
                body.yRot = -0.09F;
                right.xRot = -2.37F;
                right.yRot = -0.28F;
                right.zRot = 0.32F;
                left.xRot = -1.12F;
                left.yRot = 0.28F;
                left.zRot = -0.20F;
                rightLeg.xRot = 0.24F;
                leftLeg.xRot = -0.32F;
            }
            case 5 -> { // pickaxe: raised arm mining into a block
                head.xRot = 0.42F;
                body.xRot = 0.23F;
                right.xRot = -1.94F;
                right.yRot = -0.53F;
                right.zRot = 0.14F;
                left.xRot = -0.98F;
                left.yRot = 0.25F;
                left.zRot = -0.32F;
                rightLeg.xRot = -0.18F;
                leftLeg.xRot = 0.25F;
            }
            case 6 -> { // hoe: hunched down and tilling the soil
                head.xRot = 0.58F;
                body.xRot = 0.34F;
                right.xRot = -0.87F;
                right.yRot = -0.28F;
                right.zRot = 0.21F;
                left.xRot = -0.62F;
                left.yRot = 0.13F;
                left.zRot = -0.24F;
                rightLeg.xRot = -0.28F;
                leftLeg.xRot = 0.33F;
            }
            case 7 -> { // searching through a proper 3D telescope
                head.xRot = -0.08F;
                head.yRot = -0.07F;
                right.xRot = -1.65F;
                right.yRot = -0.38F;
                right.zRot = 0.12F;
                left.xRot = 0.04F;
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
            case 10 -> { // elbow high, hand OUTSIDE the face
                right.xRot = -2.10F;
                right.yRot = -0.13F;
                right.zRot = 1.04F;
                left.xRot = -0.17F;
                head.yRot = 0.12F;
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
