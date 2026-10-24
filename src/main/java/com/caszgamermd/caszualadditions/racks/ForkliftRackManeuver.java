package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side rack-entry/withdrawal planner. Both tines and carried cargo
 * maintain rack elevation throughout the entire motion. A truck backs along
 * a gradual 90 degree arc when it cannot withdraw straight across the aisle.
 *
 * NEVER allow a mast height change until the entire swept fork envelope is
 * clear of every rack (including the source/destination). Refuse an unsafe
 * route rather than ghost the forks through warehouse shelves.
 */
public final class ForkliftRackManeuver {
    public record Pose(Vec3 position,float yaw){}
    /** Poses are ordered dock -> safe exterior. */
    public record Plan(List<Pose> poses,int turn) {
        public Pose outside(){return poses.getLast();}
        public Pose dock(){return poses.getFirst();}
    }
    private static final int STEPS=24;
    private ForkliftRackManeuver(){}

    private static float dockYaw(Direction intoRack){
        return (float)Math.toDegrees(Math.atan2(-intoRack.getStepX(),intoRack.getStepZ()));
    }
    private static boolean insideSelectedRack(ServerLevel level,BlockPos p,
            RackBlockEntity rack){
        return RackBlock.controller(level,p)==rack;
    }
    /** Enforce the full extended fork sweep, not just the 1.2-wide wheels.
     * The rack being serviced is intentionally contacted by the fork tines
     * during insertion; every OTHER rack/pallet/cargo proxy is forbidden.
     */
    public static boolean clearForkEnvelope(ServerLevel world,Vec3 center,
            float yaw,int floorY,int palletY,RackBlockEntity serviced,
            boolean allowSelectedRack){
        double angle=Math.toRadians(yaw);
        double fx=-Math.sin(angle),fz=Math.cos(angle);
        double sx=fz,sz=-fx;
        int height=Math.max(floorY,palletY);
        for(double forward=1.65;forward<=4.12;forward+=.19){
            for(double across:new double[]{-.66,-.44,0,.44,.66}){
                double x=center.x+fx*forward+sx*across;
                double z=center.z+fz*forward+sz*across;
                // Cross-section of both tines AND a wrapped cargo base,
                // including a second sample one block above the pallet.
                for(int dy=0;dy<=1;dy++){
                    BlockPos p=BlockPos.containing(x,height+dy+.15,z);
                    if(!world.hasChunkAt(p))return false;
                    var state=world.getBlockState(p);
                    boolean rack=state.getBlock() instanceof RackBlock;
                    if(rack){
                        if(!(allowSelectedRack&&insideSelectedRack(world,p,serviced)))
                            return false;
                    }else if(state.getBlock() instanceof PalletBlock
                            ||state.getBlock() instanceof PalletCargoCollisionBlock)
                        return false;
                }
            }
        }
        return true;
    }
    public static boolean fullyOutside(ServerLevel world,Plan plan,int floorY,
            int palletY,RackBlockEntity rack){
        Pose last=plan.outside();
        return clearForkEnvelope(world,last.position(),last.yaw(),
                floorY,palletY,rack,false);
    }
    /** Find a verified straight withdrawal or a left/right swept 90 degree
     * reverse manoeuvre. Sideways travel uses easing to avoid instant pivots.
     * The static envelope is sampled every 1/24 manoeuvre, so nearby racks
     * remain solid even when the forklift itself has a clear chassis path.
     */
    public static Plan plan(ServerLevel world,WarehouseForklift forklift,
            RackBlockEntity rack,Vec3 dock,int preferredTurn){
        Direction outward=rack.facing();
        Direction inward=outward.getOpposite();
        int floorY=(int)Math.floor(dock.y);
        int palletY=rack.getBlockPos().getY();
        List<Plan> candidates=new ArrayList<>();
        // Straight out where enough room exists. Otherwise search rounded
        // exits in both directions and several depths for a tight aisle.
        int[] turns=preferredTurn!=0?new int[]{preferredTurn}:
                new int[]{0,1,-1};
        for(int turn:turns){
            for(double back:turn==0?new double[]{1.75,2.2,2.7}:
                    new double[]{.45,.7,1.0,1.25}){
                for(double lateral:turn==0?new double[]{0}:
                        new double[]{1.9,2.2,2.6}){
                    List<Pose> poses=new ArrayList<>();
                    boolean valid=true;
                    float start=dockYaw(inward);
                    for(int i=0;i<=STEPS;i++){
                        double t=i/(double)STEPS;
                        double eased=t*t*(3-2*t);
                        double travel=back*Math.sin(t*Math.PI*.5);
                        double sideways=turn*lateral*(1-Math.cos(t*Math.PI*.5));
                        Vec3 pos=new Vec3(
                                dock.x+outward.getStepX()*travel+
                                    outward.getClockWise().getStepX()*sideways,
                                dock.y,
                                dock.z+outward.getStepZ()*travel+
                                    outward.getClockWise().getStepZ()*sideways);
                        float yaw=start+turn*90f*(float)eased;
                        Pose pose=new Pose(pos,yaw);
                        if(!ForkliftAislePath.canStand(world,forklift,pos)
                                ||!clearForkEnvelope(world,pos,yaw,floorY,palletY,
                                        rack,true)){
                            valid=false;break;
                        }
                        poses.add(pose);
                    }
                    if(!valid)continue;
                    Plan path=new Plan(List.copyOf(poses),turn);
                    if(!fullyOutside(world,path,floorY,palletY,rack))continue;
                    candidates.add(path);
                }
            }
        }
        if(candidates.isEmpty())return null;
        // Prefer the exterior closest to the driver's present location;
        // straight withdrawal is preferred if equally convenient.
        candidates.sort(java.util.Comparator.comparingDouble((Plan p)->
                forklift.distanceToSqr(p.outside().position().x,
                        p.outside().position().y,p.outside().position().z))
                .thenComparingInt(p->Math.abs(p.turn())));
        return candidates.getFirst();
    }
    public static float angleTo(float from,float to,float fraction){
        return from+Mth.wrapDegrees(to-from)*fraction;
    }
}
