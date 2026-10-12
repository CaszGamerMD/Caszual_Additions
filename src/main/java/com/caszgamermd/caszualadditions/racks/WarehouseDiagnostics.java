package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Loaded-only, read-only audit invoked by sneak-using the handheld scanner
 * on the Rack Computer. Never moves stock or forces chunk loading. */
public final class WarehouseDiagnostics {
    private WarehouseDiagnostics(){}

    public static List<String> audit(ServerLevel level,BlockPos computer){
        List<String> results=new ArrayList<>();
        results.add("=== WAREHOUSE DIAGNOSTICS ===");
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER)){
            results.add("[BLOCKED] Target a Rack Computer with the handheld scanner.");
            return results;
        }
        List<RackBlockEntity> racks=RackNetwork.fromComputer(level,computer);
        int readyRacks=0,emptyRacks=0;
        long items=0;
        for(RackBlockEntity rack:racks){
            if(rack.hasPallet())readyRacks++;
            if(!rack.hasPallet()&&rack.isEmpty())emptyRacks++;
            items+=rack.totalItems();
        }
        results.add("Racks: "+racks.size()+" linked / "+readyRacks
                +" with pallets / "+emptyRacks+" empty / "+items+" stored items.");
        List<StorageGolemShelfEntity> shelves=RackComputerNet.nearbyShelves(level,computer);
        int docked=shelves.stream().mapToInt(StorageGolemShelfEntity::docked).sum();
        results.add("Shelves: "+shelves.size()+" linked / "+docked
                +" docked real Golems. Four are required for a carrying crew.");
        int boxes=StorageGolemDispatch.linkedEmptyShulkers(level,shelves);
        results.add("Shelf Shulker Tanks: "+boxes+" empty boxes.");
        List<PtpBlockEntity> points=StoragePtpNetwork.attached(level,computer,shelves);
        results.add("Transfer Points: "+points.size()+" attached.");
        for(PtpBlockEntity ptp:points){
            int stacks=0,itemsOnPoint=0;
            for(int i=0;i<ptp.getContainerSize();i++){
                var item=ptp.displayItem(i);
                if(!item.isEmpty()){stacks++;itemsOnPoint+=item.getCount();}
            }
            results.add("PTP "+ptp.getBlockPos().toShortString()
                    +": "+(ptp.input()?"IN":"OUT")
                    +" pallet="+ptp.hasPallet()
                    +" sealed="+ptp.sealed()
                    +" glass wrap="+ptp.hasWrap()
                    +" items="+itemsOnPoint+" in "+stacks+" slots"
                    +" orders="+ptp.pendingOutputOrders());
            if(ptp.hauling())
                results.add("  Four-Golem pickup: "+ptp.haulWorkersAtSource()
                        +"/4 at source.");
        }
        if(racks.isEmpty())results.add("[BLOCKED] Attach computer to a rack.");
        if(shelves.isEmpty())results.add("[BLOCKED] Attach Storage Golem Shelves.");
        if(docked<4)results.add("[CREW] Need 4 free docked Golems to enlist.");
        if(points.isEmpty())results.add("[ORDER] Attach an OUTPUT PTP to computer or shelves.");
        results.add("Right-click computer to enlist/return crew; right-click sealed "
                +"PTP pallet to lift; rack or ground to deliver.");
        return results;
    }
}
