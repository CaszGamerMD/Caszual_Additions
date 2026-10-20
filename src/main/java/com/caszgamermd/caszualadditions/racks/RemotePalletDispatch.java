package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;

/**
 * Four shelf tokens form one visible, moving, four-Golem pallet carrier.
 * An empty pallet is physically withdrawn from warehouse stock BEFORE the
 * rig departs. The crew delivers that pallet, swaps with the wrapped target,
 * and returns the 216-slot original to an INPUT PTP or vacant rack.
 *
 * No cargo teleportation. The corridor must be chunk-loaded as it travels.
 * Scanner requests can remain queued until both endpoints are loaded.
 */
public final class RemotePalletDispatch {
    private RemotePalletDispatch(){}

    public static String request(ServerLevel level,ServerPlayer player,
            BlockPos computer,BlockPos target){
        if(!level.hasChunkAt(computer)||!level.hasChunkAt(target))
            return "WAITING: warehouse or pickup area not loaded.";
        if(!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "INVALID: linked Rack Computer was removed.";
        PalletBlockEntity old=StorageGroundPalletTransfer.wrappedController(
                level,target);
        if(old==null)return "INVALID: target is no longer a wrapped ground pallet.";
        // A player can own many independent missions, each with four
        // distinct workers. Reserve the TARGET instead of reserving the
        // entire player: one pallet must never be collected by two crews.
        // The mission UUID is already persisted by all three pickup Golems
        // and by the replacement-pallet courier.
        for(var entity:level.getAllEntities()){
            if(entity instanceof MovingPallet rig
                    &&old.getBlockPos().equals(rig.remoteExchangeTarget()))
                return "INVALID: this pallet already has an active remote crew.";
            if(entity instanceof StorageGolem worker
                    &&worker.remotePickupActive()
                    &&old.getBlockPos().equals(worker.remotePickupTarget()))
                return "INVALID: this pallet already has an active remote crew.";
        }

        var network=WarehouseNetwork.fromComputer(level,computer);
        List<StorageGolemShelfEntity> shelves=new ArrayList<>(4);
        for(StorageGolemShelfEntity shelf:network.shelves()){
            for(int i=0;i<shelf.docked()&&shelves.size()<4;i++)
                shelves.add(shelf);
            if(shelves.size()==4)break;
        }
        if(shelves.size()<4)return "WAITING: four docked Golems required.";
        BlockPos staging=null;
        for(StorageGolemShelfEntity shelf:shelves){
            BlockPos door=StorageGolemShelfDoor.exit(level,shelf);
            if(door==null)continue;
            BlockPos candidate=door.relative(StorageGolemShelfDoor.front(shelf),1);
            boolean clear=true;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)
                if(!StorageGolemReturnPath.walkable(level,candidate.offset(x,0,z)))
                    clear=false;
            if(clear){staging=candidate;break;}
        }
        if(staging==null)return "WAITING: four-Golem departure aisle blocked.";
        // ALL three outbound workers must travel as separate actual mobs.
        // Validate an external 3-wide rally beside the destination BEFORE
        // taking any stock or shelf token from the linked warehouse.
        BlockPos rally=RemotePalletPickupCrew.rally(
                level,old.getBlockPos(),staging);
        if(rally.equals(BlockPos.ZERO))
            return "WAITING: no safe three-Golem rally aisle near target.";

        RackBlockEntity sourceRack=null;
        int sourceSlot=-1;
        ItemStack previous=ItemStack.EMPTY,fresh=ItemStack.EMPTY;
        ItemStack updated=ItemStack.EMPTY;
        // Placed magazines are first-class warehouse storage, not merely
        // decorations or items hidden inside another pallet.
        StackOfPalletsBlockEntity sourceMagazine=null;
        if(fresh.isEmpty()){
            for(StackOfPalletsBlockEntity magazine:network.magazines()){
                if(magazine.count()==0)continue;
                for(int slot=magazine.getContainerSize()-1;slot>=0;slot--){
                    ItemStack candidate=magazine.getItem(slot);
                    if(StackOfPalletsItem.isPallet(candidate)){
                        sourceMagazine=magazine;
                        fresh=candidate.copyWithCount(1);
                        break;
                    }
                }
                if(sourceMagazine!=null)break;
            }
        }
        if(fresh.isEmpty()){
        for(RackBlockEntity rack:network.racks()){
            for(int i=0;i<rack.getContainerSize();i++){
                ItemStack item=rack.getItem(i);
                if(!item.is(RackContent.STACK_OF_PALLETS)
                        ||StackOfPalletsItem.count(item)==0)continue;
                ItemStack change=item.copy();
                ItemStack extracted=StackOfPalletsItem.take(change);
                if(extracted.isEmpty()||!rack.canPlaceItem(i,change))continue;
                sourceRack=rack;sourceSlot=i;
                previous=item.copy();updated=change;fresh=extracted;
                break;
            }
            if(sourceRack!=null)break;
        }
        }
        PtpBlockEntity sourcePtp=null;
        if(fresh.isEmpty()){
            for(PtpBlockEntity ptp:network.points()){
                if(ptp.input()&&ptp.hasPallet()&&!ptp.sealed()&&ptp.emptyCargo()){
                    // removePallet() returns the effective item with its
                    // live PTP filter data. Prepare the rig with that same
                    // canonical stack or component comparison will reject
                    // every upgraded pallet and leave the job queued.
                    ItemStack candidate=com.caszgamermd.caszualadditions.pallet.PalletData
                            .applyFilterSettings(ptp.palletStack(),ptp);
                    if(StackOfPalletsItem.isPallet(candidate)){
                        sourcePtp=ptp;
                        fresh=candidate.copyWithCount(1);
                        break;
                    }
                }
            }
        }
        if(fresh.isEmpty())return "WAITING: fresh empty pallet needed in a magazine or INPUT PTP.";

        List<StorageGolem> identities=new ArrayList<>(4);
        for(int i=0;i<4;i++){
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(
                    level,EntitySpawnReason.EVENT);
            if(worker==null)return "Could not create four-Golem carrier.";
            worker.assignPlayerCrew(shelves.get(i).getBlockPos(),i,player.getUUID());
            identities.add(worker); // models only; never spawn duplicate bodies
        }
        MovingPallet carrier=RackContent.MOVING_PALLET_TYPE.create(
                level,EntitySpawnReason.EVENT);
        if(carrier==null||!carrier.prepare(player.getUUID(),identities,fresh,
                NonNullList.withSize(216,ItemStack.EMPTY))
                ||!carrier.beginRemoteExchange(old.getBlockPos(),computer))
            return "Unable to prepare remote replacement crew.";
        carrier.setPos(staging.getX()+.5,staging.getY(),staging.getZ()+.5);
        StorageGolem courier=identities.get(3);
        // The courier pushes a real TWO-WIDE deck. Never spawn it in
        // a one-cell aisle, then discover every route is blocked and leave
        // it standing still forever. Its start needs a checked 2x2 clear
        // passage AND must not overlap the three other actual workers.
        BlockPos courierStart=null;
        for(int step=2;step<=4&&courierStart==null;step++)
            for(net.minecraft.core.Direction dir:net.minecraft.core.Direction.Plane.HORIZONTAL){
                BlockPos trial=staging.relative(dir,step);
                if(!RemotePalletCourierRoute.clear(level,trial))continue;
                boolean crowd=false;
                for(int index=0;index<3;index++)
                    if(trial.distSqr(staging.offset(index-1,0,0))<4)
                        crowd=true;
                if(!crowd){courierStart=trial;break;}
            }
        if(courierStart==null)
            return "WAITING: separate two-wide courier departure aisle blocked.";
        courier.setPos(courierStart.getX()+.5,courierStart.getY(),
                courierStart.getZ()+.5);
        // The rig is ONLY an unspawned mission/UUID template here.
        // The THREE outbound workers are independent live StorageGolem
        // entities; there is no linked platform or shared hitbox until they
        // arrive, lift the target and merge at the remote service aisle.
        if(!carrier.splitRemoteCourier(courier))
            return "Unable to assign the independent empty-pallet courier.";
        BlockPos[] homes={
                shelves.get(0).getBlockPos(),shelves.get(1).getBlockPos(),
                shelves.get(2).getBlockPos(),shelves.get(3).getBlockPos()};
        for(int i=0;i<3;i++){
            StorageGolem worker=identities.get(i);
            worker.beginRemotePickup(carrier.getUUID(),courier.getUUID(),
                    old.getBlockPos(),computer,rally,homes);
            BlockPos start=staging.offset(i-1,0,0);
            if(!StorageGolemReturnPath.walkable(level,start))start=staging;
            worker.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
        }
        if(sourceRack!=null){
            sourceRack.setItem(sourceSlot,updated);
            if(!ItemStack.matches(sourceRack.getItem(sourceSlot),updated)){
                // No crew has been consumed yet. Restore the entire magazine
                // before reporting a rejected withdrawal to the scanner.
                sourceRack.setItem(sourceSlot,previous);
                return "WAITING: pallet magazine could not withdraw a fresh pallet.";
            }
        }else if(sourceMagazine!=null){
            ItemStack removed=sourceMagazine.take();
            if(!ItemStack.matches(removed,fresh)){
                if(!removed.isEmpty()&&!sourceMagazine.put(removed)){
                    BlockPos drop=sourceMagazine.getBlockPos();
                    net.minecraft.world.Containers.dropItemStack(level,
                            drop.getX()+.5,drop.getY()+.5,drop.getZ()+.5,removed);
                }
                return "WAITING: placed pallet stack contents changed during withdrawal.";
            }
        }else{
            ItemStack removed=sourcePtp.removePallet();
            if(!ItemStack.matches(removed,fresh)){
                // The PTP may have returned a different pallet than the one
                // inspected. Never abandon that real item on an early return.
                if(!removed.isEmpty()&&!sourcePtp.installPallet(removed)){
                    BlockPos drop=sourcePtp.getBlockPos();
                    net.minecraft.world.Containers.dropItemStack(level,
                            drop.getX()+.5,drop.getY()+.5,drop.getZ()+.5,removed);
                }
                return "WAITING: INPUT PTP pallet changed during withdrawal.";
            }
        }
        int consumed=0;
        for(StorageGolemShelfEntity shelf:shelves){
            if(!shelf.take())break;
            consumed++;
        }
        java.util.ArrayList<StorageGolem> spawned=new java.util.ArrayList<>(4);
        if(consumed==4){
            boolean accepted=true;
            for(int i=0;i<3;i++){
                StorageGolem worker=identities.get(i);
                if(!level.addFreshEntity(worker)){accepted=false;break;}
                spawned.add(worker);
            }
            if(accepted&&level.addFreshEntity(courier)){
                // The empty carrier template is never added to the world.
                // Its saved future UUID is used exactly once when these
                // three physical Golems reach the actual target pallet.
                return "Three Golems dispatched INDEPENDENTLY, empty-handed, to "
                        +old.getBlockPos().toShortString()
                        +"; the fourth is pushing the unwrapped fresh pallet. "
                        +"The three will link ONLY once they lift the target.";
            }
        }
        for(StorageGolem worker:spawned)worker.discard();
        // If the courier was spawned but another entity failed, it would
        // have been last; there is no partially committed four-worker party.

        for(int i=0;i<consumed;i++)shelves.get(i).add();
        // Roll back the warehouse's ONE withdrawn empty pallet on failure.
        if(sourceRack!=null)sourceRack.setItem(sourceSlot,previous);
        else if(sourceMagazine!=null&&!sourceMagazine.put(fresh)){
            BlockPos drop=sourceMagazine.getBlockPos();
            net.minecraft.world.Containers.dropItemStack(level,
                    drop.getX()+.5,drop.getY()+.5,drop.getZ()+.5,fresh);
        }
        else if(sourcePtp!=null&&!sourcePtp.installPallet(fresh))
            net.minecraft.world.Containers.dropItemStack(level,
                    staging.getX()+.5,staging.getY()+.5,staging.getZ()+.5,fresh);
        return "Unable to dispatch Golems; warehouse pallet stock restored.";
    }
}
