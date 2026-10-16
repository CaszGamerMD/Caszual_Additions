package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * The shelf's horizontal FACING is the physical FRONT OPENING,
 * opposite the direction its placer looked. Its top is NEVER a door.
 */
public final class StorageGolemShelfDoor {
    private StorageGolemShelfDoor(){}

    public static Direction front(StorageGolemShelfEntity shelf){
        return shelf.getBlockState().getValue(StorageGolemShelfBlock.FACING);
    }

    /** Each worker emerges at floor level through the FRONT opening.
     * A second worker can take the next free square in that same aisle;
     * it may not materialize in the roof, rear or wall.
     * Unavailable or blocked exits postpone dispatch without spending a shelf token.
     */
    public static BlockPos exit(ServerLevel level,StorageGolemShelfEntity shelf){
        BlockPos root=shelf.getBlockPos();
        Direction facing=front(shelf);
        for(int depth=1;depth<=3;depth++){
            BlockPos candidate=root.relative(facing,depth);
            if(!StorageGolemReturnPath.walkable(level,candidate))continue;
            if(!level.getEntitiesOfClass(StorageGolem.class,
                    new AABB(candidate).inflate(.12,0,.12),
                    robot->!robot.isRemoved()).isEmpty())continue;
            boolean clear=true;
            for(int step=1;step<depth;step++){
                if(!StorageGolemReturnPath.walkable(level,
                        root.relative(facing,step))){clear=false;break;}
            }
            if(clear)return candidate;
        }
        return null;
    }

    public static void emerge(StorageGolem worker,BlockPos exit,Direction facing){
        worker.setPos(exit.getX()+.5,exit.getY(),exit.getZ()+.5);
        worker.setYRot(facing.toYRot());
        worker.setYHeadRot(facing.toYRot());
        worker.setYBodyRot(facing.toYRot());
    }
}
