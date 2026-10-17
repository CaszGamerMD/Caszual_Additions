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
        if(old==null)return "WAITING: target is no longer a wrapped ground pallet.";
        // Across the loaded dimension, do not spawn a second crew with the
        // same owner while another delivery/recovery rig exists.
        for(var entity:level.getAllEntities())
            if(entity instanceof MovingPallet rig&&rig.owns(player.getUUID()))
                return "WAITING: existing four-Golem crew already assigned.";

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
        courier.setPos(staging.getX()+1.5,staging.getY(),staging.getZ()+.5);
        // Before withdrawing from a magazine, move ownership to the one
        // actual Golem entity. The other three are only rendered on the rig.
        if(!carrier.splitRemoteCourier(courier))
            return "Unable to split the three-Golem lift crew from the empty-pallet courier.";
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
        if(consumed==4&&level.addFreshEntity(carrier)){
            if(level.addFreshEntity(courier))
                return "Three Golems dispatched to lift the wrapped target while "
                        +"a fourth pushes the unwrapped empty pallet to "
                        +old.getBlockPos().toShortString()
                        +". The loaded team will step aside, wait for placement, "
                        +"rejoin, and return together.";
            // Spawning is synchronous on the server thread. Both entities
            // must exist, otherwise restore stock and shelf token counts.
            carrier.discard();
        }
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
