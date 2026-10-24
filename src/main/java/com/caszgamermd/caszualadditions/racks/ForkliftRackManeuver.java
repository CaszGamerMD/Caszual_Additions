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
    public record Plan(List<Pose> poses,int turn,double reverseDepth,double aisleShift) {
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
        // The service dock is 2.82 blocks in front of the pallet center.
        // The pallet bay ends ~3.82 blocks ahead of the chassis. Checking
        // 4.12 blocks treated a separate back-to-back rack BEHIND this bay
        // as if the tines had to pass through it; every valid dock became
        // impossible even when the pallet and aisle were clear. Check the
        // actual bay-contained 3.68-block fork/load sweep instead.
        for(double forward=1.65;forward<=3.68;forward+=.17){
            // Full 1.8-wide wrapped pallet, not just the fork metal.
            // Center + opposite side rails are sampled across its width.
            for(double across:new double[]{-.92,-.70,-.46,-.22,0,
                    .22,.46,.70,.92}){
                double x=center.x+fx*forward+sx*across;
                double z=center.z+fz*forward+sz*across;
                // Cross-section of both tines plus the complete three-block
                // high wrapped cargo envelope; loaded crates must not sweep
                // through a neighboring rack while the truck curves away.
                for(int dy=0;dy<=2;dy++){
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
            RackBlockEntity rack,Vec3 dock,int preferredTurn,
            double preferredDepth,double preferredShift){
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
            for(double back:turn==0?
                    // 2.7 blocks clears a rack only in some layouts: the
                    // actual tines reach 4.12 blocks forward and can still
                    // overlap the bay after a nominal withdrawal. Search
                    // farther on clear floors rather than insisting on a
                    // cramped 90-degree turn alongside neighboring racks.
                    new double[]{1.75,2.2,2.7,3.2,3.8,4.5,5.5,6.5,8.0}:
                    // A raised 1.8-wide pallet sometimes needs to turn in
                    // the aisle instead of backing toward the OPPOSITE rack.
                    // Also try almost pure lateral reverse arcs; every pose
                    // still undergoes the full swept collision test.
                    new double[]{0,.2,.45,.7,1.0,1.25}){
                // A curved arc with ZERO reverse depth is still an assigned
                // persistent path. Its saved lateral shift must constrain
                // this search after a chunk reload.
                if((preferredDepth>0||preferredShift>0)
                        &&Math.abs(back-preferredDepth)>.001)continue;
                for(double lateral:turn==0?new double[]{0}:
                        new double[]{1.5,1.9,2.2,2.6,3.2}){
                    if(preferredShift>0&&Math.abs(lateral-preferredShift)>.001)
                        continue;
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
                    Plan path=new Plan(List.copyOf(poses),turn,back,lateral);
                    if(!fullyOutside(world,path,floorY,palletY,rack))continue;
                    candidates.add(path);
                }
            }
        }
        if(candidates.isEmpty())return null;
        // A swept exit may be geometrically clear yet separated from the
        // forklift by another rack bank. The old planner picked that exit
        // anyway, and drive() could not reach it: the driver never moved.
        // Prefer a straight exit only among OUTSIDE positions that the
        // chassis can actually navigate to from its current position.
        candidates.sort(java.util.Comparator
                .comparingInt((Plan p)->Math.abs(p.turn()))
                .thenComparingDouble(p->forklift.distanceToSqr(
                        p.outside().position().x,p.outside().position().y,
                        p.outside().position().z)));
        // Persisted maneuvers were already traversing their saved arc when
        // the world unloaded. Do not reject them because the truck is now
        // partially inside the transfer lane rather than at its old approach.
        boolean resuming=preferredTurn!=0||preferredDepth>0||preferredShift>0;
        if(resuming)return candidates.getFirst();
        for(Plan candidate:candidates){
            Vec3 outside=candidate.outside().position();
            if(forklift.distanceToSqr(outside.x,outside.y,outside.z)<.04
                    ||!ForkliftAislePath.route(world,forklift,outside).isEmpty())
                return candidate;
        }
        return null;
    }
    public static float angleTo(float from,float to,float fraction){
        return from+Mth.wrapDegrees(to-from)*fraction;
    }
}
