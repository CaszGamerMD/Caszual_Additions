package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PalletBlockEntityRenderState extends BlockEntityRenderState {
    /*
     * A 2x2 pallet has a 3x3 footprint for visible items. Only seven
     * tiers are shown; the full 216-slot storage inventory is unaffected.
     *
     * Item models rendered in GROUND context normally fit inside a unit
     * cube before scaling. The final tier ends below five world blocks.
     */
    public static final int DISPLAY_COLUMNS = 3;
    public static final int DISPLAY_ROWS = 3;
    public static final int DISPLAY_LAYERS = 7;
    public static final int DISPLAY_CAPACITY =
            DISPLAY_COLUMNS * DISPLAY_ROWS * DISPLAY_LAYERS;
    public static final float ITEM_SCALE = 0.55F;
    public static final float FIRST_LAYER_Y = 0.60F;
    public static final float LAYER_SPACING = 0.65F;

    public final BlockModelRenderState base = new BlockModelRenderState();
    public final ItemStackRenderState[] items = new ItemStackRenderState[DISPLAY_CAPACITY];
    public int visibleCount;
    public boolean root;

    public PalletBlockEntityRenderState() {
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStackRenderState();
        }
    }
}
