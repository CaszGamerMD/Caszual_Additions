package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class StorageGolemShelfRenderState extends BlockEntityRenderState {
    public int docked;
    public net.minecraft.core.Direction facing=net.minecraft.core.Direction.NORTH;
    public final BlockModelRenderState frame=new BlockModelRenderState();
    public final BlockModelRenderState lamp=new BlockModelRenderState();
}
