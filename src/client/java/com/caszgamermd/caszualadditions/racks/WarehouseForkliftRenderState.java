package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Full-size visible 2x2 pallet and carton/glass cargo while on the forks.
 * Only the real 217-slot Jack item owns the inventory server-side. */
public final class WarehouseForkliftRenderState extends LivingEntityRenderState {
    public float mastHeight;
    public int task;
    public int visibleCargo;
    public boolean hasPallet;
    public final BlockModelRenderState palletDeck=new BlockModelRenderState();
    public final BlockModelRenderState[] boards={
            new BlockModelRenderState(),new BlockModelRenderState(),
            new BlockModelRenderState(),new BlockModelRenderState()};
    public final BlockModelRenderState support=new BlockModelRenderState();
    public final BlockModelRenderState crate=new BlockModelRenderState();
    public final BlockModelRenderState glass=new BlockModelRenderState();
}
