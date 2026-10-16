package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

/** Server-authoritative player-led moving crew. All four real Golems exist
 * in the world; the one designated leader owns the entire sealed pallet
 * payload during transport. Neither a scanner nor a player inventory stores
 * a second copy of the 216 slots. */
public final class StoragePalletCrew {
    private StoragePalletCrew(){}

    private static List<StorageGolem> team(ServerLevel level,ServerPlayer player){
        UUID owner=player.getUUID();
        return level.getEntitiesOfClass(StorageGolem.class,
                new AABB(player.blockPosition()).inflate(64),
                golem->golem.ownedBy(owner));
    }
    private static MovingPallet moving(ServerLevel level,ServerPlayer player){
        List<MovingPallet> carriers=level.getEntitiesOfClass(MovingPallet.class,
                new AABB(player.blockPosition()).inflate(96),
                rig->rig.owns(player.getUUID()));
        return carriers.isEmpty()?null:carriers.getFirst();
    }
    /** Converts four physically assembled workers into one navigable body.
     * Rollback: if spawning is rejected, the lead golem still owns cargo. */
    public static boolean formMovingPallet(ServerLevel level,
            List<StorageGolem> crew,UUID owner){
        if(crew.size()!=4||owner==null)return false;
        StorageGolem leader=null;
        int mask=0;
        for(StorageGolem worker:crew){
            if(!worker.ownedBy(owner))return false;
            mask=WarehouseTeamProgress.arrive(mask,worker.crewIndex());
            if(worker.crewIndex()==0)leader=worker;
        }
        if(!WarehouseTeamProgress.complete(mask)
                ||leader==null||!leader.holdsPallet())return false;
        MovingPallet carrier=RackContent.MOVING_PALLET_TYPE.create(
                level,EntitySpawnReason.EVENT);
        if(carrier==null||!carrier.prepare(owner,crew,leader.palletForTransfer(),
                leader.palletContentsForTransfer()))return false;
        double x=0,y=0,z=0;
        for(StorageGolem member:crew){x+=member.getX();y+=member.getY();z+=member.getZ();}
        carrier.setPos(x/4,y/4,z/4);
        if(!level.addFreshEntity(carrier))return false;
        // The single entity has accepted the full saved inventory; all
        // physical workers are now model parts and no longer tick separately.
        leader.relinquishPalletToRig();
        for(StorageGolem member:crew)member.discard();
        return true;
    }

    private static boolean completeTeam(List<StorageGolem> team){
        if(team.size()!=4)return false;
        int mask=0;
        for(StorageGolem member:team)mask=WarehouseTeamProgress.arrive(mask,member.crewIndex());
        return WarehouseTeamProgress.complete(mask);
    }
    /** Permit a freshly acquired handheld scanner to bind an existing
     * crew dispatched from the Golem workstation page, without stealing
     * workers from a different warehouse or dismissing them. */
    public static boolean assignedToComputer(ServerLevel level,ServerPlayer player,
            BlockPos computer){
        MovingPallet rig=moving(level,player);
        if(rig!=null)return rig.fromWarehouse(
                WarehouseConnections.shelvesForComputer(level,computer));
        List<StorageGolem> assigned=team(level,player);
        if(assigned.isEmpty())return false;
        java.util.Set<BlockPos> shelves=new java.util.HashSet<>();
        for(StorageGolemShelfEntity shelf:WarehouseConnections.shelvesForComputer(level,computer))
            shelves.add(shelf.getBlockPos());
        for(StorageGolem worker:assigned)
            if(!shelves.contains(worker.shelfHome()))return false;
        return true;
    }

    /** Shift-use the linked scanner in the air to send its current loaded
     * crew and their wrapped cargo back to the assigned warehouse. */
    public static String returnLoadedCrew(ServerLevel level,ServerPlayer player,
            BlockPos computer){
        MovingPallet rig=moving(level,player);
        if(rig==null)return "No loaded pallet crew to send home.";
        if(!rig.fromWarehouse(WarehouseConnections.shelvesForComputer(level,computer)))
            return "This crew belongs to a different warehouse.";
        return rig.returnToWarehouse(level,computer);
    }

