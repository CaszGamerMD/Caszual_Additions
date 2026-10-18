package com.caszgamermd.caszualadditions.racks;

import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Side-mounted climbable rack ladder. Minecraft's vanilla ladder
 * movement allows climbing, side dismounts and walking backwards off it.
 * The texture is industrial graphite steel rather than wooden rungs. */
public final class RackLadderBlock extends LadderBlock {
    public RackLadderBlock(BlockBehaviour.Properties properties){
        super(properties);
    }
}
