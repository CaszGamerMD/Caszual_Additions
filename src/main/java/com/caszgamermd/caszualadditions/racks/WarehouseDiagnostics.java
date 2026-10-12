package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Read-only preflight diagnostics for real in-game warehouse testing.
 * Never forces chunk loads, changes inventories, reserves orders, or spawns
 * any worker. All counts reflect loaded, connected warehouse components.
 */
public final class WarehouseDiagnostics {
    private WarehouseDiagnostics() {}

    public static List<String> audit(ServerLevel level,BlockPos scanner) {
        List<String> results=new ArrayList<>();
        PtpBlockEntity point=StorageScanner.nearbyPoint(level,scanner);
        results.add("=== WAREHOUSE TEST AUDIT ===");
        if(point==null){
            results.add("[BLOCKED] Scanner is not touching an assembled 3x3 PTP.");
            results.add("Place nine transfer blocks, then put the scanner beside any cell.");
            return results;
        }
        StorageScanner.Report scan=StorageScanner.scan(level,scanner);
        results.add("PTP "+point.getBlockPos().toShortString()
                +": "+(point.input()?"INPUT":"OUTPUT")
                +" | pallet="+point.hasPallet()+" | sealed="+point.sealed()
                +" | wrap="+point.hasWrap()+" | hopper="+point.hasAdapter());
        if(scan!=null)results.add("Inventory: "+scan.items()+" items in "
                +scan.occupiedSlots()+" slots, "+scan.itemTypes()+" item types.");
        results.add("Output orders: "+point.pendingOutputOrders()+" queued, "
                +point.pendingOutputItems()+" items pending.");
        if(point.hauling())
            results.add("HAUL: "+point.haulPhase()+" / "
                    +point.haulWorkersAtSource()+"/4 rallied, "
                    +point.haulWorkersAtDestination()+"/4 arrived; to "
                    +point.haulDestination().toShortString());
        BlockPos terminal=StorageScanner.connectedComputer(level,scanner,point);
        if(terminal==null){
            results.add("[BLOCKED] No Rack Computer touching this PTP/scanner or linked shelves.");
            results.add("Connect shelves physically to a Rack Computer and the PTP.");
            return results;
        }
        List<RackBlockEntity> racks=RackNetwork.fromComputer(level,terminal);
        long stored=0;
        int empty=0,installed=0;
        for(RackBlockEntity rack:racks){
            if(rack.hasPallet())installed++;
            else if(rack.isEmpty())empty++;
            stored+=rack.totalItems();
        }
        results.add("Computer "+terminal.toShortString()+": "+racks.size()
                +" loaded racks; "+installed+" with pallets, "
                +empty+" empty for hauling; "+stored+" stored items.");
        OrderHubEntity hub=OrderHubEntity.nextToComputer(level,terminal);
        if(hub!=null)
            results.add("Legacy Order Collector Hub: "
                    +hub.collectors()+"/3 collectors, "+hub.orders()
                    +" queued orders, pallet="+hub.hasPallet()
                    +", output empty="+hub.isEmpty()+".");
        else results.add("Legacy Order Collector Hub: not connected (optional).");
        List<StorageGolemShelfEntity> shelves=RackComputerNet.nearbyShelves(level,terminal);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        long auto=shelves.stream().filter(StorageGolemShelfEntity::sortingEnabled).count();
        results.add("Shelves: "+shelves.size()+" linked / "+docked
                +" docked Golems / "+auto+" enabled shelves.");
        int boxes=StorageGolemDispatch.linkedEmptyShulkers(level,shelves);
        results.add("Shulker Tanks: "+boxes+" empty boxes across linked shelves.");
        if(hub!=null&&hub.collectors()==0)
            results.add("[LEGACY] Insert an Order Collector to activate the old hub.");
        if(racks.isEmpty())
            results.add("[BLOCKED] Computer must touch a pallet rack frame.");
        else if(installed==0)
            results.add("[SETUP] Install a pallet in a rack to test input/output jobs.");
        if(shelves.isEmpty())
            results.add("[BLOCKED] No linked Storage Golem Shelves at computer.");
        else if(auto==0)
            results.add("[PAUSED] Enable GOLEMS: AUTO in Rack Computer for sorting/orders.");
        if(docked==0)
            results.add("[BLOCKED] No docked Storage Golems; insert worker items into shelves.");
        if(!point.hasPallet())
            results.add("[BLOCKED] PTP needs an installed pallet for sorting/orders/hauling.");
        else if(point.sealed()&&!point.hasWrap())
            results.add("[BLOCKED] Sealed pallet has no Wrap Unit; cannot dispatch a haul.");
        else if(point.sealed()){
            if(docked<4)
                results.add("[HAUL] Four docked Golems required for a new haul (have "+docked+").");
            if(empty==0)
                results.add("[HAUL] Leave a connected rack without a pallet for delivery.");
            if(point.hauling())
                results.add("[HAUL] Sneak-click scanner to cancel the active four-worker job.");
            else if(point.hasWrap()&&docked>=4&&empty>0)
                results.add("[READY] Wrapped-pallet transport prerequisites present.");
        } else if(point.input()){
            results.add("[INPUT] Put stock on PTP, leave rack capacity and GOLEMS: AUTO on.");
        } else {
            results.add("[OUTPUT] Request an item from Rack Computer; output pallet stays unsealed.");
            if(boxes==0)results.add("[PACKING] Add empty shulkers to a tank touching linked shelves.");
        }
        if(scan!=null&&scan.loaded())
            results.add("Scanner estimate: "+scan.storable()+" fit / "
                    +scan.overflow()+" without room (snapshot, not reservation).");
        results.add("Use a backed-up world to test breaks, reloads, filters and cancellations.");
        return results;
    }
}
