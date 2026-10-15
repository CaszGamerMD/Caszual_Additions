package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Four-direction, same-floor return paths for real Storage Golems.
 *
 * Vanilla ground navigation can choose a one-block ascent even when a
 * perfectly level detour exists; using a shelf-top destination actively
 * encourages that. Plan only standable cells at shelf-floor elevation and
 * issue turn-by-turn corridor waypoints. No jumping or forced chunk loads.
 * The bounded route is recomputed if the warehouse layout changes.
 */
public final class StorageGolemReturnPath {
    public static final int MAX_VISITED=12000;
    private static final int DETOUR_MARGIN=32;
    private StorageGolemReturnPath(){}

    public static boolean walkable(ServerLevel level,BlockPos feet){
        if(!level.hasChunkAt(feet)||!level.hasChunkAt(feet.above())
                ||!level.hasChunkAt(feet.below()))return false;
        BlockState feetState=level.getBlockState(feet);
        BlockState headState=level.getBlockState(feet.above());
        BlockPos ground=feet.below();
        BlockState below=level.getBlockState(ground);
        // Don't treat the empty-looking interior of the rack as traversable,
        // and never take a "shortcut" across the upper steel frame.
        // All rack segments count as full obstacles for warehouse paths.
        if(feetState.getBlock() instanceof RackBlock
                ||headState.getBlock() instanceof RackBlock
                ||below.getBlock() instanceof RackBlock)return false;
        return feetState.getCollisionShape(level,feet).isEmpty()
                &&headState.getCollisionShape(level,feet.above()).isEmpty()
                &&!below.getCollisionShape(level,ground).isEmpty();
    }

