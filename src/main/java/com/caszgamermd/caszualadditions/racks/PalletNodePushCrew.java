package com.caszgamermd.caszualadditions.racks;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

/** Separate two-Golem ground transport from the FOUR-Golem INPUT pickup and
 * the unchanged FOUR-Golem (three plus courier) REMOTE exchange. */
public final class PalletNodePushCrew {
    public static String request(ServerLevel level,UUID owner,BlockPos computer,
                                 BlockPos node,UUID vehicle,boolean loading){
        if(owner==null||vehicle==null||!level.hasChunkAt(computer)||!level.hasChunkAt(node))
            return "WAITING: Node or warehouse unavailable.";
        if(!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "WAITING: Linked Rack Computer missing.";
        // A transport MUST be adjacent. Pallets Plus validates actual
        // vehicle class, speed and cargo capacity again at commit time.
        if(level.getEntity(vehicle)==null)return "WAITING: Adjacent vehicle unloaded.";
        var network=WarehouseNetwork.fromComputer(level,computer);
        // OUTPUT aisle is the warehouse's staging inventory. When the
        // Transfer Node is empty, pick a genuine wrapped pallet in an
        // accessible ground-level OUTPUT rack, then physically walk both
        // Golems to that rack BEFORE traveling together to the node.
        RackBlockEntity source=null;
        RackPickupPlan sourcePlan=null;
        boolean onNode=level.getBlockState(node.above().offset(-1,0,-1))
                .getBlock() instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock;
        if(loading&&!onNode){
            for(RackBlockEntity rack:WarehouseAislePriority.stagedOutputRacks(level,computer)){
                if(!rack.glassWrapped()||!WarehouseSortingReach.groundRack(level,rack))
                    continue;
                RackPickupPlan plan=RackPickupPlan.find(level,rack);
                if(plan!=null&&plan.height()==0){source=rack;sourcePlan=plan;break;}
            }
            if(source==null)return "WAITING: no wrapped pallet in a reachable OUTPUT aisle rack.";
        }
        for(var entity:level.getAllEntities())
            if(entity instanceof StorageGolem golem&&golem.nodePushActive()
                    &&node.equals(golem.nodePushNode()))
                return "IN PROGRESS: Two-Golem node transfer already assigned.";
        ArrayList<StorageGolemShelfEntity> shelves=new ArrayList<>(2);
        for(var shelf:network.shelves()){
            for(int count=0;count<shelf.docked()&&shelves.size()<2;count++)shelves.add(shelf);
            if(shelves.size()==2)break;
        }
        if(shelves.size()!=2)return "WAITING: Two docked push Golems needed.";
        // Two distinct dry service cells OUTSIDE the 2x2 pallet footprint.
        BlockPos[] goals=null;
        for(int dx=-1;dx<=1&&goals==null;dx+=2)
            for(int dz=-1;dz<=1&&goals==null;dz+=2){
                BlockPos a=node.offset(dx*2,1,dz);
                BlockPos d=node.offset(dx*2,1,-dz);
                if(StorageGolemReturnPath.walkable(level,a)
                        &&StorageGolemReturnPath.walkable(level,d)
                        &&a.distSqr(d)>=4)goals=new BlockPos[]{a,d};
            }
        if(goals==null)return "WAITING: Clear two-wide walking approach beside node.";
        String[] names=new String[2];
        for(int i=0;i<2;i++){
            names[i]=shelves.get(i).takeNamed();
            if(names[i]==null){
                for(int j=0;j<i;j++)shelves.get(j).addNamed(names[j]);
                return "WAITING: Push Golems changed while reserving.";
            }
        }
        UUID mission=UUID.randomUUID();
        ArrayList<StorageGolem> spawned=new ArrayList<>(2);
        for(int i=0;i<2;i++){
            var golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null)break;
            golem.assignPlayerCrew(shelves.get(i).getBlockPos(),i,owner);
            golem.setGolemName(names[i]);
            BlockPos sourceRoot=source==null?BlockPos.ZERO:source.getBlockPos();
            BlockPos sourceApproach=sourcePlan==null?BlockPos.ZERO:
                    sourcePlan.goal(i,false);
            int sourceFloor=sourcePlan==null?0:sourcePlan.floorY();
            golem.beginNodePush(mission,node,vehicle,goals[i],loading,
                    sourceRoot,sourceApproach,sourceFloor);
            BlockPos departure=StorageGolemShelfDoor.exit(level,shelves.get(i));
            if(departure==null){golem.discard();break;}
            golem.setPos(departure.getX()+.5,departure.getY(),departure.getZ()+.5);
            if(!level.addFreshEntity(golem))break;
            spawned.add(golem);
        }
        if(spawned.size()!=2){
            for(var golem:spawned)golem.discard();
            for(int i=0;i<2;i++)shelves.get(i).addNamed(names[i]);
            return "WAITING: Could not safely launch both Golems.";
        }
        return "Two Golems dispatched to "+(loading?"push pallet onto":"retrieve pallet from")
                +" adjacent train/conveyor at Pallet Transfer Node.";
    }
    public static void tryComplete(ServerLevel level,StorageGolem first){
        if(!first.nodePushActive()||first.nodePushIndex()!=0)return;
        StorageGolem partner=null;
        for(var entity:level.getAllEntities())
            if(entity instanceof StorageGolem candidate
                    &&candidate!=first&&candidate.nodePushActive()
                    &&first.nodePushMission().equals(candidate.nodePushMission())
                    &&candidate.nodePushIndex()==1){partner=candidate;break;}
        if(partner==null)return;
        if(!first.nodePushSource().equals(BlockPos.ZERO)&&!first.nodePushFromSource()){
            if(!first.nodePushReady(level)||!partner.nodePushReady(level))return;
            if(!(level.getBlockEntity(first.nodePushSource()) instanceof RackBlockEntity rack))
                return;
            if(!rack.startPull(first.nodePushMission(),first.nodePushSourceFloor()))
                return;
            if(rack.pullProgress()<RackPickupPlan.PULL_TICKS)return;
            if(!rack.liftToCarrier(first))return;
            first.nodePushDepartSource();
            partner.nodePushDepartSource();
            return;
        }
        if(!first.nodePushReady(level)||!partner.nodePushReady(level))return;
        // Two workers visibly walk into place, stop, then push together.
        if(first.tickCount<15||partner.tickCount<15)return;
        boolean transferred=false;
        try{
            Class<?> bridge=Class.forName("com.caszgamermd.palletsplus.transport.NodePushBridge");
            boolean fromRack=first.nodePushFromSource()&&first.holdsPallet();
            var method=fromRack?
                bridge.getMethod("completeFromCrew",ServerLevel.class,BlockPos.class,
                        UUID.class,StorageGolem.class):
                bridge.getMethod("complete",ServerLevel.class,BlockPos.class,UUID.class,boolean.class);
            transferred=Boolean.TRUE.equals(fromRack?
                method.invoke(null,level,first.nodePushNode(),first.nodePushVehicle(),first):
                method.invoke(null,level,first.nodePushNode(),
                        first.nodePushVehicle(),first.nodePushLoading()));
        }catch(ReflectiveOperationException ignored){}
        if(!transferred)return; // Both remain ready; original cargo never disappears.
        first.finishNodePush();
        partner.finishNodePush();
    }
    private PalletNodePushCrew(){}
}
