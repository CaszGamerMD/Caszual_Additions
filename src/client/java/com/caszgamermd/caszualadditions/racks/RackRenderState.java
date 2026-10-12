package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public final class RackRenderState extends BlockEntityRenderState {
    public static final int CARGO=27;
    public static final float CELL=2.0f/3.0f;
    public final BlockModelRenderState[] boards=new BlockModelRenderState[4];
    public final BlockModelRenderState carton=new BlockModelRenderState();
    public final BlockModelRenderState crossBeam=new BlockModelRenderState();
    public final BlockModelRenderState highlightEdge=new BlockModelRenderState();
    public boolean outlined;
    public final BlockModelRenderState nameplate=new BlockModelRenderState();
    public final boolean[] linkedFaces=new boolean[6];
    public String rackLabel="",aisleLabel="";
    public final boolean[] aisleEndFaces=new boolean[6];
    public final ItemStackRenderState[] items=new ItemStackRenderState[CARGO];
    public final boolean[] boxed=new boolean[CARGO];
    public Direction facing=Direction.NORTH;
    public boolean pallet;
    public boolean sealed;
    public boolean inputMode;
    public final BlockModelRenderState statusLamp=new BlockModelRenderState();
    public int visible;
    public RackRenderState() {
        for(int i=0;i<boards.length;i++)boards[i]=new BlockModelRenderState();
        for(int i=0;i<CARGO;i++)items[i]=new ItemStackRenderState();
    }
}
