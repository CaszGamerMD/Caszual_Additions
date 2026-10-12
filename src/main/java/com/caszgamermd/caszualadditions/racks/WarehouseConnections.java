package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * A Storage Golem Shelf can attach either directly to a Rack Computer or to
 * ANY frame segment of a rack belonging to that computer's connected rack
 * network. Shelf-to-shelf adjacency extends the resulting shelf group.
 *
 * Both directions use physical loaded-chunk contact only: no remote
 * inventory reads, forced chunk loads or magic long-range shelf links.
 */
public final class WarehouseConnections {
    private WarehouseConnections(){}

    private static void collectShelf(ServerLevel level,BlockPos at,
            Map<BlockPos,StorageGolemShelfEntity> result){
        if(!level.hasChunkAt(at)||result.containsKey(at)
                ||!level.getBlockState(at).is(RackContent.STORAGE_SHELF))return;
        for(StorageGolemShelfEntity shelf:StorageGolemShelfEntity.linked(level,at))
            result.putIfAbsent(shelf.getBlockPos(),shelf);
    }

    /** Find all physical shelf groups touching this computer's connected
     * rack network or touching the computer itself. */
    public static List<StorageGolemShelfEntity> shelvesForComputer(
            ServerLevel level,BlockPos computer){
        return WarehouseNetwork.fromComputer(level,computer).shelves();
    }

    /** Resolve a connected computer from any shelf group touching racks.
     * Scan a connected rack network only once per unique rack root; a
     * computer can touch any of a rack's twelve physical frame segments. */
    public static BlockPos computerForShelves(ServerLevel level,
            List<StorageGolemShelfEntity> group){
        List<BlockPos> computers=WarehouseNetwork.fromShelfGroup(level,group).computers();
        return computers.isEmpty()?null:computers.getFirst();
    }
}
