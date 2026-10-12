package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** One loaded-only discovery pass for every PTP touching a computer or
 * a member of its physically linked Storage Golem shelf group. */
public final class StoragePtpNetwork {
    private StoragePtpNetwork(){}
    public static List<PtpBlockEntity> attached(ServerLevel level,BlockPos computer,
            List<StorageGolemShelfEntity> shelves){
        List<BlockPos> contacts=new ArrayList<>();
        contacts.add(computer);
        for(StorageGolemShelfEntity shelf:shelves)contacts.add(shelf.getBlockPos());
        Set<BlockPos> seen=new HashSet<>();
        List<PtpBlockEntity> result=new ArrayList<>();
        for(BlockPos from:contacts)for(Direction direction:Direction.values()){
            BlockPos adjacent=from.relative(direction);
            if(!level.hasChunkAt(adjacent))continue;
            var state=level.getBlockState(adjacent);
            if(!(state.getBlock() instanceof PtpBlock))continue;
            int part=state.getValue(PtpBlock.PART);
            if(part==0)continue;
            BlockPos center=PtpBlock.root(adjacent,part).offset(1,0,1);
            if(!level.hasChunkAt(center)||!seen.add(center))continue;
            if(level.getBlockEntity(center) instanceof PtpBlockEntity point)
                result.add(point);
        }
        result.sort(Comparator.comparingLong(p->p.getBlockPos().asLong()));
        return result;
    }
}