    public static String enlist(ServerLevel level,ServerPlayer player,BlockPos computer){
        if(moving(level,player)!=null||!team(level,player).isEmpty())
            return "Four Golems already assigned. Deliver the carried pallet, then dismiss at the warehouse.";
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "No Rack Computer at that location.";
        List<StorageGolemShelfEntity> connected=RackComputerNet.nearbyShelves(level,computer);
        List<StorageGolemShelfEntity> available=new ArrayList<>();
        for(StorageGolemShelfEntity shelf:connected)
            for(int i=0;i<shelf.docked();i++)available.add(shelf);
        if(available.size()<4)return "Four docked Storage Golems required: "
                +available.size()+" currently available.";
        List<StorageGolem> spawned=new ArrayList<>();
        List<StorageGolemShelfEntity> withdrawn=new ArrayList<>();
        for(int i=0;i<4;i++){
            StorageGolemShelfEntity shelf=available.get(i);
            BlockPos shelfDoor=StorageGolemShelfDoor.exit(level,shelf);
            if(shelfDoor==null){
                rollback(spawned,withdrawn);
                return "Shelf front opening is blocked; clear the aisle before dispatching the crew.";
            }
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null||!shelf.take()){
                rollback(spawned,withdrawn);
                return "Unable to assign four workers; no shelf stock consumed.";
            }
            withdrawn.add(shelf);
            golem.assignPlayerCrew(shelf.getBlockPos(),i,player.getUUID());
            StorageGolemShelfDoor.emerge(golem,shelfDoor,
                    StorageGolemShelfDoor.front(shelf));
            if(!level.addFreshEntity(golem)){
                rollback(spawned,withdrawn);
                return "Worker spawn failed; existing assignments rolled back.";
            }
            spawned.add(golem);
        }
        return "Warehouse linked: four Storage Golems are following you. "
                +"Right-click a sealed pallet at a PTP to enlist them for pickup.";
    }
    private static void rollback(List<StorageGolem> workers,
            List<StorageGolemShelfEntity> shelves){
        for(StorageGolem golem:workers)golem.discard();
        for(StorageGolemShelfEntity shelf:shelves)shelf.add();
    }
    public static String pickup(ServerLevel level,ServerPlayer player,PtpBlockEntity source){
        if(moving(level,player)!=null)return "Your four Golems are already carrying a pallet.";
        List<StorageGolem> workers=team(level,player);
        if(!completeTeam(workers))return "All four assigned Golems must be nearby to pick up a pallet.";
        if(workers.stream().anyMatch(w->w.holdsPallet()
                ||w.escorting(player.getUUID())||w.pickupPending(player.getUUID())))
            return "This crew already has an active pallet pickup or delivery.";
        if(!source.hasPallet()||!source.sealed())
            return "Pallet must be plastic-wrapped before lifting (portable wrapper or Wrap Unit).";
        int ticket=source.beginPlayerHaul(player.getUUID());
        if(ticket==0)return "PTP cannot reserve pallet; check pending orders or an existing haul.";
        for(StorageGolem worker:workers)worker.beginPlayerPickup(source.getBlockPos(),ticket);
        return "Crew lifting sealed pallet. Wait for all four to reach its corners; "
                +"then they will carry it as they follow you.";
    }
    /** The warehouse Scanner can dispatch its FOUR real workers to any
     * wrapped 2x2 pallet on the ground, regardless of warehouse membership. */
    public static String pickupGround(ServerLevel level,ServerPlayer player,BlockPos clicked){
        if(moving(level,player)!=null)return "Your four Golems are already carrying a pallet.";
        List<StorageGolem> workers=team(level,player);
        if(!completeTeam(workers))
            return "Four assigned Golems must be nearby. Link a warehouse Scanner first.";
        if(workers.stream().anyMatch(w->w.holdsPallet()
                ||w.escorting(player.getUUID())||w.pickupPending(player.getUUID())))
            return "Finish or cancel the crew's current pallet job first.";
        var pallet=StorageGroundPalletTransfer.wrappedController(level,clicked);
        if(pallet==null)return "Pallet must be intact and glass-wrapped before Golems can lift it.";
        BlockPos root=pallet.getBlockPos();
        if(!assembledNear(workers,root))
            return "Wait for all four Golems to catch up before requesting pickup.";
        for(StorageGolem golem:workers)golem.beginGroundPickup(root);
        return "Crew moving to the four sides of the wrapped ground pallet. "
                +"They will lift its 216-slot inventory together and follow you.";
    }

    /**
     * A rack is both a source and a destination: selecting one with the
     * warehouse scanner retrieves its pallet when the crew is empty, or
     * delivers the carried pallet when the crew is loaded.
     */
    public static String interactRack(ServerLevel level,ServerPlayer player,RackBlockEntity rack){
        List<StorageGolem> workers=team(level,player);
        if(moving(level,player)!=null
                ||workers.stream().anyMatch(w->w.escorting(player.getUUID())&&w.holdsPallet()))
            return deliverRack(level,player,rack);
        return pickupRack(level,player,rack);
    }

    public static String pickupRack(ServerLevel level,ServerPlayer player,RackBlockEntity rack){
        if(moving(level,player)!=null)return "Your four Golems are already carrying a pallet.";
        List<StorageGolem> workers=team(level,player);
        if(!completeTeam(workers))
            return "Four assigned Golems must be nearby. Link a warehouse Scanner first.";
        if(workers.stream().anyMatch(w->w.holdsPallet()
                ||w.escorting(player.getUUID())||w.pickupPending(player.getUUID())))
            return "Finish the crew's current pallet pickup or delivery first.";
        if(!rack.hasPallet())return "This rack has no pallet to collect.";
        if(!rack.glassWrapped())
            return "Apply plastic stretch wrap before lifting this rack's pallet.";
        RackPickupPlan plan=RackPickupPlan.find(level,rack);
        if(plan==null)
            return "Rack pickup needs a clear 2-block-wide path within the 3-block aisle. "
                    +"Elevated racks also need an unobstructed lowering shaft "
                    +"(up to "+RackPickupPlan.MAX_DROP+" blocks down).";
        if(!assembledNear(workers,plan.rigCenter()))
            return "Wait for all four Golems to reach the aisle below this rack.";
        for(StorageGolem worker:workers)
            worker.beginRackPickup(rack.getBlockPos(),plan.floorY());
        return "Two Golems will pull the plastic-wrapped pallet out of "
                +RackNetwork.label(level,rack)
                +(plan.height()>0?" and lower it "+plan.height()+" blocks":"")
                +"; the other two will then join the lift.";
    }

    private static boolean assembledNear(List<StorageGolem> members,
            BlockPos destination){
        for(StorageGolem golem:members)
            if(golem.distanceToSqr(destination.getX()+.5,
                    destination.getY()+.5,destination.getZ()+.5)>16*16)
                return false;
        return true;
    }
    private static StorageGolem leader(List<StorageGolem> team,UUID owner){
        if(!completeTeam(team))return null;
        return team.stream().filter(w->w.crewIndex()==0&&w.ownedBy(owner)
                &&w.escorting(owner)&&w.holdsPallet()).findFirst().orElse(null);
    }
    private static void afterDelivery(List<StorageGolem> members){
        for(StorageGolem member:members)member.clearDelivery();
    }
    public static String deliverRack(ServerLevel level,ServerPlayer player,RackBlockEntity rack){
        MovingPallet rig=moving(level,player);
        if(rig!=null)return rig.requestRackDelivery(level,rack);
        List<StorageGolem> workers=team(level,player);
        StorageGolem lead=leader(workers,player.getUUID());
        if(lead==null)return "Wait until all four Golems finish lifting and follow you.";
        if(!assembledNear(workers,rack.getBlockPos()))
            return "Wait for all four Golems to catch up with the destination rack.";
        if(!rack.hasPallet()&&rack.isEmpty()&&lead.placeIntoRack(rack)){
            afterDelivery(workers);
            return "Pallet placed in rack "+RackNetwork.label(level,rack)
                    +". Return to your warehouse with the empty crew to dismiss them.";
        }
        return "Rack must be empty and have no installed pallet; carried cargo is unchanged.";
    }
    public static String deliverGround(ServerLevel level,ServerPlayer player,BlockPos location){
        MovingPallet rig=moving(level,player);
        if(rig!=null)return rig.requestGroundDelivery(
                level,location,player.getDirection());
        List<StorageGolem> workers=team(level,player);
        StorageGolem lead=leader(workers,player.getUUID());
        if(lead==null)return "Wait for the four Golems to complete pickup before placing.";
        if(!assembledNear(workers,location))
            return "Wait for all four Golems to catch up before placing the pallet.";
        if(lead.placeOnGround(level,location,player.getDirection())){
            afterDelivery(workers);
            return "Wrapped shipment delivered to ground at "
                    +location.toShortString()+". Return to warehouse to dismiss crew.";
        }
        return "Need a clear loaded 2x2 space for a pallet; crew retains its cargo.";
    }
    public static String dismiss(ServerLevel level,ServerPlayer player,BlockPos computer){
        if(player.distanceToSqr(computer.getX()+.5,computer.getY()+.5,
                computer.getZ()+.5)>64)
            return "Return to your linked Rack Computer to dismiss the crew.";
        if(moving(level,player)!=null)
            return "Deliver the raised pallet first; the four Golems cannot detach in transit.";
        List<StorageGolem> members=team(level,player);
        if(members.isEmpty())return "No assigned Golems are near the warehouse.";
        if(members.stream().anyMatch(StorageGolem::holdsPallet))
            return "Deliver or safely recover the pallet before dismissing its carrier.";
        // A worker may have died/recalled. Dismiss surviving empty escorts
        // without leaving them permanently assigned to the player.
        for(StorageGolem golem:members){
            golem.cancelPendingPickup(level);
            golem.clearDelivery();
            golem.dismissCrew();
        }
        return members.size()+" Golems dismissed to original shelves. "
                +(members.size()<4?"Any lost worker items must be manually recovered.":"");
    }
    public static String status(ServerLevel level,ServerPlayer player){
        MovingPallet rig=moving(level,player);
        if(rig!=null)return rig.isDelivering()
                ?"Crew heading to assigned pallet placement. They will turn inward and "
                    +"release only AFTER arriving."
                :"Four Golems carry one shared pallet near the player. "
                    +(rig.deliveryStatus().isBlank()
                        ?"Choose a delivery location with the Warehouse Scanner."
                        :rig.deliveryStatus());
        List<StorageGolem> workers=team(level,player);
        if(workers.isEmpty())return "No linked crew nearby. Right-click a Rack Computer to enlist four.";
        int cargo=0;
        for(StorageGolem worker:workers)if(worker.holdsPallet())cargo++;
        return "Crew nearby: "+workers.size()+"/4; carrying wrapped pallet: "
                +(cargo==1?"yes":cargo==0?"no":"INVALID multiple owners");
    }
}
