package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class WarehouseForkliftRenderState extends LivingEntityRenderState {
    public float mastHeight;
    public int task;
    public final ItemStackRenderState pallet=new ItemStackRenderState();
}
