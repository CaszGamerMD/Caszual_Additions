package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/** Only the real number of docked workers and shelf orientation are needed.
 * All six textured parts are rendered from the shared Storage Golem mesh. */
public final class StorageGolemShelfRenderState extends BlockEntityRenderState {
    public int docked;
    public Direction facing=Direction.NORTH;
}
