package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Remote pickup is THREE distinct walking StorageGolem entities BEFORE lift.
 * The only shared MovingPallet entity is created when all three physically
 * arrive and accept the real, wrapped 216-slot target pallet.
 *
 * No cargo is cloned to workers; the fourth Golem separately owns the sole
 * empty replacement pallet. The saved mission UUID is also the future
 * MovingPallet UUID, letting courier/task recover across world reloads.
 */
public final class RemotePalletPickupCrew {
    private RemotePalletPickupCrew(){}

    /**
     * Choose the closest genuinely reachable pallet FACE for a compact
     * two-column pulling team. The first pair attaches in the row beside
     * the pallet; the second pair waits one row farther out. This works in
     * a 2-wide aisle, even when a 3x3 rally box will not fit behind the load.
     * A separate clear pallet-width withdrawal is checked again by the
     * loaded MovingPallet before it physically moves out of the corner.
     */
    public static BlockPos rally(ServerLevel level,BlockPos target,
            BlockPos from){
        if(!level.hasChunkAt(target))return BlockPos.ZERO;
        BlockState state=level.getBlockState(target);
        if(!(state.getBlock() instanceof PalletBlock))return BlockPos.ZERO;
        BlockPos[] bounds=bounds(target,state);
        int minX=bounds[0].getX(),maxX=bounds[1].getX();
        int minZ=bounds[0].getZ(),maxZ=bounds[1].getZ();
        BlockPos best=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        for(int gap=1;gap<=5;gap++){
            BlockPos[] candidates={
                    new BlockPos(minX,target.getY(),minZ-gap),
                    new BlockPos(maxX+gap,target.getY(),minZ),
                    new BlockPos(minX,target.getY(),maxZ+gap),
                    new BlockPos(minX-gap,target.getY(),minZ)
            };
            for(BlockPos anchor:candidates){
                if(!compactAisle(level,anchor,target))continue;
                // Requiring all four destinations to be physically
                // supported avoids the previously common "3 of 4 arrived"
                // deadlock. Once one square of this contiguous 2x2 is
                // reachable the remainder are connected without crossing
                // the pallet.
                var path=WarehousePreferredPath.leg(from,anchor,
                        p->StorageGolemReturnPath.walkable(level,p),
                        BlockPos.ZERO,
                        p->WarehouseTerrain.standingY(level,p));
                if(path==null)continue;
                double cost=path.size()+gap*.3;
                if(cost<score){score=cost;best=anchor;}
            }
        }
        return best;
    }
    private static BlockPos[] bounds(BlockPos target,BlockState state){
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(int part=0;part<4;part++){
            BlockPos tile=PalletBlock.partPos(target,part,state);
            minX=Math.min(minX,tile.getX());maxX=Math.max(maxX,tile.getX());
            minZ=Math.min(minZ,tile.getZ());maxZ=Math.max(maxZ,tile.getZ());
        }
        return new BlockPos[]{
                new BlockPos(minX,target.getY(),minZ),
                new BlockPos(maxX,target.getY(),maxZ)};
    }
    /** Resolve side against the real, possibly rotated 2x2 footprint,
     * not the controller corner. Otherwise a western or southern pallet
     * can be mistaken for a different face. */
    private static Direction face(ServerLevel level,BlockPos rally,BlockPos target){
        BlockState state=level.getBlockState(target);
        if(!(state.getBlock() instanceof PalletBlock))return Direction.NORTH;
        BlockPos[] corners=bounds(target,state);
        int minX=corners[0].getX(),maxX=corners[1].getX();
        int minZ=corners[0].getZ(),maxZ=corners[1].getZ();
        if(rally.getX()<minX)return Direction.WEST;
        if(rally.getX()>maxX)return Direction.EAST;
        if(rally.getZ()<minZ)return Direction.NORTH;
        return Direction.SOUTH;
    }
    /** Four independent, non-overlapping 2x2 positions; 3 pickup
     * workers use slots 0-2 and bay/no-replacement uses all four. */
    public static BlockPos individualGoal(ServerLevel level,BlockPos rally,
            BlockPos target,int workerIndex){
        Direction outward=face(level,rally,target);
        int width=workerIndex%2;
        int depth=workerIndex/2;
        Direction across=(outward==Direction.NORTH||outward==Direction.EAST)
                ?outward.getClockWise():outward.getCounterClockWise();
        return rally.relative(outward,depth)
                .relative(across,width);
    }
    public static boolean compactAisle(ServerLevel level,BlockPos rally,
            BlockPos target){
        if(rally.equals(BlockPos.ZERO))return false;
        for(int worker=0;worker<4;worker++)
            if(!StorageGolemReturnPath.walkable(level,
                    individualGoal(level,rally,target,worker)))return false;
        return true;
    }
    /** Compatibility helper retained for existing 3x3 diagnostics. */
    public static boolean threeWideAisle(ServerLevel level,BlockPos center){
        if(center.equals(BlockPos.ZERO))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            if(!StorageGolemReturnPath.walkable(level,center.offset(x,0,z)))
                return false;
        return true;
    }

