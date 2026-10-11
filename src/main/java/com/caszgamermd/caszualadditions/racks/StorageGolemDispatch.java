package com.caszgamermd.caszualadditions.racks;

import java.util.List;
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
    private static boolean anyAccess(ServerLevel level,BlockPos place){
        return level.hasChunkAt(place);
    }
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
        if(group.isEmpty()||!group.get(0).getBlockPos().equals(shelf.getBlockPos()))return;
        BlockPos terminal=computer(level,group);
        if(terminal==null)return;
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
