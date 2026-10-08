package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PalletBlockEntityRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState base = new BlockModelRenderState();
    public final ItemStackRenderState[] items = new ItemStackRenderState[PalletBlockEntity.SLOTS];
    public final boolean[] occupied = new boolean[PalletBlockEntity.SLOTS];
    public boolean root;

    public PalletBlockEntityRenderState() {
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStackRenderState();
        }
    }
}
