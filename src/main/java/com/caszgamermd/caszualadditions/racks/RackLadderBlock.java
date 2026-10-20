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
    @Override protected boolean canSurvive(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.LevelReader level,
            net.minecraft.core.BlockPos pos){
        // A rack is a skeletal steel frame, not a solid cube. Vanilla's
        // sturdy-face test rejects attaching ladders to its thin beams,
        // so specifically allow anchoring to any of its 12 frame cells.
        net.minecraft.core.BlockPos anchor=pos.relative(
                state.getValue(FACING).getOpposite());
        return level.getBlockState(anchor).getBlock() instanceof RackBlock
                ||super.canSurvive(state,level,pos);
    }
}
