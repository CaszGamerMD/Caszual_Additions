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
        var points=WarehouseNetwork.fromComputer(level,computer).points();
        if(level.getBlockEntity(computer) instanceof WarehouseComputerBlockEntity settings
                &&!settings.orderOutput().equals(BlockPos.ZERO)){
            // A selected output is an explicit assignment; do not silently
            // reroute the order to an unrelated PTP when it is busy.
            for(PtpBlockEntity point:points)
                if(point.getBlockPos().equals(settings.orderOutput()))
                    return point.readyForOutput()?point:null;
            return null;
        }
        // Builder requests reserve an otherwise empty OUTPUT PTP. Service
        // only this pallet when it has queued work, not whichever PTP
        // happens to appear first in the warehouse scan.
        for(PtpBlockEntity point:points)
            if(point.builderReserved()&&point.readyForOutput()
                    &&point.pendingOutputOrders()>0)return point;
        for(PtpBlockEntity point:points)
            if(point.readyForOutput()&&point.pendingOutputOrders()>0)return point;
        for(PtpBlockEntity point:points)
            if(!point.input()&&!point.builderReserved())return point;
        return null;
    }
    /** Builder and manually queued output orders are explicit work, not
     * automatic INPUT sorting. Recalling Golems disables auto sorting, but
     * a later Builder order must still dispatch from a docked shelf.
     * Reserve the output ticket before spawning, so two shelf ticks never
     * assign the same remaining items to multiple pickers. */
    public static boolean tryDispatchQueuedOutput(ServerLevel level,
            StorageGolemShelfEntity shelf){
        if(shelf.docked()==0)return false;
        List<StorageGolemShelfEntity> group=StorageGolemShelfEntity.linked(
                level,shelf.getBlockPos());
        if(group.isEmpty())return false;
        BlockPos terminal=computer(level,group);
        return terminal!=null&&dispatchOutput(level,shelf,group,terminal);
    }
    /** The computer owns the packer limit (one by default). Count only
     * loaded real order-picking Golems linked to this warehouse network.
     * The check occurs on dispatch, not every Golem/server tick.
     */
    private static boolean packerSlotFree(ServerLevel level,BlockPos computer){
        if(!(level.getBlockEntity(computer) instanceof WarehouseComputerBlockEntity cfg))
            return true;
        var network=WarehouseNetwork.fromComputer(level,computer);
        java.util.Set<BlockPos> outputs=new java.util.HashSet<>();
        for(PtpBlockEntity point:network.points())
            outputs.add(point.getBlockPos());
        int running=level.getEntitiesOfClass(StorageGolem.class,
                new net.minecraft.world.phys.AABB(computer).inflate(160,32,160),
                worker->worker.activeOrderPacking()
                        &&outputs.contains(worker.packingOutput())).size();
        return running<cfg.orderPackers();
    }
    /** Reuse the same Golem immediately after delivering three physical
     * stacks to a Shulker Station, with another checked FIFO reservation.
     * No new Golem, item copy, instant rack withdrawal, or shelf round trip.
     */
    static boolean continuePacking(ServerLevel level,StorageGolem worker,
            BlockPos home,PtpBlockEntity output,ShulkerTankEntity station){
        if(output==null||station==null||output.nextOutputOrder()==null)return false;
        PtpBlockEntity.OutputOrder order=output.nextOutputOrder();
        BlockPos computer=WarehouseConnections.computerForShelves(level,
                StorageGolemShelfEntity.linked(level,home));
        if(computer==null)return false;
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,computer)){
            if(!rack.hasPallet()||rack.isEmpty()
                    ||!WarehouseSortingReach.accessible(level,rack))continue;
            List<ItemStack> preview=new java.util.ArrayList<>(3);
            int amount=0;
            for(int i=0;i<rack.getContainerSize()&&preview.size()<3
                    &&amount<order.remaining();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.isEmpty()||!BuiltInRegistries.ITEM.getKey(stack.getItem())
                        .toString().equals(order.itemId())
                        ||!stack.getItem().canFitInsideContainerItems())continue;
                int quantity=Math.min(stack.getCount(),
                        Math.min(order.remaining()-amount,3*64-amount));
                if(quantity<=0)break;
                preview.add(stack.copyWithCount(quantity));amount+=quantity;
            }
            if(amount<=0||!station.canAccept(preview,level,output))continue;
            int ticket=output.reserveOutput(order.itemId(),amount);
            if(ticket==0)return false;
            worker.assignOutput(home,rack.getBlockPos(),
                    station.getBlockPos(),preview.getFirst(),ticket,amount);
            worker.assignStationOrder(station.getBlockPos(),
                    output.getBlockPos(),amount);
            return true;
        }
        return false;
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
        // A Builder ticket is pinned to its reserved PTP. An operator may
        // change the preferred output while packing is underway, but that
        // must not strand the physical golem crew or orphan the old order.
        // Manual orders still use the currently selected outputPoint().
        PtpBlockEntity output=null;
        for(PtpBlockEntity point:WarehouseNetwork.fromComputer(level,terminal).points()){
            if(point.builderReserved()&&point.readyForOutput()
                    &&point.pendingOutputOrders()>0){
                output=point;
                break;
            }
        }
        if(output==null)output=outputPoint(level,group,terminal);
        if(output==null||!output.readyForOutput()
                ||!packerSlotFree(level,terminal))return false;
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
            if(!rack.hasPallet()||rack.isEmpty()
                    ||!WarehouseSortingReach.accessible(level,rack))continue;
            List<ItemStack> preview=new java.util.ArrayList<>(3);
            int amount=0;
            for(int i=0;i<rack.getContainerSize()&&preview.size()<3
                    &&amount<order.remaining();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.isEmpty()||!BuiltInRegistries.ITEM.getKey(stack.getItem())
                        .toString().equals(order.itemId())
                        ||!stack.getItem().canFitInsideContainerItems())continue;
                int quantity=Math.min(stack.getCount(),
                        Math.min(order.remaining()-amount,3*64-amount));
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
            String name=shelf.takeNamed();
            if(name==null){output.releaseOutput(ticket);return false;}
            worker.setGolemName(name);
            if(!level.addFreshEntity(worker)){
                shelf.addNamed(name);output.releaseOutput(ticket);
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
        // Copper-Golem sorting preference:
        // 1. a pallet ALREADY containing this exact item/component;
        // 2. an explicit whitelist allowing the sample;
        // 3. any other pallet allowing it (including empty/unfiltered and
        //    blacklist pallets that do not exclude this item).
        // Preserve source-order tie breaking to avoid job ping-pong.
        RackBlockEntity best=null;
        int bestPriority=Integer.MAX_VALUE;
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,computer)){
            if(!rack.hasPallet()||!WarehouseSortingReach.accessible(level,rack)
                    ||!canInsert(level,rack.getBlockPos(),sample))
                continue;
            int priority=2;
            for(int slot=0;slot<rack.getContainerSize();slot++){
                ItemStack existing=rack.getItem(slot);
                if(!existing.isEmpty()
                        &&ItemStack.isSameItemSameComponents(existing,sample)){
                    priority=0;break;
                }
            }
            if(priority!=0&&rack.filterUpgrade().is(RackContent.PALLET_FILTER)
                    &&rack.filterMode()==0
                    &&!rack.filterTarget().isBlank()&&rack.allows(sample))
                priority=1;
            if(priority<bestPriority){
                bestPriority=priority;
                best=rack;
                if(priority==0)break;
            }
        }
        return best==null?null:best.getBlockPos();
    }
    /**
     * Pallet magazine restocking is a real one-worker job, not a magic
     * direct transfer. Source is an empty pallet on an INPUT PTP; destination
     * is a Stack of Pallets inside a reachable warehouse rack.
     */
    private static boolean dispatchPalletMagazine(ServerLevel level,
            StorageGolemShelfEntity shelf,BlockPos terminal){
        RackBlockEntity target=null;
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,terminal)){
            if(!rack.hasPallet()||!WarehouseSortingReach.accessible(level,rack))continue;
            for(int i=0;i<rack.getContainerSize();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.is(RackContent.STACK_OF_PALLETS)
                        &&StackOfPalletsItem.count(stack)<StackOfPalletsItem.CAPACITY){
                    target=rack;break;
                }
            }
            if(target!=null)break;
        }
        // A physical Stack of Pallets block is preferred if it has space;
        // old magazine items in racks remain compatible with saved worlds.
        StackOfPalletsBlockEntity placed=null;
        for(StackOfPalletsBlockEntity magazine:
                WarehouseNetwork.fromComputer(level,terminal).magazines())
            if(magazine.count()<StackOfPalletsItem.CAPACITY){
                placed=magazine;break;
            }
        if(target==null&&placed==null)return false;
        BlockPos stockDestination=placed!=null?placed.getBlockPos():target.getBlockPos();
        for(PtpBlockEntity ptp:WarehouseNetwork.fromComputer(level,terminal).points()){
            if(!ptp.input()||!ptp.hasPallet()||ptp.sealed()
                    ||!ptp.emptyCargo()||!StackOfPalletsItem.isPallet(ptp.palletStack()))
                continue;
            // A second shelf may be ticking: do not assign two Golems
            // to the same empty pallet.
            BlockPos source=ptp.getBlockPos();
            if(!level.getEntitiesOfClass(StorageGolem.class,
                    new net.minecraft.world.phys.AABB(source).inflate(96),
                    worker->worker.isPalletStackJobFrom(source)).isEmpty())
                continue;
            BlockPos door=StorageGolemShelfDoor.exit(level,shelf);
            if(door==null)return false;
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                    net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if(worker==null)return false;
            worker.assignPalletStack(shelf.getBlockPos(),source,stockDestination);
            StorageGolemShelfDoor.emerge(worker,door,
                    StorageGolemShelfDoor.front(shelf));
            String name=shelf.takeNamed();
            if(name==null)return false;
            worker.setGolemName(name);
            if(!level.addFreshEntity(worker))shelf.addNamed(name);
            return true;
        }
        return false;
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
        // Empty installed INPUT pallets feed Stack of Pallets magazines.
        if(dispatchPalletMagazine(level,shelf,terminal))return;
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
        String name=shelf.takeNamed();
        if(name==null)return;
        worker.setGolemName(name);
        if(!level.addFreshEntity(worker))shelf.addNamed(name);
    }
}
