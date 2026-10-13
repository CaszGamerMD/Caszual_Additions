package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The rack stays in place. Two workers pull its plastic-wrapped pallet into
 * the aisle, where the other two can join the lift. Elevated racks use the
 * SAME aisle below them; workers never try to climb the steel rack frame.
 *
 * Reject blocked extraction lanes and missing floors rather than handing a
 * loaded pallet to an entity inside a wall or spawning an invisible lift.
 */
public final class RackPickupPlan {
    public static final int MAX_DROP = 12;
    public static final int PULL_TICKS = 40;
    private final BlockPos root;
    private final int floorY;
    private final Direction facing;

    private RackPickupPlan(BlockPos root,int floorY,Direction facing){
        this.root=root.immutable();
        this.floorY=floorY;
        this.facing=facing;
    }

    public int floorY(){return floorY;}
    public int height(){return root.getY()-floorY;}
    public Direction facing(){return facing;}

    /** First pair gets both outer edges of the pallet, second pair waits
     * farther back and only joins after the pallet clears the rack frame. */
    public BlockPos goal(int worker, boolean pulled){
        int column=worker%2;
        int depth=worker<2 ? 2 : (pulled ? 3 : 4);
        return RackBlock.cell(root,facing,column)
                .relative(facing,depth).atY(floorY);
    }

    public BlockPos rigCenter(){
        return RackBlock.cell(root,facing,0).relative(facing,2)
                .relative(facing.getCounterClockWise(),1).atY(floorY);
    }

    public static RackPickupPlan find(ServerLevel level,RackBlockEntity rack){
        BlockPos root=rack.getBlockPos();
        Direction direction=rack.facing();
        // Pull out horizontally FIRST: need a two-wide clear channel from
        // the open rack face. A raised load then lowers within that channel.
        for(int col=0;col<2;col++)
            for(int depth=1;depth<=2;depth++)
                for(int y=root.getY();y<=root.getY()+1;y++){
                    BlockPos position=RackBlock.cell(root,direction,col)
                            .relative(direction,depth).atY(y);
                    if(!air(level,position))return null;
                }
        for(int y=root.getY();y>=root.getY()-MAX_DROP;y--){
            boolean clear=true;
            for(int col=0;col<2&&clear;col++)
                for(int depth=1;depth<=4&&clear;depth++){
                    BlockPos base=RackBlock.cell(root,direction,col)
                            .relative(direction,depth).atY(y);
                    if(!StorageGolemReturnPath.walkable(level,base))clear=false;
                }
            if(!clear)continue;
            // The elevated pallet needs a clear vertical lowering shaft
            // at the outside edge, *not* a route over or through shelves.
            for(int col=0;col<2&&clear;col++)
                for(int height=y+2;height<=root.getY()+1&&clear;height++){
                    BlockPos at=RackBlock.cell(root,direction,col)
                            .relative(direction,2).atY(height);
                    if(!air(level,at))clear=false;
                }
            if(clear)return new RackPickupPlan(root,y,direction);
        }
        return null;
    }
    private static boolean air(ServerLevel level,BlockPos pos){
        return !level.isOutsideBuildHeight(pos)
                &&level.hasChunkAt(pos)
                &&level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();
    }
}
