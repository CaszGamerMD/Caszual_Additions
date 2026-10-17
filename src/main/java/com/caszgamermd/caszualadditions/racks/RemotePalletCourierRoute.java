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

    /** The pushed pallet is TWO blocks across, not three. Checking an
     * entire 3x3 neighborhood around EVERY center cell prevented the
     * courier from even leaving a normal Golem-shelf doorway. An actual
     * 2x2 clear quadrant is enough to orient the upright pallet to either
     * side of its pushing Golem; all four constituent tiles must have
     * valid floor and two-block headroom. The center must be valid too.
     * This retains rack, stack and PTP collision protection without
     * inventing a third-block-wide invisible wall around the courier. */
    public static boolean clear(ServerLevel level,BlockPos center){
        if(!StorageGolemReturnPath.walkable(level,center))return false;
        for(int xSide:new int[]{-1,1})for(int zSide:new int[]{-1,1}){
            if(StorageGolemReturnPath.walkable(level,center.offset(xSide,0,0))
                    &&StorageGolemReturnPath.walkable(level,center.offset(0,0,zSide))
                    &&StorageGolemReturnPath.walkable(level,center.offset(xSide,0,zSide)))
                return true;
        }
        return false;
    }

    /** Follow checked cardinal cells on the SAME Y whenever possible.
     * For distant requests the existing rolling 36-cell warehouse planner
     * gives a forward leg; never replace a failed path with an unverified
     * straight-line instruction or a random walk around the target. */
    public static List<BlockPos> route(ServerLevel level,BlockPos start,
            BlockPos destination,BlockPos avoid,boolean pushing){
        Predicate<BlockPos> passable=pos->(pushing?clear(level,pos)
                :StorageGolemReturnPath.walkable(level,pos))
                // Mark the WHOLE pushed-pallet clearance envelope as
                // blocked around a stalled waypoint, not just one center
                // cell that the next plan can graze and collide with.
                &&(avoid.equals(BlockPos.ZERO)
                    ||!(pushing?Math.abs(pos.getX()-avoid.getX())<=1
                            &&Math.abs(pos.getZ()-avoid.getZ())<=1
                            &&pos.getY()==avoid.getY()
                        :pos.equals(avoid)));
        List<BlockPos> exact=null;
        if(start.getY()==destination.getY()
                &&Math.abs(start.getX()-destination.getX())<=SEARCH_RADIUS
                &&Math.abs(start.getZ()-destination.getZ())<=SEARCH_RADIUS)
            exact=StorageGolemReturnPath.steps(start,destination,passable);
        if(exact!=null)return exact;
        // When the destination is already nearby and on this floor,
        // a short PARTIAL route is worse than waiting: the Golem would
        // repeatedly approach the same blocked wall, turn and circle.
        // Long trips may still accept rolling 36-cell forward legs.
        if(start.getY()==destination.getY()
                &&Math.abs(start.getX()-destination.getX())<=36
                &&Math.abs(start.getZ()-destination.getZ())<=36){
            // Same-Y full path failed: NOW and only now consider a
            // stair/ramp. Require that this fallback really reaches
            // the destination; a "best effort" partial route to a
            // nearby wall is the exact circling bug being fixed.
            var stairs=WarehousePreferredPath.leg(
                    start,destination,passable,avoid);
            return stairs!=null&&!stairs.isEmpty()
                    &&stairs.get(stairs.size()-1).equals(destination)
                    ?stairs:null;
        }
        return WarehousePreferredPath.leg(start,destination,passable,avoid);
    }

    /** The wrapped 2x2 block is the ITEM target, not the GOLEM waypoint.
     * Try every outer side; rank actual *route length*, separating the
     * courier from the THREE independently arriving pickup Golems.
     * No candidate is selected unless a checked route exists. */
    public static BlockPos chooseServiceAisle(ServerLevel level,
            BlockPos from,BlockPos pallet,BlockPos trioRally,BlockPos excluded){
        BlockPos chosen=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        // Evaluate nearer service aisles first. On long journeys the
        // 36-cell rolling planner is expensive; a route is only computed
        // for a candidate that already has enough 3-wide clearance.
        java.util.ArrayList<BlockPos> candidates=new java.util.ArrayList<>();
        for(Direction face:Direction.Plane.HORIZONTAL)
            for(int distance=4;distance<=7;distance++)
                candidates.add(pallet.relative(face,distance));
        candidates.sort(java.util.Comparator.comparingDouble(from::distSqr));
        for(int pass=0;pass<2;pass++){
            for(BlockPos candidate:candidates){
                int distance=(int)Math.round(Math.sqrt(candidate.distSqr(pallet)));
                if(!excluded.equals(BlockPos.ZERO)
                        &&candidate.distSqr(excluded)<9)continue;
                if(!clear(level,candidate))continue;
                // Prefer to stay away from the three-Golem assembly.
                // However a warehouse with only ONE accessible side must
                // still be functional: on the second pass, allow its outer
                // aisle and wait there until the lifters move aside.
                if(pass==0&&!trioRally.equals(BlockPos.ZERO)
                        &&candidate.distSqr(trioRally)<36)continue;
                List<BlockPos> path=route(level,from,candidate,BlockPos.ZERO,true);
                if(path==null)continue;
                double cost=path.size()
                        +.018*Math.abs(candidate.getX()-from.getX())
                        +.018*Math.abs(candidate.getZ()-from.getZ())
                        +distance*.2;
                if(cost<score){chosen=candidate;score=cost;}
            }
            if(!chosen.equals(BlockPos.ZERO))break;
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
