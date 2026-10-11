package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public final class PalletBlockEntityRenderState extends BlockEntityRenderState {
    /*
     * Only 3 contiguous tiers of 3x3 items are visible on the 2x2 deck
     * (27 display positions, unchanged 216-slot storage capacity).
     * Each cell is 2/3 of a block on every axis, and the first tier
     * starts at the actual top of the pallet without protruding below.
     */
    public static final int DISPLAY_COLUMNS = 3;
    public static final int DISPLAY_ROWS = 3;
    public static final int DISPLAY_LAYERS = 3;
    public static final int DISPLAY_CAPACITY =
            DISPLAY_COLUMNS * DISPLAY_ROWS * DISPLAY_LAYERS;
    public static final float ITEM_SCALE = 2.0F / 3.0F;
    // 5/16 is the collision height; deck board tops are at 0.295.
    public static final float FIRST_LAYER_Y = 5.0F / 16.0F;
    public static final float LAYER_SPACING = ITEM_SCALE;

    public final BlockModelRenderState base = new BlockModelRenderState();
    public final BlockModelRenderState carton = new BlockModelRenderState();
    public final BlockModelRenderState[] cargoBlocks = new BlockModelRenderState[DISPLAY_CAPACITY];
    public final BlockModelRenderState[] boards = new BlockModelRenderState[4];
    public final boolean[] boxed = new boolean[DISPLAY_CAPACITY];
    public final ItemStackRenderState[] items = new ItemStackRenderState[DISPLAY_CAPACITY];
    public int visibleCount;
    public boolean root;
    public Direction facing = Direction.NORTH;
    public boolean legacy;

    public PalletBlockEntityRenderState() {
        for (int i = 0; i < cargoBlocks.length; i++) cargoBlocks[i] = new BlockModelRenderState();
        for (int i = 0; i < boards.length; i++) boards[i] = new BlockModelRenderState();
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStackRenderState();
        }
    }
}
