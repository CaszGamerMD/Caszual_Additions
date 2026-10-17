package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Navigation exclusively for the Golem physically PUSHING a full-width
 * upright empty pallet. This is not a bare-Golem path: the load needs a
 * three-wide corridor, and its final unloading service aisle must be
 * selected by REACHABILITY, not by straight-line proximity.
 *
 * The walker's caller keeps the chosen goal and route across ticks. The
 * candidate list is evaluated only on a new task/obstruction, not on every
 * tick; constantly picking a newly "nearest" side caused orbiting.
 */
public final class RemotePalletCourierRoute {
    private RemotePalletCourierRoute(){}
    private static final int SEARCH_RADIUS=56;

    /** Conservatively reserve one full block on either side of the robot.
     * The real pushed pallet is nearly 2 blocks wide. No shortcuts between
     * racks, through stacked pallets or across partly filled PTPs. */
    public static boolean clear(ServerLevel level,BlockPos center){
        if(!StorageGolemReturnPath.walkable(level,center))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            if(!StorageGolemReturnPath.walkable(level,center.offset(x,0,z)))
                return false;
        return true;
    }

    /** Follow checked cardinal cells on the SAME Y whenever possible.
     * For distant requests the existing rolling 36-cell warehouse planner
     * gives a forward leg; never replace a failed path with an unverified
     * straight-line instruction or a random walk around the target. */
    public static List<BlockPos> route(ServerLevel level,BlockPos start,
            BlockPos destination,BlockPos avoid,boolean pushing){
        Predicate<BlockPos> passable=pos->(pushing?clear(level,pos)
                :StorageGolemReturnPath.walkable(level,pos))
                &&(avoid.equals(BlockPos.ZERO)||!pos.equals(avoid));
        List<BlockPos> exact=null;
        if(start.getY()==destination.getY()
                &&Math.abs(start.getX()-destination.getX())<=SEARCH_RADIUS
                &&Math.abs(start.getZ()-destination.getZ())<=SEARCH_RADIUS)
            exact=StorageGolemReturnPath.steps(start,destination,passable);
        if(exact!=null)return exact;
        return WarehousePreferredPath.leg(start,destination,passable,avoid);
    }

    /** The wrapped 2x2 block is the ITEM target, not the GOLEM waypoint.
     * Try every outer side; rank actual *route length*, separating the
     * courier from the THREE independently arriving pickup Golems.
     * No candidate is selected unless a checked route exists. */
    public static BlockPos chooseServiceAisle(ServerLevel level,
            BlockPos from,BlockPos pallet,BlockPos trioRally){
        BlockPos chosen=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        for(Direction face:Direction.Plane.HORIZONTAL)
            for(int distance=4;distance<=7;distance++){
                BlockPos candidate=pallet.relative(face,distance);
                if(!clear(level,candidate))continue;
                // The three loaded workers' own 3x3 assembly area must
                // remain clear. Don't try to push a 2-wide deck through it.
                if(!trioRally.equals(BlockPos.ZERO)
                        &&candidate.distSqr(trioRally)<36)continue;
                List<BlockPos> path=route(level,from,candidate,BlockPos.ZERO,true);
                if(path==null)continue;
                // A rolling route may not terminate at a distant target.
                // Keep a single service goal after choosing it, and then
                // refine that SAME goal as more chunks are loaded.
                double cost=path.size()
                        +.018*Math.abs(candidate.getX()-from.getX())
                        +.018*Math.abs(candidate.getZ()-from.getZ())
                        +distance*.2;
                if(cost<score){chosen=candidate;score=cost;}
            }
        return chosen;
    }
    /** After installing the replacement the courier is empty handed.
     * Join from a reachable cell beside the loaded rig, not inside it.
     * Keep this side fixed while walking to avoid endlessly orbiting. */
    public static BlockPos chooseJoinAisle(ServerLevel level,BlockPos from,
            BlockPos rig){
        BlockPos chosen=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        for(Direction face:Direction.Plane.HORIZONTAL){
            BlockPos cell=rig.relative(face,2);
            if(!StorageGolemReturnPath.walkable(level,cell))continue;
            List<BlockPos> path=route(level,from,cell,BlockPos.ZERO,false);
            if(path==null)continue;
            double value=path.size();
            if(value<score){score=value;chosen=cell;}
        }
        return chosen;
    }
}
