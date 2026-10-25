package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;

/** Unloaded truck frame plus exactly one server-synced packed pallet visual. */
public final class PalletJackRenderState extends EntityRenderState {
    public float heading;
    public boolean loaded;
    public int cargo;
    public final BlockModelRenderState metal=new BlockModelRenderState();
    public final BlockModelRenderState handle=new BlockModelRenderState();
    public final BlockModelRenderState wheel=new BlockModelRenderState();
    public final BlockModelRenderState deck=new BlockModelRenderState();
    public final BlockModelRenderState[] boards={
        new BlockModelRenderState(),new BlockModelRenderState(),
        new BlockModelRenderState(),new BlockModelRenderState()};
    public final BlockModelRenderState crate=new BlockModelRenderState();
    public final BlockModelRenderState glass=new BlockModelRenderState();
}
