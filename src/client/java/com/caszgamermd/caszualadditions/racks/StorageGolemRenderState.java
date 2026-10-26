package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Four raised-arm Golems move in a square; ONLY the lead submits one
 * shared pallet model between them, avoiding four overlapping duplicates. */
public final class StorageGolemRenderState extends LivingEntityRenderState {
    public boolean ridingForklift;
    public boolean carryingPallet;
    public boolean showSharedPallet;
    public boolean pushingEmptyPallet;
    public int visibleCargo;
    public int heldSlots;
    // One real item model held between the worker's wrists. All nine
    // actual inventory slots remain synced independently server-side;
    // rendering never substitutes a fake tray or cardboard proxy.
    public final ItemStackRenderState[] heldStack={new ItemStackRenderState(),new ItemStackRenderState(),new ItemStackRenderState()};
    // The FIXED item display transform halves placed-block models.
    public final boolean[] heldIsBlock=new boolean[3];
    public final BlockModelRenderState palletDeck=new BlockModelRenderState();
    public final BlockModelRenderState[] courierBoards={new BlockModelRenderState(),new BlockModelRenderState(),new BlockModelRenderState(),new BlockModelRenderState()};
    public final BlockModelRenderState palletSupport=new BlockModelRenderState();
    public final BlockModelRenderState crate=new BlockModelRenderState();
    public final BlockModelRenderState glass=new BlockModelRenderState();
}
