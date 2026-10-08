package com.caszgamermd.caszualadditions.plushie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieSpecialRenderer implements SpecialModelRenderer<ResolvableProfile> {
    private static final Identifier WHITE_WOOL = Identifier.withDefaultNamespace("textures/block/white_wool.png");
    private static final float SCALE = 0.42F;

    private final PlayerSkinRenderCache skinCache;
    private final Model.Simple wideModel;
    private final Model.Simple slimModel;
    private final Model.Simple woolBodyModel;
    private final SkullModelBase playerHeadModel;

    public PlayerPlushieSpecialRenderer(SpecialModelRenderer.BakingContext context) {
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
    public void submit(
            @Nullable ResolvableProfile profile,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.67F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-SCALE, -SCALE, SCALE);

        PlayerSkinRenderCache.RenderInfo resolved = resolvedSkin(profile);
        if (resolved != null) {
            Model.Simple model = resolved.playerSkin().model() == PlayerModelType.SLIM ? slimModel : wideModel;
            collector.submitModel(
                    model,
                    Unit.INSTANCE,
                    poseStack,
                    resolved.renderType(),
                    lightCoords,
                    overlayCoords,
                    -1,
                    null,
                    outlineColor,
                    null
            );
        } else {
            collector.submitModel(
                    woolBodyModel,
                    Unit.INSTANCE,
                    poseStack,
                    WHITE_WOOL,
                    lightCoords,
                    overlayCoords,
                    -1,
                    null,
                    outlineColor,
                    null
            );

            if (playerHeadModel != null) {
                var headRenderType = profile != null
                        ? skinCache.getOrDefault(profile).renderType()
                        : SkullBlockRenderer.getSkullRenderType(SkullBlock.Types.PLAYER, null);
                SkullBlockRenderer.submitSkull(
                        0.0F,
                        poseStack,
                        collector,
                        lightCoords,
                        playerHeadModel,
                        headRenderType,
                        outlineColor,
                        null
                );
            }
        }

        poseStack.popPose();
    }

    private @Nullable PlayerSkinRenderCache.RenderInfo resolvedSkin(@Nullable ResolvableProfile profile) {
        if (profile == null) return null;
        if (profile.skinPatch().body().isPresent()) return skinCache.getOrDefault(profile);

        Optional<PlayerSkinRenderCache.RenderInfo> loaded =
                skinCache.lookup(profile).getNow(Optional.empty());
        return loaded.orElse(null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float min = 0.08F;
        float max = 0.92F;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    output.accept(new Vector3f(
                            x == 0 ? min : max,
                            y == 0 ? min : max,
                            z == 0 ? min : max
                    ));
                }
            }
        }
    }

    @Override
    public @Nullable ResolvableProfile extractArgument(ItemStack stack) {
        return stack.get(DataComponents.PROFILE);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ResolvableProfile> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public PlayerPlushieSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new PlayerPlushieSpecialRenderer(context);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
