package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Four raised-arm Golems move in a square; ONLY the lead submits one
 * shared pallet model between them, avoiding four overlapping duplicates. */
public final class StorageGolemRenderState extends LivingEntityRenderState {
    public boolean carryingPallet;
    public boolean showSharedPallet;
    public int visibleCargo;
    public final BlockModelRenderState palletDeck=new BlockModelRenderState();
    public final BlockModelRenderState palletSupport=new BlockModelRenderState();
    public final BlockModelRenderState crate=new BlockModelRenderState();
    public final BlockModelRenderState glass=new BlockModelRenderState();
}
