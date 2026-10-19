package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Every one of the 12 real pallet items gets FOUR independent material
 * boards (not one repeated, generic oak-plank texture). */
public final class StackOfPalletsRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState[][] boards=new BlockModelRenderState[12][4];
    public final boolean[] occupied=new boolean[12];
    public final boolean[] wrapped=new boolean[12];
    public final BlockModelRenderState glass=new BlockModelRenderState();
    public final BlockModelRenderState floorGrill=new BlockModelRenderState();
    public StackOfPalletsRenderState(){
        for(int slot=0;slot<12;slot++)
            for(int board=0;board<4;board++)
                boards[slot][board]=new BlockModelRenderState();
    }
}
