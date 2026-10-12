package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** One render state shared by four corner golem models and their pallet. */
public final class MovingPalletRenderState extends LivingEntityRenderState {
    public int cargo;
    public final BlockModelRenderState deck=new BlockModelRenderState();
    public final BlockModelRenderState foot=new BlockModelRenderState();
    public final BlockModelRenderState crate=new BlockModelRenderState();
    public final BlockModelRenderState glass=new BlockModelRenderState();
}
