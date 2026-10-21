package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A named Aisle Tag is inherited across its physical aisle row using the
 * same RackNetwork.aisle resolver as the rack sign and Rack Computer.
 *
 * Only the exact, user-entered "Input" name enables remote drop-off
 * priority. Ordinary labels containing the word (e.g. "Input 2") do not
 * unexpectedly change warehouse behavior.
 */
public final class WarehouseAislePriority {
    private WarehouseAislePriority(){}

    public static boolean isInput(String label){
        return label!=null&&"Input".equalsIgnoreCase(label.trim());
    }

    public static boolean isInputAisle(Level level,RackBlockEntity rack){
        return rack!=null&&isInput(RackNetwork.aisle(level,rack));
    }

    /**
     * Find a vacant, accessible pallet rack belonging to an Input aisle.
     * Do not route to an occupied rack, a rack containing loose contents,
     * or a rack whose stairs/approach are currently obstructed.
     *
     * Called only during a returning remote pickup, not during ordinary
     * player-operated rack deliveries or local warehouse sorting.
     */
    public static @Nullable RackBlockEntity firstAvailableInputRack(
            ServerLevel level,List<RackBlockEntity> racks){
        return firstAvailableInputRack(level,racks,null);
    }
    /** Temporary exclusions let a blocked pallet crew try a free INPUT PTP
     * instead of repeatedly selecting the same unreachable Input rack. */
    public static @Nullable RackBlockEntity firstAvailableInputRack(
            ServerLevel level,List<RackBlockEntity> racks,@Nullable BlockPos exclude){
        for(RackBlockEntity rack:racks){
            if(rack.hasPallet()||!rack.isEmpty())continue;
            if(exclude!=null&&exclude.equals(rack.getBlockPos()))continue;
            if(!isInputAisle(level,rack))continue;
            if(!WarehouseSortingReach.groundRack(level,rack))continue;
            RackPickupPlan plan=RackPickupPlan.find(level,rack);
            if(plan==null||plan.height()!=0)continue;
            return rack;
        }
        return null;
    }
}
