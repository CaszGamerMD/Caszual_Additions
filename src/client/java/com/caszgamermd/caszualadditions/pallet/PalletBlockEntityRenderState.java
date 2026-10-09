package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PalletBlockEntityRenderState extends BlockEntityRenderState {
    /*
     * One shared 2x2 pallet controller draws a 4x4x8 array of visible cargo:
     * half-block (8px) display units with one-pixel gaps on every axis.
     * Storage still has 216 slots; the final cargo tier is below y=5.
     */
    public static final int DISPLAY_COLUMNS = 4;
    public static final int DISPLAY_ROWS = 4;
    public static final int DISPLAY_LAYERS = 8;
    public static final int DISPLAY_CAPACITY =
            DISPLAY_COLUMNS * DISPLAY_ROWS * DISPLAY_LAYERS;
    public static final float ITEM_SCALE = 0.5F;
    public static final float FIRST_LAYER_Y = 0.30F;
    public static final float LAYER_SPACING = 0.5625F;

    public final BlockModelRenderState base = new BlockModelRenderState();
    public final BlockModelRenderState carton = new BlockModelRenderState();
    public final BlockModelRenderState[] cargoBlocks = new BlockModelRenderState[DISPLAY_CAPACITY];
    public final BlockModelRenderState[] boards = new BlockModelRenderState[4];
    public final boolean[] boxed = new boolean[DISPLAY_CAPACITY];
    public final ItemStackRenderState[] items = new ItemStackRenderState[DISPLAY_CAPACITY];
    public int visibleCount;
    public boolean root;

    public PalletBlockEntityRenderState() {
        for (int i = 0; i < cargoBlocks.length; i++) cargoBlocks[i] = new BlockModelRenderState();
        for (int i = 0; i < boards.length; i++) boards[i] = new BlockModelRenderState();
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStackRenderState();
        }
    }
}
