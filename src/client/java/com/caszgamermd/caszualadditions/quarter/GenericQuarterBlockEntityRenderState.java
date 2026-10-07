package com.caszgamermd.caszualadditions.quarter;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class GenericQuarterBlockEntityRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState[] quarters = {
            new BlockModelRenderState(), new BlockModelRenderState(),
            new BlockModelRenderState(), new BlockModelRenderState(),
            new BlockModelRenderState(), new BlockModelRenderState(),
            new BlockModelRenderState(), new BlockModelRenderState()
    };
    public final boolean[] occupied = new boolean[8];
}
