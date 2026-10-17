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

    /** Find a genuine 3x3, SAME-Y loading aisle outside the old 2x2
     * footprint. One individually walking worker stands in each column. */
    public static BlockPos rally(ServerLevel level,BlockPos target,
            BlockPos from){
        if(!level.hasChunkAt(target))return BlockPos.ZERO;
        BlockState state=level.getBlockState(target);
        if(!(state.getBlock() instanceof PalletBlock))return BlockPos.ZERO;
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
        int minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(int part=0;part<4;part++){
            BlockPos tile=PalletBlock.partPos(target,part,state);
            minX=Math.min(minX,tile.getX());maxX=Math.max(maxX,tile.getX());
            minZ=Math.min(minZ,tile.getZ());maxZ=Math.max(maxZ,tile.getZ());
        }
        BlockPos best=BlockPos.ZERO;
        double score=Double.POSITIVE_INFINITY;
        for(int gap=2;gap<=6;gap++){
            BlockPos[] candidates={
                    new BlockPos(minX,target.getY(),minZ-gap),
                    new BlockPos(maxX+gap,target.getY(),minZ),
                    new BlockPos(minX,target.getY(),maxZ+gap),
                    new BlockPos(minX-gap,target.getY(),minZ)
            };
            for(BlockPos center:candidates){
                if(!threeWideAisle(level,center))continue;
                // All three independent destinations must stay inside
                // the checked aisle on the correct side of the pallet.
                boolean good=true;
                for(int index=0;index<3;index++)
                    if(!StorageGolemReturnPath.walkable(level,
                            individualGoal(center,target,index))){
                        good=false;break;
                    }
                if(!good)continue;
                // Straight distance may select a staging area on the
                // wrong side of a solid rack. Prefer a REAL walking route
                // whenever the warehouse/source is in loaded range.
                double distance=center.distSqr(from);
                if(from.getY()==center.getY()
                        &&Math.abs(from.getX()-center.getX())<=36
                        &&Math.abs(from.getZ()-center.getZ())<=36){
                    var route=StorageGolemReturnPath.steps(from,center,
                            pos->StorageGolemReturnPath.walkable(level,pos));
                    if(route==null)continue;
                    distance=route.size()*route.size();
                }
                if(distance<score){score=distance;best=center;}
            }
        }
        return best;
    }
    public static boolean threeWideAisle(ServerLevel level,BlockPos center){
        if(center.equals(BlockPos.ZERO))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
            if(!StorageGolemReturnPath.walkable(level,center.offset(x,0,z)))
                return false;
        return true;
    }

    public static BlockPos individualGoal(BlockPos rally,BlockPos target,
            int workerIndex){
        // Each worker gets a distinct, FIXED floor tile across the outer
        // pallet aisle, perpendicular to the target-facing direction.
        // The old unconditional X offset placed east/west teams in a
        // single-file line, sometimes pushing the nearest worker into
        // the wrapped 2x2 pallet or another worker's route.
        int lane=workerIndex-1;
        if(Math.abs(rally.getX()-target.getX())
                >Math.abs(rally.getZ()-target.getZ()))
            return rally.offset(0,0,lane);
        return rally.offset(lane,0,0);
    }

    /** Called by worker zero after all THREE independently walking workers
     * have reached their fixed external service cells. Nothing is merged
     * while any member is still travelling or cannot be found/reloaded. */
    public static void tryLift(ServerLevel level,StorageGolem leader){
        if(!leader.remotePickupActive()||leader.remotePickupIndex()!=0)return;
        UUID id=leader.remotePickupMission();
        if(id==null)return;
        if(level.getEntity(id) instanceof MovingPallet existing){
            // Crash/reload at the handoff boundary: loaded rig is already
            // sole owner of the three shelf identities. Retire remnants.
            if(existing.remoteExchangeActive()&&existing.loaded())
                for(var entity:level.getAllEntities())
                    if(entity instanceof StorageGolem worker
                            &&worker.remotePickupActive()
                            &&id.equals(worker.remotePickupMission()))
                        worker.discard();
            return;
        }
        ArrayList<StorageGolem> trio=new ArrayList<>(3);
        for(var entity:level.getAllEntities()){
            if(!(entity instanceof StorageGolem worker)
                    ||!worker.remotePickupActive()
                    ||!id.equals(worker.remotePickupMission()))continue;
            trio.add(worker);
        }
        if(trio.size()!=3)return;
        BlockPos aisle=leader.remotePickupRally();
        if(!threeWideAisle(level,aisle))return;
        for(int index=0;index<3;index++){
            final int slot=index;
            StorageGolem worker=trio.stream()
                    .filter(w->w.remotePickupIndex()==slot).findFirst().orElse(null);
            if(worker==null)return;
            BlockPos goal=individualGoal(aisle,leader.remotePickupTarget(),index);
            if(!worker.atRemotePickupGoal(level,goal))return;
        }
        if(StorageGroundPalletTransfer.wrappedController(level,
                leader.remotePickupTarget())==null)return;

        MovingPallet rig=RackContent.MOVING_PALLET_TYPE.create(level,
                EntitySpawnReason.EVENT);
        if(rig==null)return;
        rig.setUUID(id);
        if(!rig.prepareRecoveredRemote(leader.remotePickupOwner(),
                leader.remotePickupHomes(),leader.remotePickupCourier(),
                leader.remotePickupTarget(),leader.remotePickupWarehouse()))
            return;
        rig.setPos(aisle.getX()+.5,aisle.getY(),aisle.getZ()+.5);
        if(!level.addFreshEntity(rig))return;
        // Existing transaction copies 216 slots into the ONE rig then
        // removes the ground pallet. On failure it never destroys source;
        // return to three real workers and retry later.
        if(!StorageGroundPalletTransfer.liftForRemoteExchange(level,
                leader.remotePickupTarget(),rig)){
            rig.discard();
            return;
        }
        for(StorageGolem worker:trio)worker.discard();
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