    /** Recall an entire pre-lift mission, not one worker at a time.
     * A materialized carrier already owns its cargo/worker identities and
     * is deliberately not canceled by this empty-handed recovery path. */
    public static boolean abortBeforeLift(ServerLevel level,UUID mission){
        if(mission==null)return false;
        if(level.getEntity(mission) instanceof MovingPallet)return false;
        List<StorageGolem> members=new ArrayList<>();
        for(var entity:level.getAllEntities()){
            if(!(entity instanceof StorageGolem worker))continue;
            if((worker.remotePickupActive()
                    &&mission.equals(worker.remotePickupMission()))
                    ||(worker.remoteCourierActive()
                    &&mission.equals(worker.remoteCarrierId())))
                members.add(worker);
        }
        if(members.isEmpty())return false;
        for(StorageGolem member:members)
            member.finishAbortedRemotePickup(level);
        return true;
    }

    /** Called by worker zero after THREE exchange workers or FOUR bay workers
     * have reached their fixed external service cells. Nothing is merged
     * while any member is still travelling or cannot be found/reloaded. */
    public static void tryLift(ServerLevel level,StorageGolem leader){
        if(!leader.remotePickupActive()||leader.remotePickupIndex()!=0)return;
        UUID id=leader.remotePickupMission();
        if(id==null)return;
        if(level.getEntity(id) instanceof MovingPallet existing){
            // Crash/reload at the handoff boundary: loaded rig is already
            // sole owner of the three shelf identities. Retire remnants.
            if(existing.loaded())
                for(var entity:level.getAllEntities())
                    if(entity instanceof StorageGolem worker
                            &&worker.remotePickupActive()
                            &&id.equals(worker.remotePickupMission()))
                        worker.discard();
            return;
        }
        boolean bayPickup=DroneBayNoReplacementDispatch.NO_COURIER.equals(
                leader.remotePickupCourier());
        int workerCount=bayPickup?4:3;
        ArrayList<StorageGolem> team=new ArrayList<>(workerCount);
        for(var entity:level.getAllEntities()){
            if(!(entity instanceof StorageGolem worker)
                    ||!worker.remotePickupActive()
                    ||!id.equals(worker.remotePickupMission()))continue;
            team.add(worker);
        }
        if(team.size()!=workerCount){
            leader.reportRemotePickupStatus("PICKUP TEAM: "+team.size()+"/"+
                    workerCount+" LOADED; WAITING FOR MISSING WORKERS");
            return;
        }
        BlockPos aisle=leader.remotePickupRally();
        if(!compactAisle(level,aisle,leader.remotePickupTarget())){
            leader.reportRemotePickupStatus("PICKUP BLOCKED: RALLY AISLE CHANGED");
            return;
        }
        int readyCount=0;
        String waiting="";
        for(int index=0;index<workerCount;index++){
            final int slot=index;
            StorageGolem worker=team.stream()
                    .filter(w->w.remotePickupIndex()==slot).findFirst().orElse(null);
            if(worker==null){
                leader.reportRemotePickupStatus("PICKUP BLOCKED: MISSING WORKER "+index);
                return;
            }
            BlockPos goal=individualGoal(level,aisle,leader.remotePickupTarget(),index);
            if(worker.atRemotePickupGoal(level,goal))readyCount++;
            else waiting=worker.golemName()+" -> "+goal.toShortString();
        }
        if(readyCount!=workerCount){
            leader.reportRemotePickupStatus("PICKUP TEAM "+readyCount+"/"+workerCount+
                    " READY; WAITING "+waiting);
            return;
        }
        if(StorageGroundPalletTransfer.wrappedController(level,
                leader.remotePickupTarget())==null){
            leader.reportRemotePickupStatus("PICKUP BLOCKED: WRAPPED TARGET MISSING");
            return;
        }

        MovingPallet rig=RackContent.MOVING_PALLET_TYPE.create(level,
                EntitySpawnReason.EVENT);
        if(rig==null){
            leader.reportRemotePickupStatus("PICKUP BLOCKED: CANNOT CREATE CARRIER");
            return;
        }
        rig.setUUID(id);
        if(!rig.prepareRecoveredRemote(leader.remotePickupOwner(),
                leader.remotePickupHomes(),leader.remotePickupCourier(),
                leader.remotePickupTarget(),leader.remotePickupWarehouse())){
            leader.reportRemotePickupStatus("PICKUP BLOCKED: CARRIER SETUP REJECTED");
            return;
        }
        for(StorageGolem member:team)rig.setCrewName(
                member.remotePickupIndex(),member.golemName());
        if(!bayPickup&&level.getEntity(leader.remotePickupCourier()) instanceof StorageGolem courier)
            rig.setCrewName(3,courier.golemName());
        // The loaded rig must start where the real pallet actually sits,
        // then PULL the load through a checked aisle. Spawning it at the
        // 4-6-block staging waypoint effectively teleported the pallet
        // into the aisle, bypassing any physical extraction.
        BlockState oldState=level.getBlockState(leader.remotePickupTarget());
        BlockPos[] footprint=bounds(leader.remotePickupTarget(),oldState);
        rig.setPos((footprint[0].getX()+footprint[1].getX()+1)*.5,
                leader.remotePickupTarget().getY(),
                (footprint[0].getZ()+footprint[1].getZ()+1)*.5);
        if(!level.addFreshEntity(rig)){
            leader.reportRemotePickupStatus("PICKUP BLOCKED: CARRIER SPAWN REJECTED");
            return;
        }
        // Existing transaction copies 216 slots into the ONE rig then
        // removes the ground pallet. On failure it never destroys source;
        // return to the same independent workers and retry later.
        if(!StorageGroundPalletTransfer.liftForRemoteExchange(level,
                leader.remotePickupTarget(),rig)){
            rig.discard();
            leader.reportRemotePickupStatus("PICKUP BLOCKED: PALLET HANDOFF REJECTED");
            return;
        }
        // The rig now owns the one original pallet and 216 real slots.
        // It physically pulls into the 2-wide aisle BEFORE any exchange
        // placement or no-replacement warehouse return begins.
        rig.beginRemotePullout(face(level,aisle,leader.remotePickupTarget()));
        for(StorageGolem worker:team)worker.discard();
    }

