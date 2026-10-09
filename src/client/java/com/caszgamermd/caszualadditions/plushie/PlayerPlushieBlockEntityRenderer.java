package com.caszgamermd.caszualadditions.plushie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieBlockEntityRenderer
        implements BlockEntityRenderer<PlayerPlushieBlockEntity, PlayerPlushieRenderState> {
    private static final Identifier WHITE_WOOL = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float SCALE = 0.42F;
    private static final float ANCHOR_Y = 0.63F;

    private final PlayerSkinRenderCache skinCache;
    private final Model.Simple wideModel;
    private final Model.Simple slimModel;
    private final Model.Simple woolBodyModel;
    private final SkullModelBase playerHeadModel;

    public PlayerPlushieBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        skinCache = context.playerSkinRenderCache();
        wideModel = createModel(false, true);
        slimModel = createModel(true, true);
        woolBodyModel = createModel(false, false);
        playerHeadModel = SkullBlockRenderer.createModel(context.entityModelSet(), SkullBlock.Types.PLAYER);
    }

    private static Model.Simple createModel(boolean slim, boolean showHead) {
        var root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
        root.getChild("head").visible = showHead;

        root.getChild("right_arm").xRot = -0.20F;
        root.getChild("left_arm").xRot = -0.20F;
        root.getChild("right_arm").zRot = 0.10F;
        root.getChild("left_arm").zRot = -0.10F;
        root.getChild("right_leg").xRot = -0.18F;
        root.getChild("left_leg").xRot = -0.18F;

        return new Model.Simple(root, showHead ? RenderTypes::entityTranslucent : RenderTypes::entityCutout);
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
            poseStack.translate(0.0F, -0.40F, -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-88.0F));
        }
        poseStack.scale(-SCALE, -SCALE, SCALE);

        PlayerSkinRenderCache.RenderInfo resolved = resolvedSkin(state);
        if (resolved != null) {
            Model.Simple model = resolved.playerSkin().model() == PlayerModelType.SLIM ? slimModel : wideModel;
            poseModel(model, state.pose);
            collector.submitModel(
                    model,
                    Unit.INSTANCE,
                    poseStack,
                    resolved.renderType(),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    state.breakProgress
            );
        } else {
            poseModel(woolBodyModel, state.pose);
            collector.submitModel(
                    woolBodyModel,
                    Unit.INSTANCE,
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

        poseStack.popPose();
    }

    /** Reset every part before posing because the models are shared between plushies. */
    private static void poseModel(Model.Simple model, int pose) {
        ModelPart root = model.root();
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
                head.xRot = 0.32F;
                right.xRot = left.xRot = -1.00F;
                right.yRot = -0.38F;
                left.yRot = 0.38F;
            }
            case 3, 4, 5, 6 -> {
                right.xRot = -1.35F;
                right.zRot = -0.18F;
                left.xRot = -0.40F;
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
                right.xRot = left.xRot = -0.12F;
                rightLeg.xRot = leftLeg.xRot = 0;
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
