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
        return feetState.getCollisionShape(level,feet).isEmpty()
                &&headState.getCollisionShape(level,feet.above()).isEmpty()
                &&!below.getCollisionShape(level,ground).isEmpty();
    }

    /** Returns turn/end waypoints with the start omitted. No diagonal
     * corner cutting, ascents or leaps are permitted. A null result means
     * there is no complete same-level route through currently loaded blocks. */
    public static List<BlockPos> route(BlockPos from,BlockPos shelf,
            Predicate<BlockPos> passable){
        if(from.getY()!=shelf.getY())return null;
        Set<BlockPos> goals=new HashSet<>();
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos goal=shelf.relative(side);
            if(passable.test(goal))goals.add(goal);
        }
        if(goals.isEmpty())return null;
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
