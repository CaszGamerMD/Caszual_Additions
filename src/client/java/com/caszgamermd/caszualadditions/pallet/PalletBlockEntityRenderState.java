package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public final class PalletBlockEntityRenderState extends BlockEntityRenderState {
    /*
     * One shared 2x2 controller draws a tightly packed 3x3x7
     * display. Every cell is 2/3 of a block wide with no gaps;
     * seven full tiers reach just below five blocks above ground.
     * Storage capacity remains 216 slots (63 visible).
     */
    public static final int DISPLAY_COLUMNS = 3;
    public static final int DISPLAY_ROWS = 3;
    public static final int DISPLAY_LAYERS = 7;
    public static final int DISPLAY_CAPACITY =
            DISPLAY_COLUMNS * DISPLAY_ROWS * DISPLAY_LAYERS;
    public static final float ITEM_SCALE = 2.0F / 3.0F;
    public static final float FIRST_LAYER_Y = 0.3125F;
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
