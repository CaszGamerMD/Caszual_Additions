package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Safe, finite physical warehouse routes. Prefer a same-height path; allow
 * occasional, collision-checked one-block climbs/drops only when no level
 * route reaches the requested goal. Each height change has a large penalty.
 *
 * Long-distance carriers compute 36-block legs in currently loaded chunks,
 * keeping the SAME persisted job and recomputing on obstruction/reload.
 */
public final class WarehousePreferredPath {
    private WarehousePreferredPath(){}
    public static final int RADIUS=36,MAX_CELLS=6500;
    private static final int STEP_HEIGHT_COST=14,MAX_ELEVATION_FROM_START=12;
    private record Node(BlockPos pos,int cost,int estimate){}
    private static int heuristic(BlockPos p,BlockPos target){
        return Math.abs(p.getX()-target.getX())
                +Math.abs(p.getZ()-target.getZ())
                +Math.abs(p.getY()-target.getY())*20;
    }
    public static java.util.List<BlockPos> leg(BlockPos start,BlockPos target,
            Predicate<BlockPos> passable,BlockPos blocked){
        return leg(start,target,passable,blocked,p->p.getY());
    }
    /** Height-aware path for the actual world. A change of one integer
     * BlockPos Y between a full block and a bottom slab is only 1/2 block,
     * and must not incur the usual costly one-block climb penalty. */
    public static java.util.List<BlockPos> leg(BlockPos start,BlockPos target,
            Predicate<BlockPos> passable,BlockPos blocked,
            java.util.function.ToDoubleFunction<BlockPos> standingY){
        // Keep world lookups bounded: the flat and terrain alternatives
        // share ONE request-local cache of loaded tile collision and
        // standing heights. Cache does not escape this search, so block
        // placement is reflected on the next replanning event.
        Map<BlockPos,Boolean> validCache=new HashMap<>();
        Map<BlockPos,Double> heightCache=new HashMap<>();
        Predicate<BlockPos> checked=p->validCache.computeIfAbsent(p,passable::test);
        java.util.function.ToDoubleFunction<BlockPos> height=p->
                heightCache.computeIfAbsent(p,standingY::applyAsDouble);
        // A flat route is cheap and normally ideal, but NOT mandatory:
        // a giant same-level detour should lose to two short, legal
        // one-block transitions. Compare its excess distance against the
        // exact height-change penalty (up + down), rather than forbidding
        // all climbs merely because a very long flat route exists.
        var flat=start.getY()==target.getY()
                ?find(start,target,checked,blocked,false,height):null;
        int direct=Math.abs(start.getX()-target.getX())
                +Math.abs(start.getZ()-target.getZ());
        if(flat!=null&&flat.size()<=direct+STEP_HEIGHT_COST*2)
            return flat;
        var terrain=find(start,target,checked,blocked,true,height);
        if(flat==null)return terrain;
        if(terrain==null)return flat;
        // A partial forward leg must not outrank an exact 3-D arrival.
        boolean flatExact=flat.getLast().equals(target);
        boolean terrainExact=terrain.getLast().equals(target);
        if(flatExact!=terrainExact)return flatExact?flat:terrain;
        int terrainCost=terrain.size();
        BlockPos previous=start;
        for(BlockPos cell:terrain){
            terrainCost+=(int)Math.round(Math.max(0,
                    Math.abs(height.applyAsDouble(cell)
                            -height.applyAsDouble(previous))-.501)
                    *STEP_HEIGHT_COST);
            previous=cell;
        }
        return flat.size()<=terrainCost?flat:terrain;
    }
    /**
     * Select the CHEAPEST truly reachable service square in a single
     * multi-destination Dijkstra search. The former approach performed
     * twelve separate A* searches, then chose a geometrically close cell
     * that could be blocked by racks, a stair, or another workbench.
     *
     * Only cardinal, loaded and collision-approved tiles are passed by
     * the caller. A one-block rise is allowed but costs more than flat
     * aisle movement. The first settled service cell is therefore the
     * globally shortest weighted route within the bounded search window.
     * The expensive search is run on task change / stall, never per tick.
     */
    public static BlockPos cheapestService(BlockPos from,
            java.util.Collection<BlockPos> options,
            Predicate<BlockPos> walkable,
            java.util.function.ToDoubleFunction<BlockPos> standingY){
        if(options==null||options.isEmpty())return null;
        var targets=new java.util.HashSet<BlockPos>();
        // Avoid repeated per-block server shape lookups while exploring
        // neighboring warehouse tiles. This cache lives ONLY for the
        // current bounded route search; it is never stale after a block
        // is placed or a door is changed.
        Map<BlockPos,Boolean> validCache=new HashMap<>();
        Map<BlockPos,Double> floorCache=new HashMap<>();
        for(BlockPos target:options)
            if(validCache.computeIfAbsent(target,walkable::test))
                targets.add(target);
        if(targets.isEmpty())return null;
        if(targets.contains(from))return from;
        PriorityQueue<Node> queue=new PriorityQueue<>(
                java.util.Comparator.comparingInt(Node::cost));
        Map<BlockPos,Integer> costs=new HashMap<>();
        queue.add(new Node(from,0,0));
        costs.put(from,0);
        int expanded=0;
        while(!queue.isEmpty()&&expanded<4000){
            Node node=queue.poll();
            if(node.cost()!=costs.getOrDefault(node.pos(),Integer.MAX_VALUE))
                continue;
            expanded++;
            BlockPos here=node.pos();
            if(targets.contains(here))return here;
            for(Direction dir:Direction.Plane.HORIZONTAL){
                BlockPos adjacent=here.relative(dir);
                for(int dy:new int[]{0,-1,1}){
                    BlockPos next=adjacent.offset(0,dy,0);
                    if(Math.abs(next.getX()-from.getX())>RADIUS
                            ||Math.abs(next.getZ()-from.getZ())>RADIUS
                            ||Math.abs(next.getY()-from.getY())>MAX_ELEVATION_FROM_START
                            ||!validCache.computeIfAbsent(next,walkable::test))continue;
                    double rise=Math.abs(floorCache.computeIfAbsent(next,
                            standingY::applyAsDouble)-floorCache.computeIfAbsent(
                                    here,standingY::applyAsDouble));
                    if(!Double.isFinite(rise)||rise>1.001)continue;
                    int newCost=node.cost()+10+(rise>.501?15:0);
                    if(newCost>=costs.getOrDefault(next,Integer.MAX_VALUE))continue;
                    costs.put(next,newCost);
                    queue.add(new Node(next,newCost,newCost));
                }
            }
        }
        // For very remote shelves, only the first 36-block route leg is
        // knowable without generating chunks. Aim at the nearest safe
        // service cell and re-evaluate once the worker enters search range.
        int nearest=Integer.MAX_VALUE;
        BlockPos remote=null;
        boolean distant=true;
        for(BlockPos cell:targets){
            int horizontal=Math.abs(cell.getX()-from.getX())
                    +Math.abs(cell.getZ()-from.getZ());
            if(horizontal<=RADIUS)distant=false;
            int score=horizontal*10+Math.abs(cell.getY()-from.getY())*20;
            if(score<nearest){nearest=score;remote=cell;}
        }
        return distant?remote:null;
    }

