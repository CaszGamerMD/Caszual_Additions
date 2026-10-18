package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Shared-platform route planner; no independent corner Golem navigation.
 * Always prefer a flat aisle, permit a one-block DROP, and forbid rises
 * when the person being followed is not higher than the carrying crew.
 *
 * Cardinal steps only, no jumping/diagonal corner clipping. A world-level
 * passability predicate must inspect the whole 1.85-wide pallet footprint.
 */
public final class MovingPalletFloorPath {
    private static final int MAX_CELLS=2200;
    private static final int MARGIN=12;
    private MovingPalletFloorPath(){}

    public static List<BlockPos> route(BlockPos start,BlockPos playerFloor,
            boolean playerAbove,Predicate<BlockPos> standable){
        return route(start,playerFloor,playerAbove,standable,(a,b)->false);
    }
    /** Permit an upward integer Y waypoint even without a full jump when
     * its actual surface is only a HALF slab above the previous cell. */
    public static List<BlockPos> route(BlockPos start,BlockPos playerFloor,
            boolean playerAbove,Predicate<BlockPos> standable,
            java.util.function.BiPredicate<BlockPos,BlockPos> smoothHalfStep){
        if(nearPlayer(start,playerFloor))return List.of();
        int minX=Math.min(start.getX(),playerFloor.getX())-MARGIN;
        int maxX=Math.max(start.getX(),playerFloor.getX())+MARGIN;
        int minZ=Math.min(start.getZ(),playerFloor.getZ())-MARGIN;
        int maxZ=Math.max(start.getZ(),playerFloor.getZ())+MARGIN;
        int maxY=Math.max(start.getY(),playerFloor.getY());
        int minY=Math.min(start.getY(),playerFloor.getY())-1;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Map<BlockPos,BlockPos> parent=new HashMap<>();
        queue.add(start);
        parent.put(start,start);
        BlockPos destination=null;
        while(!queue.isEmpty()&&parent.size()<MAX_CELLS){
            BlockPos current=queue.removeFirst();
            if(nearPlayer(current,playerFloor)){
                destination=current;break;
            }
            for(Direction direction:Direction.Plane.HORIZONTAL){
                BlockPos same=current.relative(direction);
                // Flat ground first, then down one block; up ONLY if
                // the player is elevated. Up is never explored merely
                // because the pathfinder found a shortcut over a crate.
                for(int change:new int[]{0,-1,1}){
                    BlockPos next=same.offset(0,change,0);
                    if(change==1&&!playerAbove
                            &&!smoothHalfStep.test(current,next))continue;
                    if(next.getY()<minY||next.getY()>maxY
                            ||next.getX()<minX||next.getX()>maxX
                            ||next.getZ()<minZ||next.getZ()>maxZ
                            ||parent.containsKey(next)||!standable.test(next))
                        continue;
                    parent.put(next,current);
                    queue.addLast(next);
                }
            }
        }
        if(destination==null)return null;
        ArrayList<BlockPos> result=new ArrayList<>();
        for(BlockPos p=destination;!p.equals(start);p=parent.get(p))
            result.add(p);
        Collections.reverse(result);
        return result;
    }
    /**
     * Rack delivery has an EXACT front-aisle destination; proximity to
     * the nearest rack block is not enough and may put the crew behind
     * its solid frame. This route uses the same checked level-only cells
     * as loaded follow, but does not stop short of the front staging cell.
     */
    public static List<BlockPos> toRackFront(BlockPos start,BlockPos target,
            Predicate<BlockPos> standable){
        if(start.getY()!=target.getY())return null;
        if(start.equals(target))return List.of();
        int minX=Math.min(start.getX(),target.getX())-MARGIN;
        int maxX=Math.max(start.getX(),target.getX())+MARGIN;
        int minZ=Math.min(start.getZ(),target.getZ())-MARGIN;
        int maxZ=Math.max(start.getZ(),target.getZ())+MARGIN;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Map<BlockPos,BlockPos> parent=new HashMap<>();
        queue.add(start);
        parent.put(start,start);
        while(!queue.isEmpty()&&parent.size()<MAX_CELLS){
            BlockPos current=queue.removeFirst();
            if(current.equals(target)){
                ArrayList<BlockPos> result=new ArrayList<>();
                for(BlockPos p=current;!p.equals(start);p=parent.get(p))
                    result.add(p);
                Collections.reverse(result);
                return result;
            }
            for(Direction side:Direction.Plane.HORIZONTAL){
                BlockPos next=current.relative(side);
                if(next.getX()<minX||next.getX()>maxX
                        ||next.getZ()<minZ||next.getZ()>maxZ
                        ||parent.containsKey(next)||!standable.test(next))
                    continue;
                parent.put(next,current);
                queue.addLast(next);
            }
        }
        return null;
    }

    private static boolean nearPlayer(BlockPos spot,BlockPos player){
        double x=spot.getX()-player.getX();
        double z=spot.getZ()-player.getZ();
        return x*x+z*z<=3.25
                &&spot.getY()==player.getY();
    }
}
