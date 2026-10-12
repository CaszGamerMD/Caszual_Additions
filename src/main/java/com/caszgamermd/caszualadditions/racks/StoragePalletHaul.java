package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Player-initiated, bounded pallet transportation using exactly four real
 * shelf workers and an escrowed wrapped source pallet. */
public final class StoragePalletHaul {
    private StoragePalletHaul() {}

    public static String request(ServerLevel level,BlockPos scanner) {
        PtpBlockEntity point=StorageScanner.nearbyPoint(level,scanner);
        if(point==null)return "No assembled transfer point beside this scanner.";
        if(point.hauling()){
            point.cancelHaul();
            return "Pallet haul cancelled. The wrapped pallet remains on the transfer point; workers are returning.";
        }
        if(!point.hasPallet())return "Install a pallet before requesting a haul.";
        if(!point.hasWrap()||!point.sealed())
            return "Install a Wrap Unit and seal the pallet before requesting four Golems.";
        BlockPos terminal=StorageScanner.connectedComputer(level,scanner,point);
        if(terminal==null)return "Connect the transfer point to a Rack Computer through a linked shelf.";
        List<StorageGolemShelfEntity> shelves=RackComputerNet.nearbyShelves(level,terminal);
        List<StorageGolemShelfEntity> available=new ArrayList<>();
        for(StorageGolemShelfEntity shelf:shelves)
            for(int i=0;i<shelf.docked();i++)available.add(shelf);
        if(available.size()<4)
            return "Four docked Storage Golems required; available: "+available.size()+".";
        // A rack with an existing pallet cannot accept an entire second
        // pallet. Use nearest empty rack for this first transport iteration.
        List<RackBlockEntity> candidates=new ArrayList<>(RackNetwork.fromComputer(level,terminal));
        candidates.removeIf(rack->rack.hasPallet()||!rack.isEmpty()
                ||rack.getBlockPos().distSqr(point.getBlockPos())>48*48
                ||Math.abs(rack.getBlockPos().getY()-point.getBlockPos().getY())>12);
        candidates.sort(Comparator.comparingDouble(rack->
                rack.getBlockPos().distSqr(point.getBlockPos())));
        if(candidates.isEmpty())
            return "No empty reachable rack within 48 blocks of the transfer point.";
        RackBlockEntity destination=candidates.getFirst();
        // Prepare all four entities first, before consuming shelf tokens.
        List<StorageGolem> workers=new ArrayList<>(4);
        for(int i=0;i<4;i++){
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(level,
                    net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if(worker==null)return "Could not initialize four Storage Golems.";
            workers.add(worker);
        }
        int ticket=point.beginHaul(destination.getBlockPos());
        if(ticket==0)
            return "Unable to reserve this wrapped pallet (pending orders or another shipment).";
        List<StorageGolemShelfEntity> consumed=new ArrayList<>(4);
        List<StorageGolem> spawned=new ArrayList<>(4);
        for(int i=0;i<4;i++){
            StorageGolemShelfEntity shelf=available.get(i);
            StorageGolem worker=workers.get(i);
            if(!shelf.take()){
                rollback(point,consumed,spawned);
                return "A docked worker became unavailable. Haul not started.";
            }
            consumed.add(shelf);
            worker.assignHaul(shelf.getBlockPos(),point.getBlockPos(),
                    destination.getBlockPos(),ticket,i);
            worker.setPos(shelf.getBlockPos().getX()+.5,
                    shelf.getBlockPos().getY()+1,shelf.getBlockPos().getZ()+.5);
            if(!level.addFreshEntity(worker)){
                rollback(point,consumed,spawned);
                return "Unable to deploy all four Golems. Haul cancelled without moving items.";
            }
            spawned.add(worker);
        }
        return "FOUR-GOLEM HAUL DISPATCHED: "+RackNetwork.label(level,destination)
                +". The sealed source retains the full pallet until all four arrive.";
    }
    private static void rollback(PtpBlockEntity point,
            List<StorageGolemShelfEntity> consumed,List<StorageGolem> spawned) {
        // Spawn cancellation is synchronous and occurs before any worker
        // has ticked or moved cargo. Return each consumed worker token once.
        point.cancelHaul();
        for(StorageGolem worker:spawned)worker.discard();
        for(StorageGolemShelfEntity shelf:consumed)shelf.add();
    }
}
