package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Client snapshot of the four complete pallet items carried by the balloon. */
public final class PalletBalloonRenderState extends LivingEntityRenderState {
    public final ItemStackRenderState[] cargo = {
            new ItemStackRenderState(), new ItemStackRenderState(),
            new ItemStackRenderState(), new ItemStackRenderState()
    };
    public int cargoCount;
}
