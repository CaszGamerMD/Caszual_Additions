package com.caszgamermd.caszualadditions.painting;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class IndustrialFinishRenderState extends EntityRenderState {
    public List<AABB> shapes=List.of();
    public boolean show;
    public final BlockModelRenderState finish=new BlockModelRenderState();
}