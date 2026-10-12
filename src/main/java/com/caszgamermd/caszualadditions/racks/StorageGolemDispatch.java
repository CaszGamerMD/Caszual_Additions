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
        for(StorageGolemShelfEntity shelf:group){
            for(Direction side:Direction.values()){
                BlockPos adjacent=shelf.getBlockPos().relative(side);
                if(level.hasChunkAt(adjacent)
                        &&level.getBlockState(adjacent).is(RackContent.RACK_COMPUTER))
                    return adjacent;
            }
        }
        return null;
    }
    private static PtpBlockEntity inputPoint(ServerLevel level,
            List<StorageGolemShelfEntity> group,BlockPos computer){
        for(StorageGolemShelfEntity shelf:group)
            for(Direction dir:Direction.values()){
                BlockPos p=shelf.getBlockPos().relative(dir);
                if(!level.hasChunkAt(p))continue;
                PtpBlockEntity ptp=PtpBlock.controller(level,p);
                if(ptp!=null &&ptp.hasPallet()&&ptp.input()&&!ptp.sealed()&&!ptp.isEmpty())
                    return ptp;
            }
        for(Direction dir:Direction.values()){
            BlockPos p=computer.relative(dir);
            if(!level.hasChunkAt(p))continue;
            PtpBlockEntity ptp=PtpBlock.controller(level,p);
            if(ptp!=null &&ptp.hasPallet()&&ptp.input()&&!ptp.sealed()&&!ptp.isEmpty())
                return ptp;
        }
        return null;
    }
    static PtpBlockEntity outputPoint(ServerLevel level,
            List<StorageGolemShelfEntity> group,BlockPos computer){
        for(StorageGolemShelfEntity shelf:group)
            for(Direction dir:Direction.values()){
                BlockPos p=shelf.getBlockPos().relative(dir);
                if(!level.hasChunkAt(p))continue;
                PtpBlockEntity point=PtpBlock.controller(level,p);
                if(point!=null&&!point.input())
                    return point;
            }
        for(Direction dir:Direction.values()){
            BlockPos p=computer.relative(dir);
            if(!level.hasChunkAt(p))continue;
            PtpBlockEntity point=PtpBlock.controller(level,p);
            if(point!=null&&!point.input())
                return point;
        }
        return null;
    }
    /** Only a directly shelf-attached magazine belongs to this worker group.
     * Resolving either half of the two-high tank keeps placement flexible. */
    private static ShulkerTankEntity tank(ServerLevel level,
            List<StorageGolemShelfEntity> shelves){
        for(StorageGolemShelfEntity shelf:shelves)
            for(Direction side:Direction.values()){
                BlockPos adjacent=shelf.getBlockPos().relative(side);
                if(!level.hasChunkAt(adjacent))continue;
                ShulkerTankEntity tank=ShulkerTankBlock.controller(level,adjacent);
                if(tank!=null&&!tank.isEmpty())return tank;
            }
        return null;
    }
    public static int linkedEmptyShulkers(ServerLevel level,
            List<StorageGolemShelfEntity> shelves){
        java.util.Set<BlockPos> visited=new java.util.HashSet<>();
        int count=0;
        for(StorageGolemShelfEntity shelf:shelves)
            for(Direction side:Direction.values()){
                BlockPos p=shelf.getBlockPos().relative(side);
                if(!level.hasChunkAt(p))continue;
                ShulkerTankEntity tank=ShulkerTankBlock.controller(level,p);
                if(tank!=null&&visited.add(tank.getBlockPos()))count+=tank.count();
            }
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
        PtpBlockEntity point=outputPoint(level,group,terminal);
        if(point==null||!point.readyForOutput())return false;
        PtpBlockEntity.OutputOrder order=point.nextOutputOrder();
        if(order==null)return false;
        ShulkerTankEntity magazine=tank(level,group);
        ItemStack emptyBox=magazine==null?ItemStack.EMPTY:magazine.peek();
        for(RackBlockEntity rack:RackNetwork.fromComputer(level,terminal)){
            if(!rack.hasPallet()||rack.isEmpty())continue;
            for(int i=0;i<rack.getContainerSize();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem())
                        .toString().equals(order.itemId()))continue;
                // A packed trip can carry 27 original stacks. The preview
                // checks the actual filled-box components against the PTP
                // filter before reserving any inventory or taking a box.
                ItemStack preview=emptyBox.isEmpty()||!stack.getItem().canFitInsideContainerItems()
                        ?ItemStack.EMPTY:StorageGolemPacking.fill(emptyBox,
                            List.of(stack.copyWithCount(Math.min(
                                    stack.getCount(),order.remaining()))));
                boolean pack=!preview.isEmpty() && freeRoom(point,preview)>0;
                int amount=pack?Math.min(order.remaining(),27*64)
                        :Math.min(order.remaining(),
                                Math.min(stack.getCount(),freeRoom(point,stack)));
                if(amount<=0)continue;
                StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                        net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if(worker==null)return false;
                int ticket=point.reserveOutput(order.itemId(),amount);
                if(ticket==0)return false;
                worker.assignOutput(shelf.getBlockPos(),rack.getBlockPos(),
                        point.getBlockPos(),stack,ticket,amount);
                if(pack)worker.assignPackedOutput(magazine.getBlockPos(),amount);
                worker.setPos(shelf.getBlockPos().getX()+.5,
                        shelf.getBlockPos().getY()+1,shelf.getBlockPos().getZ()+.5);
                if(!shelf.take()){point.releaseOutput(ticket);return false;}
                if(!level.addFreshEntity(worker)){
                    shelf.add();point.releaseOutput(ticket);
                }
                return true;
            }
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

        // Only remove a worker token once an entity is ready to be spawned.
        StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if(worker==null)return;
        worker.assign(shelf.getBlockPos(),ptp.getBlockPos(),dest,sample);
        worker.setPos(shelf.getBlockPos().getX()+.5,
                shelf.getBlockPos().getY()+1,shelf.getBlockPos().getZ()+.5);
        if(!shelf.take())return;
        if(!level.addFreshEntity(worker))shelf.add();
    }
}
