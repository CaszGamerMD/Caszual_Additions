package com.caszgamermd.caszualadditions.quarter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class QuarterBlockSpecialRenderer implements SpecialModelRenderer<BlockState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public QuarterBlockSpecialRenderer() {
        this.blockModelResolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
    }

    @Override
    public void submit(
            @Nullable BlockState source,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        BlockState material = source == null ? Blocks.STONE.defaultBlockState() : source;
        BlockModelRenderState renderState = new BlockModelRenderState();
        blockModelResolver.update(renderState, material, DISPLAY_CONTEXT);

        poseStack.pushPose();
        poseStack.translate(0.25F, 0.25F, 0.25F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderState.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    output.accept(new Vector3f(
                            x == 0 ? 0.25F : 0.75F,
                            y == 0 ? 0.25F : 0.75F,
                            z == 0 ? 0.25F : 0.75F
                    ));
                }
            }
        }
    }

    @Override
    public BlockState extractArgument(ItemStack stack) {
        return QuarterBlocks.source(stack);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<BlockState> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public QuarterBlockSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new QuarterBlockSpecialRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
