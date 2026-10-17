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
        // If the job ends on a different level, a flat partial route would
        // strand the golem at the base of the very last one-block step.
        var flat=start.getY()==target.getY()
                ?find(start,target,passable,blocked,false):null;
        if(flat!=null)return flat;
        return find(start,target,passable,blocked,true);
    }
    private static java.util.List<BlockPos> find(BlockPos start,BlockPos target,
            Predicate<BlockPos> passable,BlockPos blocked,boolean allowElevation){
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
                    int cost=node.cost()+1+(delta==0?0:STEP_HEIGHT_COST);
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
