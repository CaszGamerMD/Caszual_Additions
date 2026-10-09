package com.caszgamermd.caszualadditions.plushie;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieRenderState extends BlockEntityRenderState {
    public @Nullable ResolvableProfile profile;
    public Direction facing = Direction.NORTH;
    public int pose;
    public final net.minecraft.client.renderer.item.ItemStackRenderState prop = new net.minecraft.client.renderer.item.ItemStackRenderState();
}
