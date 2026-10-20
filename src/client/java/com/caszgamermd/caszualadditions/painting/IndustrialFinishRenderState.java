package com.caszgamermd.caszualadditions.painting;

import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class IndustrialFinishRenderState extends EntityRenderState {
    public List<IndustrialPaintModelGeometry.Part> elements=List.of();
    public boolean show;
    /** Each proxy has ONE baked quad; rendering a thin six-sided cube
     * produced coincident front/back quads and black z-fighting. */
    public final BlockModelRenderState[] faces=new BlockModelRenderState[6];
    public IndustrialFinishRenderState(){
        for(int i=0;i<faces.length;i++)faces[i]=new BlockModelRenderState();
    }
}
