package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Collision-aware, horizontal warehouse routing. Vanilla ground navigation
 * tries to climb racks or fall back to a straight line when a 1.2-wide
 * forklift cannot stand in a path node. This small 0.5-block lattice planner
 * checks the complete chassis at EVERY intermediate step, including the
 * corners of a three-block aisle. It never teleports or ignores collisions.
 *
 * A blocked destination is NOT a reason to steer through the intervening
 * racks. Return no route until the aisle can actually be traversed.
 */
public final class ForkliftAislePath {
    private static final int MAX_VISITED=18000,MAX_RANGE=64;
    private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};
    private record Cell(int x,int z){}
    private record Candidate(Cell cell,int f){}
    private ForkliftAislePath(){}

    private static boolean standable(ServerLevel world,WarehouseForklift vehicle,
            double x,double y,double z){
        AABB box=vehicle.getBoundingBox().move(x-vehicle.getX(),
                y-vehicle.getY(),z-vehicle.getZ());
        if(!world.noCollision(vehicle,box))return false;
        // Check the physical support beneath all corners, not just the
        // vehicle midpoint: an exposed wheel must not hang over a rack gap.
        double inset=.12;
        for(double px:new double[]{box.minX+inset,box.maxX-inset})
            for(double pz:new double[]{box.minZ+inset,box.maxZ-inset}){
                BlockPos below=BlockPos.containing(px,y-.08,pz);
                if(!world.hasChunkAt(below))return false;
                if(world.getBlockState(below).is(RackContent.FORKLIFT_PARKING))
                    below=below.below();
                if(!world.hasChunkAt(below)
                        ||world.getBlockState(below)
                                .getCollisionShape(world,below).isEmpty())
                    return false;
            }
        return true;
    }
    /** Use this for final dock clearance too, with the real fractional
     * 2x2 pallet midpoint rather than one of its corners. */
    public static boolean canStand(ServerLevel world,WarehouseForklift vehicle,
            Vec3 location){
        return Math.abs(vehicle.getY()-location.y)<.7
                &&standable(world,vehicle,location.x,vehicle.getY(),location.z);
    }

    /** Verify the entire vehicle can follow an interpolated straight segment,
     * rather than checking only its far endpoint. This is used to smooth
     * the short A* grid nodes into real driving stretches WITHOUT cutting
     * across rack corners or driving through pallet loads.
     * Each sample checks both the collision box and floor under all wheels.
     */
    public static boolean clearSegment(ServerLevel world,WarehouseForklift vehicle,
            Vec3 from,Vec3 to){
        if(Math.abs(from.y-to.y)>.25)return false;
        double distance=Math.hypot(to.x-from.x,to.z-from.z);
        if(distance<.005)return true;
        int samples=Math.max(1,(int)Math.ceil(distance/.18));
        if(samples>96)return false;
        for(int i=1;i<=samples;i++){
            double t=i/(double)samples;
            if(!standable(world,vehicle,
                    from.x+(to.x-from.x)*t,vehicle.getY(),
                    from.z+(to.z-from.z)*t))return false;
        }
        return true;
    }

    /** Convert the half-block A* cells into a FIXED series of longer
     * verified driving segments, ONCE per route. The previous controller
     * recalculated its farthest visible aim every tick, making the steering
     * target wobble and causing repeated braking even in clear corridors.
     *
     * Only omit intermediate points when the full 1.2-wide forklift can
     * traverse the connecting line without clipping a rack corner.
     */
    public static List<Vec3> smooth(ServerLevel world,WarehouseForklift vehicle,
            List<Vec3> raw,double maxSegment){
        if(raw.isEmpty())return List.of();
        List<Vec3> segments=new ArrayList<>();
        Vec3 start=vehicle.position();
        int index=0;
        while(index<raw.size()){
            int furthest=-1;
            for(int next=index;next<raw.size();next++){
                Vec3 candidate=raw.get(next);
                if(start.distanceToSqr(candidate)>maxSegment*maxSegment
                        &&next>index)break;
                if(!clearSegment(world,vehicle,start,candidate))break;
                furthest=next;
            }
            if(furthest<index)return List.of();
            Vec3 end=raw.get(furthest);
            segments.add(end);
            start=end;
            index=furthest+1;
        }
        return segments;
    }

    private static int distance(Cell a,Cell b){
        return Math.abs(a.x-b.x)+Math.abs(a.z-b.z);
    }

    public static List<Vec3> route(ServerLevel world,WarehouseForklift vehicle,
            Vec3 destination){
        if(!canStand(world,vehicle,destination))return List.of();
        Cell start=new Cell((int)Math.round(vehicle.getX()*2),
                (int)Math.round(vehicle.getZ()*2));
        Cell end=new Cell((int)Math.round(destination.x*2),
                (int)Math.round(destination.z*2));
        if(distance(start,end)>MAX_RANGE*4)return List.of();
        Map<Cell,Integer> cost=new HashMap<>();
        Map<Cell,Cell> parent=new HashMap<>();
        Set<Cell> visited=new HashSet<>();
        PriorityQueue<Candidate> frontier=new PriorityQueue<>(
                Comparator.comparingInt(Candidate::f));
        cost.put(start,0);
        frontier.add(new Candidate(start,10*distance(start,end)));
        while(!frontier.isEmpty()&&visited.size()<MAX_VISITED){
            Cell current=frontier.poll().cell();
            if(!visited.add(current))continue;
            if(current.equals(end)){
                List<Vec3> path=new ArrayList<>();
                Cell at=end;
                while(!at.equals(start)){
                    path.add(new Vec3(at.x*.5,vehicle.getY(),at.z*.5));
                    at=parent.get(at);
                    if(at==null)return List.of();
                }
                Collections.reverse(path);
                // The last 0.25-block adjustment matters: a 2-wide rack
                // pallet has a true central axis between the rack columns.
                if(path.isEmpty()
                        ||path.getLast().distanceToSqr(destination)>.0001)
                    path.add(destination);
                return path;
            }
            Cell previous=parent.get(current);
            for(int[] dir:DIRECTIONS){
                Cell next=new Cell(current.x+dir[0],current.z+dir[1]);
                if(Math.abs(next.x-start.x)>MAX_RANGE*2
                        ||Math.abs(next.z-start.z)>MAX_RANGE*2
                        ||visited.contains(next))continue;
                // Turning costs a little more than continuing straight,
                // reducing zig-zags in long parallel warehouse aisles.
                int turnCost=previous!=null
                        &&(current.x-previous.x!=dir[0]
                        ||current.z-previous.z!=dir[1])?3:0;
                int nextCost=cost.get(current)+10+turnCost;
                if(nextCost>=cost.getOrDefault(next,Integer.MAX_VALUE))continue;
                if(!standable(world,vehicle,next.x*.5,vehicle.getY(),next.z*.5))
                    continue;
                cost.put(next,nextCost);
                parent.put(next,current);
                frontier.add(new Candidate(next,nextCost+10*distance(next,end)));
            }
        }
        return List.of();
    }
}