    /**
     * A shelf is occupied by a solid block, not an air destination. A Golem
     * can check in from a clear cardinal aisle cell one OR two blocks away.
     * The two-cell option prevents four returning workers from queueing in a
     * single narrow docking square. Never count a blocked inner cell as an
     * accessible dock from two cells away (e.g. through a wall).
     *
     * The caller supplies a shelf position at the GOLEM'S standing Y level.
     * The actual shelf block can be one block above or below that level.
     */
    public static boolean canDock(BlockPos feet,BlockPos shelf,
            Predicate<BlockPos> passable){
        if(feet.getY()!=shelf.getY())return false;
        // Cardinal aisle approaches: either worker may check in from one,
        // two or three clear cells away. Both shelf slots stay independent.
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos cell=shelf;
            for(int depth=1;depth<=3;depth++){
                cell=cell.relative(side);
                if(!passable.test(cell))break;
                if(feet.equals(cell))return true;
            }
        }
        // A 2-wide aisle can hold returning workers side-by-side rather
        // than driving both into the exact same entrance square. Both
        // intermediate tiles must be traversable; never dock diagonally
        // through the corner of a solid wall.
        for(Direction side:Direction.Plane.HORIZONTAL){
            for(Direction other:Direction.Plane.HORIZONTAL){
                if(side.getAxis()==other.getAxis())continue;
                BlockPos first=shelf.relative(side);
                BlockPos second=shelf.relative(other);
                BlockPos corner=first.relative(other);
                if(feet.equals(corner)&&passable.test(feet)
                        &&passable.test(first)&&passable.test(second))
                    return true;
            }
        }
        return false;
    }

    /**
     * Docking is a reachable SERVICE AREA, not a single center-of-block
     * waypoint. Real golems stop short of a shelf when another worker
     * occupies the intended square, or when walking around the corner of
     * a bank of linked shelves. They may check in from anywhere within
     * four horizontal blocks of their shelf, provided a short CARDINAL
     * path leads there through truly walkable aisle cells.
     *
     * The shelf itself is never crossed. There are no jumps, no through-wall
     * diagonal shortcuts and no docking through solid rack frames.
     */
    public static boolean canDockNearby(BlockPos feet,BlockPos shelf,
            Predicate<BlockPos> passable){
        if(feet.getY()!=shelf.getY())return false;
        int dx=feet.getX()-shelf.getX(),dz=feet.getZ()-shelf.getZ();
        if(dx*dx+dz*dz>16)return false;
        if(canDock(feet,shelf,passable))return true;
        // A standable goal is important: never grant docking from inside a
        // collision block. Occupied worker cells are deliberately allowed.
        if(!passable.test(feet))return false;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Map<BlockPos,Integer> depth=new HashMap<>();
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos neighbor=shelf.relative(side);
            if(passable.test(neighbor)){
                depth.put(neighbor,1);
                queue.addLast(neighbor);
            }
        }
        while(!queue.isEmpty()){
            BlockPos cell=queue.removeFirst();
            int steps=depth.get(cell);
            if(cell.equals(feet))return true;
            if(steps>=5)continue;
            for(Direction side:Direction.Plane.HORIZONTAL){
                BlockPos next=cell.relative(side);
                int x=next.getX()-shelf.getX(),z=next.getZ()-shelf.getZ();
                if(x*x+z*z>16||next.equals(shelf)
                        ||depth.containsKey(next)||!passable.test(next))continue;
                depth.put(next,steps+1);
                queue.addLast(next);
            }
        }
        return false;
    }

    /** Returns turn/end waypoints with the start omitted. No diagonal
     * corner cutting, ascents or leaps are permitted. A null result means
     * there is no complete same-level route through currently loaded blocks. */
    public static List<BlockPos> route(BlockPos from,BlockPos shelf,
            Predicate<BlockPos> passable){
        if(from.getY()!=shelf.getY())return null;
        Set<BlockPos> goals=new HashSet<>();
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos inner=shelf.relative(side);
            if(!passable.test(inner))continue;
            BlockPos cell=inner;
            goals.add(cell);
            for(int depth=2;depth<=3;depth++){
                cell=cell.relative(side);
                if(!passable.test(cell))break;
                goals.add(cell);
            }
        }
        for(Direction side:Direction.Plane.HORIZONTAL)
            for(Direction other:Direction.Plane.HORIZONTAL){
                if(side.getAxis()==other.getAxis())continue;
                BlockPos first=shelf.relative(side);
                BlockPos second=shelf.relative(other);
                BlockPos corner=first.relative(other);
                if(passable.test(first)&&passable.test(second)
                        &&passable.test(corner))goals.add(corner);
        }
        if(goals.isEmpty())return null;
        return search(from,shelf,passable,goals);
    }

    /** Cell-by-cell same-floor route for steering. Adjacent targets
     * prevent vanilla navigation from cutting across shelf corners. */
    public static List<BlockPos> steps(BlockPos from,BlockPos target,
            Predicate<BlockPos> passable){
        if(from.getY()!=target.getY()||!passable.test(target))return null;
        return search(from,target,passable,Set.of(target),false);
    }
    public static List<BlockPos> stepsHome(BlockPos from,BlockPos shelf,
            Predicate<BlockPos> passable){
        if(from.getY()!=shelf.getY())return null;
        java.util.Set<BlockPos> goals=new java.util.LinkedHashSet<>();
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos cell=shelf;
            for(int depth=1;depth<=3;depth++){
                cell=cell.relative(side);
                if(!passable.test(cell))break;
                goals.add(cell);
            }
        }
        for(Direction side:Direction.Plane.HORIZONTAL)
            for(Direction other:Direction.Plane.HORIZONTAL){
                if(side.getAxis()==other.getAxis())continue;
                BlockPos first=shelf.relative(side),second=shelf.relative(other);
                BlockPos corner=first.relative(other);
                if(passable.test(first)&&passable.test(second)
                        &&passable.test(corner))goals.add(corner);
            }
        if(goals.isEmpty())return null;
        return search(from,shelf,passable,goals,false);
    }
    /** Go directly to a validated workstation/aisle tile rather than
     * targeting the solid block above it. No diagonal movement or jumps. */
    public static List<BlockPos> routeTo(BlockPos from,BlockPos target,
            Predicate<BlockPos> passable){
        if(from.getY()!=target.getY()||!passable.test(target))return null;
        return search(from,target,passable,Set.of(target));
    }

    private static List<BlockPos> search(BlockPos from,BlockPos shelf,
            Predicate<BlockPos> passable,Set<BlockPos> goals){
        return search(from,shelf,passable,goals,true);
    }

    private static List<BlockPos> search(BlockPos from,BlockPos shelf,
            Predicate<BlockPos> passable,Set<BlockPos> goals,boolean cornersOnly){
        if(goals.contains(from))return List.of();
        int minX=Math.min(from.getX(),shelf.getX())-DETOUR_MARGIN;
        int maxX=Math.max(from.getX(),shelf.getX())+DETOUR_MARGIN;
        int minZ=Math.min(from.getZ(),shelf.getZ())-DETOUR_MARGIN;
        int maxZ=Math.max(from.getZ(),shelf.getZ())+DETOUR_MARGIN;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Map<BlockPos,BlockPos> parents=new HashMap<>();
        parents.put(from,from);
        queue.add(from);
        BlockPos finish=null;
        while(!queue.isEmpty()&&parents.size()<MAX_VISITED){
            BlockPos p=queue.removeFirst();
            if(goals.contains(p)){finish=p;break;}
            for(Direction side:Direction.Plane.HORIZONTAL){
                BlockPos next=p.relative(side);
                if(next.getX()<minX||next.getX()>maxX
                        ||next.getZ()<minZ||next.getZ()>maxZ
                        ||parents.containsKey(next)||!passable.test(next))continue;
                parents.put(next,p);
                queue.addLast(next);
            }
        }
        if(finish==null)return null;
        ArrayList<BlockPos> cells=new ArrayList<>();
        for(BlockPos p=finish;!p.equals(from);p=parents.get(p))cells.add(p);
        Collections.reverse(cells);
        if(!cornersOnly)return cells;
        // Only navigate to corners. Every straight corridor was checked
        // cell-by-cell for 2-block headroom and supporting ground.
        ArrayList<BlockPos> turns=new ArrayList<>();
        for(int i=1;i<cells.size();i++){
            BlockPos previous=i==1?from:cells.get(i-2);
            BlockPos here=cells.get(i-1),next=cells.get(i);
            int dx=here.getX()-previous.getX();
            int dz=here.getZ()-previous.getZ();
            if(dx!=next.getX()-here.getX()
                    ||dz!=next.getZ()-here.getZ())turns.add(here);
        }
        turns.add(finish);
        return turns;
    }
}
