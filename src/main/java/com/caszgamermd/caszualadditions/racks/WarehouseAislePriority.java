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
 * Only exact, case-insensitive "Input" and "Output" names are operational.
 * Input receives remote returns; Output reserves bays for outbound shipping.
 * Other aisle labels remain ordinary storage.
 */
public final class WarehouseAislePriority {
    private WarehouseAislePriority(){}

    public static boolean isInput(String label){
        return label!=null&&"Input".equalsIgnoreCase(label.trim());
    }

    public static boolean isInputAisle(Level level,RackBlockEntity rack){
        return rack!=null&&isInput(RackNetwork.aisle(level,rack));
    }

    /** Exact user-defined shipping aisle name, independent of INPUT aisles. */
    public static boolean isOutput(String label){
        return label!=null&&"Output".equalsIgnoreCase(label.trim());
    }

    public static boolean isOutputAisle(Level level,RackBlockEntity rack){
        return rack!=null&&isOutput(RackNetwork.aisle(level,rack));
    }

    /**
     * Outbound four-Golem teams stage their complete pallet in a vacant,
     * ground-accessible OUTPUT aisle rack. Never choose a normal rack or
     * receiving PTP for a shipping job; hold cargo until space opens.
     * Pallets Plus can later collect only from these designated bays.
     */
    public static @Nullable RackBlockEntity firstAvailableOutputRack(
            ServerLevel level,List<RackBlockEntity> racks,@Nullable BlockPos exclude){
        for(RackBlockEntity rack:racks){
            if(rack.hasPallet()||!rack.isEmpty())continue;
            if(exclude!=null&&exclude.equals(rack.getBlockPos()))continue;
            if(!isOutputAisle(level,rack))continue;
            if(!WarehouseSortingReach.groundRack(level,rack))continue;
            RackPickupPlan plan=RackPickupPlan.find(level,rack);
            if(plan==null||plan.height()!=0)continue;
            return rack;
        }
        return null;
    }

    public static @Nullable RackBlockEntity firstAvailableOutputRack(
            ServerLevel level,List<RackBlockEntity> racks){
        return firstAvailableOutputRack(level,racks,null);
    }

    /** Transport-facing discovery: loaded OUTPUT rack bays of this warehouse.
     * Read-only; Pallets Plus must perform its own atomic pallet pickup.
     */
    public static List<RackBlockEntity> stagedOutputRacks(
            ServerLevel level,BlockPos computer){
        java.util.ArrayList<RackBlockEntity> ready=new java.util.ArrayList<>();
        for(RackBlockEntity rack:WarehouseNetwork.fromComputer(level,computer).racks())
            if(isOutputAisle(level,rack)&&rack.hasPallet())ready.add(rack);
        return List.copyOf(ready);
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
