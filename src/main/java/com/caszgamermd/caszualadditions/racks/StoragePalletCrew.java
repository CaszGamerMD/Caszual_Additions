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
                golem->golem.ownedBy(owner)
                        &&!golem.remotePickupActive()
                        &&!golem.remoteCourierActive());
    }
    private static MovingPallet moving(ServerLevel level,ServerPlayer player){
        List<MovingPallet> carriers=level.getEntitiesOfClass(MovingPallet.class,
                new AABB(player.blockPosition()).inflate(96),
                rig->rig.owns(player.getUUID())&&!rig.remoteMission());
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
        // The item link already authenticates the computer/dimension. The
        // distant home chunks need not be loaded just to record the order.
        return rig.returnToWarehouse(level,computer);
    }

    /** Direct mode must be summoned from a shelf that is REALLY attached to
     * the scanner's linked warehouse, never from a random nearby shelf. */
    public static String enlistFromShelf(ServerLevel level,ServerPlayer player,
            BlockPos computer,BlockPos selectedShelf){
        if(!level.hasChunkAt(computer)){
            WarehouseRemoteTickets.keep(level,computer);
            return "Linked warehouse computer is unloaded. Retry once its chunk is ready.";
        }
        return enlistAt(level,player,computer,selectedShelf);
    }
    public static String enlist(ServerLevel level,ServerPlayer player,BlockPos computer){
        return enlistAt(level,player,computer,null);
    }
    private static String enlistAt(ServerLevel level,ServerPlayer player,
            BlockPos computer,BlockPos selectedShelf){
        if(moving(level,player)!=null||!team(level,player).isEmpty())
            return "Four Golems already assigned. Deliver the carried pallet, then dismiss at the warehouse.";
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "No Rack Computer at that location.";
        List<StorageGolemShelfEntity> connected=RackComputerNet.nearbyShelves(level,computer);
        if(selectedShelf!=null){
            boolean valid=connected.stream().anyMatch(
                    shelf->shelf.getBlockPos().equals(selectedShelf));
            if(!valid)return "DIRECT: this Golem Shelf is not connected to "
                    +"the scanner's linked warehouse.";
            // Prefer selected shelf's workers, then its other linked shelves.
            connected.sort(java.util.Comparator.comparingInt(
                    shelf->shelf.getBlockPos().equals(selectedShelf)?0:1));
        }
        List<StorageGolemShelfEntity> available=new ArrayList<>();
        for(StorageGolemShelfEntity shelf:connected)
            for(int i=0;i<shelf.docked();i++)available.add(shelf);
        if(available.size()<4)return "Four docked Storage Golems required: "
                +available.size()+" currently available.";
        List<StorageGolem> spawned=new ArrayList<>();
        List<StorageGolemShelfEntity> withdrawn=new ArrayList<>();
        List<String> withdrawnNames=new ArrayList<>();
        for(int i=0;i<4;i++){
            StorageGolemShelfEntity shelf=available.get(i);
            BlockPos shelfDoor=StorageGolemShelfDoor.exit(level,shelf);
            if(shelfDoor==null){
                rollback(spawned,withdrawn,withdrawnNames);
                return "Shelf front opening is blocked; clear the aisle before dispatching the crew.";
            }
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null){
                rollback(spawned,withdrawn,withdrawnNames);
                return "Unable to create worker; no shelf stock consumed.";
            }
            String name=shelf.takeNamed();
            if(name==null){
                rollback(spawned,withdrawn,withdrawnNames);
                return "Unable to assign four workers; no shelf stock consumed.";
            }
            withdrawn.add(shelf);
            withdrawnNames.add(name);
            golem.assignPlayerCrew(shelf.getBlockPos(),i,player.getUUID());
            golem.setGolemName(name);
            StorageGolemShelfDoor.emerge(golem,shelfDoor,
                    StorageGolemShelfDoor.front(shelf));
            if(!level.addFreshEntity(golem)){
                rollback(spawned,withdrawn,withdrawnNames);
                return "Worker spawn failed; existing assignments rolled back.";
            }
            spawned.add(golem);
        }
        return "DIRECT: four Storage Golems called from linked shelves; "
                +"they are following you. Right-click a wrapped pallet on "
                +"a PTP, rack, or ground to pick it up. Then click an empty "
                +"rack or block face for them to place it.";
    }
    private static void rollback(List<StorageGolem> workers,
            List<StorageGolemShelfEntity> shelves,List<String> names){
        for(StorageGolem golem:workers)golem.discard();
        for(int i=0;i<shelves.size();i++)
            shelves.get(i).addNamed(names.get(i));
    }
    /** PTPs behave like racks: occupied = source; empty = destination
     * when holding cargo. Both actions use a protected exterior aisle. */
    public static String interactPtp(ServerLevel level,ServerPlayer player,
            PtpBlockEntity point){
        MovingPallet carrier=moving(level,player);
        if(carrier!=null)return carrier.requestPtpDelivery(level,point);
        List<StorageGolem> workers=team(level,player);
        if(workers.stream().anyMatch(w->w.escorting(player.getUUID())&&w.holdsPallet()))
            return deliverPtp(level,player,point);
        return pickup(level,player,point);
    }
    public static String deliverPtp(ServerLevel level,ServerPlayer player,
            PtpBlockEntity point){
        MovingPallet carrier=moving(level,player);
        if(carrier!=null)return carrier.requestPtpDelivery(level,point);
        List<StorageGolem> crew=team(level,player);
        StorageGolem lead=leader(crew,player.getUUID());
        if(lead==null)return "Wait until all four Golems have lifted the pallet.";
        if(!point.canInstallPallet())return "PTP occupied, reserved or blocked above; shipment retained.";
        PtpAccessPlan access=PtpAccessPlan.find(level,point,lead.blockPosition());
        if(access==null)return "PTP has no clear three-wide external unloading aisle.";
        if(!assembledNear(crew,access.service()))
            return "Wait for your four Golems to reach the PTP loading aisle.";
        if(!lead.placeIntoPtp(point))return "PTP rejected a pallet or cargo item; crew retains everything.";
        afterDelivery(crew);
        return "Pallet delivered to PTP "+point.getBlockPos().toShortString()
                +"; Golems are ready for another assignment.";
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
        PtpAccessPlan access=PtpAccessPlan.find(level,source,player.blockPosition());
        if(access==null)return "PTP pickup requires a clear three-wide aisle "
                +"outside its 3x3 floor, like the entrance to a rack.";
        int ticket=source.beginPlayerHaul(player.getUUID());
        if(ticket==0)return "PTP cannot reserve pallet; check pending orders or an existing haul.";
        for(StorageGolem worker:workers)
            worker.beginPlayerPickup(source.getBlockPos(),ticket,access.facing());
        return "Crew approaching PTP from its clear aisle to lift the wrapped pallet. "
                +"They will carry it as one rig and follow you.";
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
        if(!WarehouseSortingReach.groundRack(level,rack))
            return "Stacked rack loading requires a Golem-operated forklift. "
                    +"Stage the wrapped pallet on the aisle floor.";
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
    /**
     * DIRECT scanner: click ANY of the player's FOLLOWING Golems to send
     * their whole independently walking crew back to their own shelves.
     *
     * This is intentionally NOT the Rack Computer's warehouse-wide recall:
     * it only touches workers personally assigned to the clicking player,
     * never unrelated inventory sorters or another player's crew. No
     * proximity to a Rack Computer, shelf, or even a loaded home chunk is
     * needed to issue the instruction.
     */
    public static String recallFollowing(ServerLevel level,ServerPlayer player){
        java.util.ArrayList<StorageGolem> members=new java.util.ArrayList<>();
        for(var entity:level.getAllEntities()){
            if(entity instanceof StorageGolem golem
                    &&golem.ownedBy(player.getUUID()))
                members.add(golem);
        }
        if(members.isEmpty())
            return "DIRECT: no personally assigned Golems are following you.";

        // Validate the WHOLE crew first: recalling 3 of 4 while the
        // remaining leader holds 216 slots abandons the loaded pallet.
        if(members.stream().anyMatch(w->w.holdsPallet()
                ||w.escorting(player.getUUID())))
            return "DIRECT: crew is carrying a pallet. Place the pallet first, "
                    +"then right-click any follower to send the whole crew home.";

        int sent=0,alreadyReturning=0;
        for(StorageGolem worker:members){
            if(worker.recallToShelf(level))sent++;
            else alreadyReturning++;
        }
        return "DIRECT: "+sent+" Golem"+(sent==1?"":"s")
                +" sent to their original shelves"
                +(alreadyReturning>0?"; "+alreadyReturning
                    +" already returning or unavailable":"")+".";
    }

    /** Operator recall covers EVERY loaded worker whose home is a linked
     * warehouse shelf, regardless of its player owner or current task.
     * Rigs retain their 216 real cargo slots until they physically unload. */
    public static String dismissWarehouse(ServerLevel level,BlockPos computer){
        var network=WarehouseNetwork.fromComputer(level,computer);
        java.util.Set<BlockPos> homes=new java.util.HashSet<>();
        for(StorageGolemShelfEntity shelf:network.shelves()){
            homes.add(shelf.getBlockPos());
            // A recall is meant to leave the workers IN their shelves;
            // suspend automatic sorting or they would instantly redeploy.
            shelf.requestRecall();
        }
        if(homes.isEmpty())return "No linked Storage Golem shelves.";
        java.util.ArrayList<StorageGolem> workers=new java.util.ArrayList<>();
        java.util.ArrayList<MovingPallet> rigs=new java.util.ArrayList<>();
        for(var entity:level.getAllEntities()){
            if(entity instanceof StorageGolem worker&&homes.contains(worker.shelfHome()))
                workers.add(worker);
            if(entity instanceof MovingPallet carrier
                    &&carrier.fromWarehouse(network.shelves()))
                rigs.add(carrier);
        }
        // A leader currently owns a partially assembled manual pallet; do
        // not recall its escorts and leave the 216-slot pallet orphaned.
        java.util.Set<String> carryingOwners=new java.util.HashSet<>();
        for(StorageGolem worker:workers)
            if(worker.holdsPallet()&&!worker.assignedOwner().isBlank())
                carryingOwners.add(worker.assignedOwner());
        int recalled=0,protectedCargo=0;
        for(StorageGolem worker:workers){
            if(!worker.assignedOwner().isBlank()
                    &&carryingOwners.contains(worker.assignedOwner())){
                protectedCargo++;continue;
            }
            if(worker.recallToShelf(level))recalled++;
        }
        for(MovingPallet rig:rigs){
            if(rig.loaded()){
                rig.returnToWarehouse(level,computer);
                protectedCargo+=4;
            }
        }
        return "Warehouse recall: "+recalled+" workers returning to shelves; "
                +rigs.size()+" pallet team(s) routed home; "
                +protectedCargo+" workers protected until their cargo is unloaded. "
                +"Unloaded chunks resume when their workers load.";
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
        if(workers.isEmpty())return "No direct crew nearby. In DIRECT mode right-click a linked Golem Shelf to call four.";
        int cargo=0;
        for(StorageGolem worker:workers)if(worker.holdsPallet())cargo++;
        return "Crew nearby: "+workers.size()+"/4; carrying wrapped pallet: "
                +(cargo==1?"yes":cargo==0?"no":"INVALID multiple owners");
    }
}
