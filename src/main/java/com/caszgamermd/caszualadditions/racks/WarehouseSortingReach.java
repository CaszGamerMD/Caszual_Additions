package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** A storage worker can SORT within two blocks vertically of its feet,
 * but cannot reach through a floor or perform a 12-block crane operation.
 * Elevated racks need a physical accessible stair/scaffold/ladder landing.
 */
public final class WarehouseSortingReach {
    public static final int BLOCKS=2;
    private WarehouseSortingReach(){}
    public static boolean withinReach(BlockPos feet,RackBlockEntity rack){
        BlockPos root=rack.getBlockPos();
        int dy=root.getY()-feet.getY();
        if(Math.abs(dy)>BLOCKS)return false;
        BlockPos a=RackBlock.cell(root,rack.facing(),0);
        BlockPos b=RackBlock.cell(root,rack.facing(),1);
        return feet.distSqr(a)<=20||feet.distSqr(b)<=20;
    }
    public static BlockPos access(ServerLevel level,RackBlockEntity rack){
        BlockPos root=rack.getBlockPos();
        for(int y=root.getY();y>=root.getY()-BLOCKS;y--){
            for(int distance=1;distance<=3;distance++)
                for(int column=0;column<2;column++){
                    BlockPos at=RackBlock.cell(root,rack.facing(),column)
                            .relative(rack.facing(),distance);
                    BlockPos feet=new BlockPos(at.getX(),y,at.getZ());
                    if(StorageGolemReturnPath.walkable(level,feet)
                            &&withinReach(feet,rack))return feet;
                }
        }
        return null;
    }
    public static boolean accessible(ServerLevel level,RackBlockEntity rack){
        return access(level,rack)!=null;
    }
    /** Cargo crews without forklifts may install a pallet only into a rack
     * supported by a real floor, never directly above another rack module. */
    public static boolean groundRack(ServerLevel level,RackBlockEntity rack){
        BlockPos pos=rack.getBlockPos();
        for(int h=1;h<=12;h++){
            BlockPos under=pos.below(h);
            if(!level.hasChunkAt(under))return false;
            if(level.getBlockState(under).getBlock() instanceof RackBlock)
                return false;
            // Stop at a genuine solid floor, including concrete.
            if(level.getBlockState(under).isCollisionShapeFullBlock(level,under))
                return true;
        }
        return false;
    }
}