    private static java.util.List<BlockPos> find(BlockPos start,BlockPos target,
            Predicate<BlockPos> passable,BlockPos blocked,boolean allowElevation,
            java.util.function.ToDoubleFunction<BlockPos> standingY){
        int floor=start.getY();
        if(!allowElevation&&floor!=target.getY())return null;
        PriorityQueue<Node> open=new PriorityQueue<>(
                java.util.Comparator.comparingInt(Node::estimate));
        Map<BlockPos,Integer> costs=new HashMap<>();
        Map<BlockPos,BlockPos> parent=new HashMap<>();
        costs.put(start,0);
        parent.put(start,start);
        open.add(new Node(start,0,heuristic(start,target)));
        BlockPos best=null;
        int bestScore=Integer.MAX_VALUE;
        boolean exact=Math.abs(start.getX()-target.getX())<=RADIUS
                &&Math.abs(start.getZ()-target.getZ())<=RADIUS;
        while(!open.isEmpty()&&costs.size()<MAX_CELLS){
            Node node=open.poll();
            BlockPos here=node.pos();
            if(node.cost()!=costs.getOrDefault(here,Integer.MAX_VALUE))continue;
            int distance=heuristic(here,target);
            if(here.equals(target)){
                best=here;break;
            }
            // Only accept a frontier when advancing at least several cells.
            // Returning an adjacent cell each tick caused repeated replans.
            int travelled=Math.abs(here.getX()-start.getX())
                    +Math.abs(here.getZ()-start.getZ());
            if(travelled>=8&&distance<bestScore){
                bestScore=distance;best=here;
            }
            for(Direction direction:Direction.Plane.HORIZONTAL){
                BlockPos across=here.relative(direction);
                for(int delta:allowElevation?new int[]{0,-1,1}:new int[]{0}){
                    BlockPos next=across.offset(0,delta,0);
                    if(Math.abs(next.getX()-start.getX())>RADIUS
                            ||Math.abs(next.getZ()-start.getZ())>RADIUS
                            ||Math.abs(next.getY()-floor)>MAX_ELEVATION_FROM_START
                            ||next.equals(blocked)
                            ||!passable.test(next))continue;
                    // Multiple changes cost more than a level detour.
                    // Still allow +1 or -1 where terrain actually requires it.
                    double rise=Math.abs(standingY.applyAsDouble(next)
                            -standingY.applyAsDouble(here));
                    if(rise>1.001)continue;
                    // Half slabs are smooth floor; they should never
                    // trigger a jump or a high elevation-routing penalty.
                    int penalty=rise<=.501?0:STEP_HEIGHT_COST;
                    int cost=node.cost()+1+penalty;
                    if(cost>=costs.getOrDefault(next,Integer.MAX_VALUE))continue;
                    costs.put(next,cost);
                    parent.put(next,here);
                    open.add(new Node(next,cost,
                            cost+heuristic(next,target)));
                }
            }
        }
        // If the actual nearby target cannot be reached on this floor, do
        // not treat a stuck partial leg as a valid alternative repeatedly.
        // For nearby goals a partial approach is NOT success: it sends a
        // robot to the same impassable hillside on every replan. The full
        // 3-D search must reach the real destination, otherwise wait.
        if(best==null||best.equals(start)
                ||exact&&!best.equals(target)
                ||!exact&&bestScore>=heuristic(start,target))
            return null;
        var steps=new ArrayList<BlockPos>();
        for(BlockPos at=best;!at.equals(start);at=parent.get(at))steps.add(at);
        Collections.reverse(steps);
        return steps;
    }
}