    /** Backwards-compatible upgrade for preexisting 3-model rigs that
     * are still EMPTY and have not lifted the target yet. Spawn the three
     * actual Golems at the former rig location; never double-withdraw the
     * courier's replacement pallet or consume another shelf worker. */
    public static boolean splitLegacyRig(ServerLevel level,MovingPallet rig,
            UUID player,BlockPos[] homes,UUID courier,
            BlockPos target,BlockPos warehouse){
        BlockPos rally=rally(level,target,rig.blockPosition());
        if(rally.equals(BlockPos.ZERO))return false;
        ArrayList<StorageGolem> spawned=new ArrayList<>();
        for(int i=0;i<3;i++){
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,
                    EntitySpawnReason.EVENT);
            if(golem==null)break;
            golem.assignPlayerCrew(homes[i],i,player);
            golem.beginRemotePickup(rig.getUUID(),courier,target,warehouse,
                    rally,homes);
            BlockPos start=rig.blockPosition().offset(i-1,0,0);
            if(!StorageGolemReturnPath.walkable(level,start))
                start=rig.blockPosition();
            golem.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
            if(!level.addFreshEntity(golem))break;
            spawned.add(golem);
        }
        if(spawned.size()==3){
            rig.discard();
            return true;
        }
        for(StorageGolem worker:spawned)worker.discard();
        return false;
    }
}
