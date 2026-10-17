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
 * Safe, finite physical warehouse routes. Flat cardinal aisles ALWAYS win:
 * try an all-same-Y path first, then stairs/slopes only when no flat leg
 * exists (or when the actual destination is on another floor).
 *
 * Long-distance carriers compute 36-block legs in currently loaded chunks,
 * keeping the SAME persisted job and recomputing on obstruction/reload.
 */
public final class WarehousePreferredPath {
    private WarehousePreferredPath(){}
    public static final int RADIUS=36,MAX_CELLS=6500;
    private record Node(BlockPos pos,int cost,int estimate){}
    private static int heuristic(BlockPos p,BlockPos target){
        return Math.abs(p.getX()-target.getX())
                +Math.abs(p.getZ()-target.getZ())
                +Math.abs(p.getY()-target.getY())*20;
    }
    public static java.util.List<BlockPos> leg(BlockPos start,BlockPos target,
            Predicate<BlockPos> passable,BlockPos blocked){
        var flat=find(start,target,passable,blocked,false);
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
                            ||Math.abs(next.getY()-floor)>3
                            ||next.equals(blocked)
                            ||!passable.test(next))continue;
                    int cost=node.cost()+1+(delta==0?0:18);
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
        if(best==null||best.equals(start)||exact&&bestScore>=heuristic(start,target))
            return null;
        var steps=new ArrayList<BlockPos>();
        for(BlockPos at=best;!at.equals(start);at=parent.get(at))steps.add(at);
        Collections.reverse(steps);
        return steps;
    }
}
