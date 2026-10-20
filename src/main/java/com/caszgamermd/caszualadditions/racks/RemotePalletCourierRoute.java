package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
        return route(level,start,destination,avoid,pushing,BlockPos.ZERO);
    }
    public static List<BlockPos> route(ServerLevel level,BlockPos start,
            BlockPos destination,BlockPos avoid,boolean pushing,
            BlockPos crewRally){
        Predicate<BlockPos> passable=pos->(pushing?clear(level,pos)
                :StorageGolemReturnPath.walkable(level,pos))
                // Mark the WHOLE pushed-pallet clearance envelope as
                // blocked around a stalled waypoint, not just one center
                // cell that the next plan can graze and collide with.
                // Don't push a 2-wide pallet through the three independent
                // lifters' 3x3 assembly. The courier must remain clear until
                // they physically lift the target and form the shared rig.
                &&(crewRally.equals(BlockPos.ZERO)||!pushing
                    ||pos.distSqr(crewRally)>=9)
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
                    start,destination,passable,avoid,
                    pos->WarehouseTerrain.standingY(level,pos));
            return stairs!=null&&!stairs.isEmpty()
                    &&stairs.get(stairs.size()-1).equals(destination)
                    ?stairs:null;
        }
        return WarehousePreferredPath.leg(start,destination,passable,avoid,
                pos->WarehouseTerrain.standingY(level,pos));
    }

    /**
     * The saved remote target is the CORNER of a rotating 2x2 pallet, not
     * its geometric center. Resolve the four ACTUAL footprint cells first.
     * Once the old pallet has been lifted its block states are gone, so use
     * the loaded rig's saved old orientation with the courier's replacement
     * pallet block. Never infer a different target from a nearby pallet.
     */
    public static BlockPos[] targetFootprint(ServerLevel level,BlockPos root,
            MovingPallet rig,ItemStack replacement){
        BlockState state=level.getBlockState(root);
        if(!(state.getBlock() instanceof PalletBlock)){
            if(rig==null||!rig.loaded()
                    ||!(replacement.getItem() instanceof BlockItem item)
                    ||!(item.getBlock() instanceof PalletBlock block))
                return null;
            state=block.defaultBlockState()
                    .setValue(PalletBlock.PART,0)
                    .setValue(PalletBlock.FACING,rig.remoteOldFacing())
                    .setValue(PalletBlock.LEGACY,rig.remoteOldLegacy());
        }
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
        int minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(int part=0;part<4;part++){
            BlockPos pos=PalletBlock.partPos(root,part,state);
            minX=Math.min(minX,pos.getX());maxX=Math.max(maxX,pos.getX());
            minZ=Math.min(minZ,pos.getZ());maxZ=Math.max(maxZ,pos.getZ());
        }
        return new BlockPos[]{
                new BlockPos(minX,root.getY(),minZ),
                new BlockPos(maxX,root.getY(),maxZ)};
    }

    /** Valid waiting/delivery position is TWO or THREE cells outside the
     * actual assigned pallet's footprint on one cardinal face. This works
     * even when the target root is the NE/SW/etc corner, and explicitly
     * rejects the 4-7-block staging points used by older builds.
     *
     * Requiring a true perimeter service position also prevents completing
     * a pallet placement remotely from beside an unrelated nearby pallet.
     */
    public static boolean nearTargetService(BlockPos courier,
            BlockPos[] footprint){
        if(footprint==null)return false;
        BlockPos min=footprint[0],max=footprint[1];
        if(Math.abs(courier.getY()-min.getY())>1)return false;
        int x=courier.getX(),z=courier.getZ();
        boolean northSouth=x>=min.getX()&&x<=max.getX()
                &&(min.getZ()-z>=2&&min.getZ()-z<=3
                    ||z-max.getZ()>=2&&z-max.getZ()<=3);
        boolean eastWest=z>=min.getZ()&&z<=max.getZ()
                &&(min.getX()-x>=2&&min.getX()-x<=3
                    ||x-max.getX()>=2&&x-max.getX()<=3);
        return northSouth||eastWest;
    }

    /** No service target can be valid just because another, unrelated
     * pallet happens to be near the golem's current position. */
    public static boolean validServiceAisle(ServerLevel level,BlockPos candidate,
            BlockPos[] footprint,BlockPos crewRally){
        return nearTargetService(candidate,footprint)
                &&clear(level,candidate)
                &&(crewRally.equals(BlockPos.ZERO)
                    ||candidate.distSqr(crewRally)>=9);
    }

    /** Choose an accessible perimeter spot around the fixed old 2x2 deck,
     * never around the courier or the nearest unrelated pallet. A courier
     * moved by a player to an already-valid service tile can simply stop.
     */
    public static BlockPos chooseServiceAisle(ServerLevel level,
            BlockPos from,BlockPos pallet,BlockPos trioRally,BlockPos excluded,
            MovingPallet rig,ItemStack replacement){
        BlockPos[] footprint=targetFootprint(level,pallet,rig,replacement);
        if(footprint==null)return BlockPos.ZERO;
        if(validServiceAisle(level,from,footprint,trioRally)
                &&(excluded.equals(BlockPos.ZERO)||from.distSqr(excluded)>=9))
            return from;

        BlockPos min=footprint[0],max=footprint[1];
        // Same height first, then one legal rise/drop on uneven terrain.
        // Stay close: TWO or THREE blocks outside the actual deck edge,
        // rather than FOUR to SEVEN blocks from its arbitrary root corner.
        for(int dy:new int[]{0,1,-1}){
            java.util.ArrayList<BlockPos> candidates=new java.util.ArrayList<>();
            int y=pallet.getY()+dy;
            for(int distance=2;distance<=3;distance++){
                for(int x=min.getX();x<=max.getX();x++){
                    candidates.add(new BlockPos(x,y,min.getZ()-distance));
                    candidates.add(new BlockPos(x,y,max.getZ()+distance));
                }
                for(int z=min.getZ();z<=max.getZ();z++){
                    candidates.add(new BlockPos(min.getX()-distance,y,z));
                    candidates.add(new BlockPos(max.getX()+distance,y,z));
                }
            }
            candidates.sort(java.util.Comparator.comparingDouble(from::distSqr));
            BlockPos chosen=BlockPos.ZERO;
            double score=Double.POSITIVE_INFINITY;
            for(BlockPos candidate:candidates){
                if(!validServiceAisle(level,candidate,footprint,trioRally))continue;
                if(!excluded.equals(BlockPos.ZERO)
                        &&candidate.distSqr(excluded)<9)continue;
                List<BlockPos> path=route(level,from,candidate,
                        BlockPos.ZERO,true,trioRally);
                if(path==null)continue;
                double cost=path.size()
                        +.02*Math.abs(candidate.getX()-from.getX())
                        +.02*Math.abs(candidate.getZ()-from.getZ());
                if(cost<score){score=cost;chosen=candidate;}
            }
            if(!chosen.equals(BlockPos.ZERO))return chosen;
        }
        return BlockPos.ZERO;
    }
    /** After installing the replacement the courier is empty handed.
     * Join from a reachable cell beside the loaded rig, not inside it.
     * Keep this side fixed while walking to avoid endlessly orbiting. */
    public static BlockPos chooseJoinAisle(ServerLevel level,BlockPos from,
            BlockPos rig){
        BlockPos chosen=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        // A loaded team may have moved ONE block up or down while
        // clearing the old pallet. A fixed same-Y join cell can leave
        // the courier standing just below it forever. Evaluate genuinely
        // reachable neighbor cells at the rig's height and +/- one step.
        for(int dy:new int[]{0,-1,1}){
            for(Direction face:Direction.Plane.HORIZONTAL){
                BlockPos cell=rig.relative(face,2).offset(0,dy,0);
                if(!StorageGolemReturnPath.walkable(level,cell))continue;
                // Every candidate must actually be within the rig's join
                // distance. Never plan a route to a waypoint that cannot
                // trigger the final ownership handoff.
                double dx=cell.getX()+.5-(rig.getX()+.5);
                double dz=cell.getZ()+.5-(rig.getZ()+.5);
                if(dx*dx+dz*dz+dy*dy>9.0)continue;
                List<BlockPos> path=route(level,from,cell,BlockPos.ZERO,false);
                if(path==null)continue;
                double value=path.size()+Math.abs(dy)*2;
                if(value<score){score=value;chosen=cell;}
            }
        }
        return chosen;
    }
}
