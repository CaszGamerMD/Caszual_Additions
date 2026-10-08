package com.caszgamermd.caszualadditions.plushie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import net.minecraft.client.model.Model;
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
        poseStack.scale(-SCALE, -SCALE, SCALE);

        PlayerSkinRenderCache.RenderInfo resolved = resolvedSkin(state);
        if (resolved != null) {
            Model.Simple model = resolved.playerSkin().model() == PlayerModelType.SLIM ? slimModel : wideModel;
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

    private @Nullable PlayerSkinRenderCache.RenderInfo resolvedSkin(PlayerPlushieRenderState state) {
        if (state.profile == null) return null;
        if (state.profile.skinPatch().body().isPresent()) return skinCache.getOrDefault(state.profile);

        Optional<PlayerSkinRenderCache.RenderInfo> loaded =
                skinCache.lookup(state.profile).getNow(Optional.empty());
        return loaded.orElse(null);
    }
}
