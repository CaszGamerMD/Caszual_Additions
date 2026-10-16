package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Bounded, loaded-chunk-only dispatch from linked shelves to adjacent INPUT
 * transfer points and physically connected pallet racks. No teleporting cargo.
 */
public final class StorageGolemDispatch {
    private StorageGolemDispatch(){}
    // Only loaded shelf groups and their physically adjacent computer/point
    // are discovered; workers never force-load remote chunks.
    private static BlockPos computer(ServerLevel level,List<StorageGolemShelfEntity> group){
        return WarehouseConnections.computerForShelves(level,group);
    }
    private static PtpBlockEntity inputPoint(ServerLevel level,
            List<StorageGolemShelfEntity> group,BlockPos computer){
        for(PtpBlockEntity ptp:WarehouseNetwork.fromComputer(level,computer).points())
            if(ptp.hasPallet()&&ptp.input()&&!ptp.sealed()&&!ptp.isEmpty())
                return ptp;
        return null;
    }
    static PtpBlockEntity outputPoint(ServerLevel level,
            List<StorageGolemShelfEntity> group,BlockPos computer){
        for(PtpBlockEntity point:WarehouseNetwork.fromComputer(level,computer).points())
            if(!point.input())return point;
        return null;
    }
    /** A Shulker Tank can now connect through any warehouse component,
     * including IGC floors and adjacent racks. */
    private static ShulkerTankEntity tank(ServerLevel level,
            List<StorageGolemShelfEntity> shelves){
        for(ShulkerTankEntity tank:WarehouseNetwork.fromShelfGroup(level,shelves).tanks())
            if(!tank.isEmpty())return tank;
        return null;
    }
    public static int linkedEmptyShulkers(ServerLevel level,
            List<StorageGolemShelfEntity> shelves){
        int count=0;
        for(ShulkerTankEntity tank:WarehouseNetwork.fromShelfGroup(level,shelves).tanks())
            count+=tank.count();
        return count;
    }
    /** Capacity in actual compatible item slots, including partial stacks. */
    private static int freeRoom(PtpBlockEntity point,ItemStack item){
        if(!point.readyForOutput()||item.isEmpty())return 0;
        int capacity=0;
        for(int i=0;i<point.getContainerSize();i++){
            if(!point.canPlaceItem(i,item))continue;
            ItemStack existing=point.getItem(i);
            if(existing.isEmpty())capacity+=item.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(existing,item))
                capacity+=Math.max(0,existing.getMaxStackSize()-existing.getCount());
            if(capacity>=64)return 64;
        }
        return capacity;
    }
    private static boolean dispatchOutput(ServerLevel level,StorageGolemShelfEntity shelf,
            List<StorageGolemShelfEntity> group,BlockPos terminal){
        PtpBlockEntity output=outputPoint(level,group,terminal);
        if(output==null||!output.readyForOutput())return false;
        PtpBlockEntity.OutputOrder order=output.nextOutputOrder();
        if(order==null)return false;
        ShulkerTankEntity station=null;
        for(ShulkerTankEntity candidate:WarehouseNetwork
                .fromShelfGroup(level,group).tanks()){
            if(candidate.attachedOutput(level)==output){
                station=candidate;break;
            }
        }
        // An order cannot bypass the packing machine: the complete shulker
        // is always transferred through a physically adjacent Hopper Adapter.
        if(station==null)return false;
        BlockPos shelfDoor=StorageGolemShelfDoor.exit(level,shelf);
        if(shelfDoor==null)return false; // Don't consume the worker token.
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,terminal)){
            if(!rack.hasPallet()||rack.isEmpty())continue;
            List<ItemStack> preview=new java.util.ArrayList<>(9);
            int amount=0;
            for(int i=0;i<rack.getContainerSize()&&preview.size()<9
                    &&amount<order.remaining();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.isEmpty()||!BuiltInRegistries.ITEM.getKey(stack.getItem())
                        .toString().equals(order.itemId())
                        ||!stack.getItem().canFitInsideContainerItems())continue;
                int quantity=Math.min(stack.getCount(),
                        Math.min(order.remaining()-amount,9*64-amount));
                if(quantity<=0)break;
                preview.add(stack.copyWithCount(quantity));
                amount+=quantity;
            }
            if(amount==0||!station.canAccept(preview,level,output))continue;
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                    net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if(worker==null)return false;
            int ticket=output.reserveOutput(order.itemId(),amount);
            if(ticket==0)return false;
            worker.assignOutput(shelf.getBlockPos(),rack.getBlockPos(),
                    station.getBlockPos(),preview.getFirst(),ticket,amount);
            worker.assignStationOrder(station.getBlockPos(),
                    output.getBlockPos(),amount);
            StorageGolemShelfDoor.emerge(worker,shelfDoor,
                    StorageGolemShelfDoor.front(shelf));
            if(!shelf.take()){output.releaseOutput(ticket);return false;}
            if(!level.addFreshEntity(worker)){
                shelf.add();output.releaseOutput(ticket);
            }
            return true;
        }
        return false;
    }
    public static boolean canInsert(ServerLevel level,BlockPos pos,ItemStack item){
        if(item.isEmpty() || !level.hasChunkAt(pos)
                ||!(level.getBlockEntity(pos) instanceof Container container))return false;
        for(int i=0;i<container.getContainerSize();i++){
            if(!container.canPlaceItem(i,item))continue;
            ItemStack existing=container.getItem(i);
            if(existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing,item)
                    &&existing.getCount()<existing.getMaxStackSize()))return true;
        }
        return false;
    }
    public static int insert(ServerLevel level,BlockPos pos,ItemStack moving){
        if(moving.isEmpty()||!level.hasChunkAt(pos)
                ||!(level.getBlockEntity(pos) instanceof Container dst))return 0;
        int added=0;
        // Match partial stacks first; avoid splitting existing item
        // component variants into incompatible stacks.
        for(int pass=0;pass<2 &&!moving.isEmpty();pass++){
            for(int i=0;i<dst.getContainerSize() &&!moving.isEmpty();i++){
                if(!dst.canPlaceItem(i,moving))continue;
                ItemStack slot=dst.getItem(i);
                if(pass==0 &&(slot.isEmpty()
                        ||!ItemStack.isSameItemSameComponents(slot,moving)))continue;
                if(pass==1 &&!slot.isEmpty())continue;
                int room=slot.isEmpty()?moving.getMaxStackSize()
                        :slot.getMaxStackSize()-slot.getCount();
                if(room<=0)continue;
                int count=Math.min(room,moving.getCount());
                ItemStack merged=slot.isEmpty()?moving.copyWithCount(count):slot.copy();
                if(!slot.isEmpty())merged.grow(count);
                dst.setItem(i,merged);
                // Guard against inventories refusing setItem when filtered.
                ItemStack confirmed=dst.getItem(i);
                if(!ItemStack.isSameItemSameComponents(confirmed,merged)
                        ||confirmed.getCount()!=merged.getCount())continue;
                moving.shrink(count);
                added+=count;
            }
        }
        return added;
    }
    public static BlockPos findRackFor(ServerLevel level,BlockPos home,ItemStack sample){
        if(sample.isEmpty() || !level.hasChunkAt(home))return null;
        List<StorageGolemShelfEntity> group=StorageGolemShelfEntity.linked(level,home);
        BlockPos computer=computer(level,group);
        if(computer==null)return null;
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,computer)){
            if(rack.hasPallet()&&canInsert(level,rack.getBlockPos(),sample))
                return rack.getBlockPos();
        }
        return null;
    }
    public static void tryDispatch(ServerLevel level,StorageGolemShelfEntity shelf){
        if(shelf.docked()==0)return;
        List<StorageGolemShelfEntity> group=StorageGolemShelfEntity.linked(
                level,shelf.getBlockPos());
        if(group.isEmpty())return;
        BlockPos terminal=computer(level,group);
        if(terminal==null)return;
        // Real outbound orders take priority; input sorting continues whenever
        // there is no dispatchable output item or output space.
        if(dispatchOutput(level,shelf,group,terminal))return;
        PtpBlockEntity ptp=inputPoint(level,group,terminal);
        if(ptp==null)return;
        ItemStack sample=ItemStack.EMPTY;
        BlockPos dest=null;
        for(int i=0;i<ptp.getContainerSize();i++){
            ItemStack test=ptp.getItem(i);
            if(test.isEmpty())continue;
            dest=findRackFor(level,shelf.getBlockPos(),test);
            if(dest!=null){sample=test.copyWithCount(1);break;}
        }
        if(dest==null)return;
        BlockPos shelfDoor=StorageGolemShelfDoor.exit(level,shelf);
        if(shelfDoor==null)return; // Worker waits behind its closed door.

        // Only remove a worker token once an entity is ready to be spawned.
        StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if(worker==null)return;
        worker.assign(shelf.getBlockPos(),ptp.getBlockPos(),dest,sample);
        StorageGolemShelfDoor.emerge(worker,shelfDoor,
                StorageGolemShelfDoor.front(shelf));
        if(!shelf.take())return;
        if(!level.addFreshEntity(worker))shelf.add();
    }
}
