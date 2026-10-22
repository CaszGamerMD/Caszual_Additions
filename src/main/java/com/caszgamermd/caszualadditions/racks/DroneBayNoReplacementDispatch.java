package com.caszgamermd.caszualadditions.racks;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

/** No-exchange warehouse pickup: four real independently walking Golems
 * retrieve a wrapped floor pallet and deliver it to an INPUT aisle rack.
 * No courier, spare pallet or player-online requirement. */
public final class DroneBayNoReplacementDispatch {
    /** Sentinel distinguishes four empty-handed pickup workers from the
     * normal REMOTE exchange (three walkers plus one replacement courier). */
    public static final UUID NO_COURIER=new UUID(0,1);

    public static String request(ServerLevel level,UUID owner,BlockPos computer,BlockPos target){
        if(owner==null||!level.hasChunkAt(computer)||!level.hasChunkAt(target))
            return "WAITING: warehouse or INPUT bay chunk unavailable.";
        if(!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "INVALID: linked warehouse computer is missing.";
        var pallet=StorageGroundPalletTransfer.wrappedController(level,target);
        if(pallet==null||!pallet.getBlockPos().equals(target))
            return "WAITING: wrapped pallet is no longer on the INPUT bay.";
        for(var entity:level.getAllEntities()){
            if(entity instanceof MovingPallet rig&&target.equals(rig.remoteExchangeTarget()))
                return "WAITING: this pallet has an active crew.";
            if(entity instanceof StorageGolem worker&&worker.remotePickupActive()
                    &&target.equals(worker.remotePickupTarget()))
                return "WAITING: this pallet has an active crew.";
        }
        var warehouse=WarehouseNetwork.fromComputer(level,computer);
        // A mission requires a REAL empty INPUT rack or accessible fallback;
        // never remove the bay pallet before space is available for return.
        boolean space=WarehouseAislePriority.firstAvailableInputRack(
                level,warehouse.racks())!=null;
        if(!space)for(var rack:warehouse.racks())
            if(!rack.hasPallet()&&rack.isEmpty()&&!WarehouseAislePriority.isOutputAisle(level,rack)
                    &&WarehouseSortingReach.groundRack(level,rack)
                    &&RackPickupPlan.find(level,rack)!=null){space=true;break;}
        if(!space)return "WAITING: warehouse needs a vacant INPUT or regular rack.";
        List<StorageGolemShelfEntity> shelves=new ArrayList<>(4);
        for(var shelf:warehouse.shelves()){
            for(int i=0;i<shelf.docked()&&shelves.size()<4;i++)shelves.add(shelf);
            if(shelves.size()==4)break;
        }
        if(shelves.size()<4)return "WAITING: four docked storage Golems needed.";
        BlockPos staging=null;
        for(var shelf:shelves){
            BlockPos door=StorageGolemShelfDoor.exit(level,shelf);
            if(door==null)continue;
            BlockPos candidate=door.relative(StorageGolemShelfDoor.front(shelf),1);
            boolean clear=true;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(!StorageGolemReturnPath.walkable(level,candidate.offset(dx,0,dz)))
                    clear=false;
            if(clear){staging=candidate;break;}
        }
        if(staging==null)return "WAITING: clear four-Golem departure aisle required.";
        BlockPos rally=RemotePalletPickupCrew.rally(level,target,staging);
        if(rally.equals(BlockPos.ZERO))
            return "WAITING: INPUT bay needs a clear three-wide pickup approach.";
        List<BlockPos> starts=new ArrayList<>(4);
        for(int dx=-1;dx<=1&&starts.size()<4;dx++)for(int dz=-1;dz<=1&&starts.size()<4;dz++){
            BlockPos where=staging.offset(dx,0,dz);
            if(!StorageGolemReturnPath.walkable(level,where))continue;
            if(starts.stream().anyMatch(previous->previous.distSqr(where)<2))continue;
            starts.add(where);
        }
        if(starts.size()!=4)return "WAITING: four separated crew departure tiles required.";
        var names=new ArrayList<String>(3);
        for(var shelf:shelves){
            String name=shelf.takeNamed();
            if(name==null){
                for(int i=0;i<names.size();i++)shelves.get(i).addNamed(names.get(i));
                return "WAITING: reserved Golems changed before dispatch.";
            }
            names.add(name);
        }
        BlockPos[] homes={shelves.get(0).getBlockPos(),shelves.get(1).getBlockPos(),
                shelves.get(2).getBlockPos(),shelves.get(3).getBlockPos()};
        UUID mission=UUID.randomUUID();
        var spawned=new ArrayList<StorageGolem>(3);
        boolean ok=true;
        for(int i=0;i<4;i++){
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(
                level,EntitySpawnReason.EVENT);
            if(worker==null){ok=false;break;}
            worker.assignPlayerCrew(homes[i],i,owner);
            worker.setGolemName(names.get(i));
            worker.beginRemotePickup(mission,NO_COURIER,target,computer,rally,homes);
            BlockPos start=starts.get(i);
            worker.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
            if(!level.addFreshEntity(worker)){ok=false;break;}
            spawned.add(worker);
        }
        if(!ok){
            for(var worker:spawned)worker.discard();
            for(int i=0;i<names.size();i++)shelves.get(i).addNamed(names.get(i));
            return "WAITING: dispatch failed; all four shelf workers restored.";
        }
        return "Four Golems dispatched to INPUT bay "+target.toShortString()+
                " with NO replacement pallet; returning cargo to INPUT rack.";
    }
    private DroneBayNoReplacementDispatch(){}
}
