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
    private static boolean completeTeam(List<StorageGolem> team){
        if(team.size()!=4)return false;
        int mask=0;
        for(StorageGolem member:team)mask=WarehouseTeamProgress.arrive(mask,member.crewIndex());
        return WarehouseTeamProgress.complete(mask);
    }
    public static String enlist(ServerLevel level,ServerPlayer player,BlockPos computer){
        if(!team(level,player).isEmpty())
            return "Your four-Golem team is already assigned. Return it before linking another warehouse.";
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
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null||!shelf.take()){
                rollback(spawned,withdrawn);
                return "Unable to assign four workers; no shelf stock consumed.";
            }
            withdrawn.add(shelf);
            golem.assignPlayerCrew(shelf.getBlockPos(),i,player.getUUID());
            golem.setPos(shelf.getBlockPos().getX()+.5,
                    shelf.getBlockPos().getY()+1,shelf.getBlockPos().getZ()+.5);
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
        List<StorageGolem> workers=team(level,player);
        if(!completeTeam(workers))return "All four assigned Golems must be nearby to pick up a pallet.";
        if(workers.stream().anyMatch(w->w.holdsPallet()||w.escorting(player.getUUID())))
            return "This crew is already transporting a pallet.";
        if(!source.hasPallet()||!source.sealed()||!source.hasWrap())
            return "Pallet must be sealed with a Wrap Unit before lifting.";
        int ticket=source.beginPlayerHaul(player.getUUID());
        if(ticket==0)return "PTP cannot reserve pallet; check pending orders or an existing haul.";
        for(StorageGolem worker:workers)worker.beginPlayerPickup(source.getBlockPos(),ticket);
        return "Crew lifting sealed pallet. Wait for all four to reach its corners; "
                +"then they will carry it as they follow you.";
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
        List<StorageGolem> workers=team(level,player);
        StorageGolem lead=leader(workers,player.getUUID());
        if(lead==null)return "Wait until all four Golems finish lifting and follow you.";
        if(!rack.hasPallet()&&rack.isEmpty()&&lead.placeIntoRack(rack)){
            afterDelivery(workers);
            return "Pallet placed in rack "+RackNetwork.label(level,rack)
                    +". Return to your warehouse with the empty crew to dismiss them.";
        }
        return "Rack must be empty and have no installed pallet; carried cargo is unchanged.";
    }
    public static String deliverGround(ServerLevel level,ServerPlayer player,BlockPos location){
        List<StorageGolem> workers=team(level,player);
        StorageGolem lead=leader(workers,player.getUUID());
        if(lead==null)return "Wait for the four Golems to complete pickup before placing.";
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
        List<StorageGolem> members=team(level,player);
        if(!completeTeam(members))return "Not all four Golems are here. Wait for the crew.";
        if(members.stream().anyMatch(w->w.holdsPallet()||w.escorting(player.getUUID())))
            return "Deliver the wrapped pallet before dismissing the Golems.";
        for(StorageGolem golem:members)golem.dismissCrew();
        return "All four Golems dismissed; they are returning to their original shelves.";
    }
    public static String status(ServerLevel level,ServerPlayer player){
        List<StorageGolem> workers=team(level,player);
        if(workers.isEmpty())return "No linked crew nearby. Right-click a Rack Computer to enlist four.";
        int cargo=0;
        for(StorageGolem worker:workers)if(worker.holdsPallet())cargo++;
        return "Crew nearby: "+workers.size()+"/4; carrying wrapped pallet: "
                +(cargo==1?"yes":cargo==0?"no":"INVALID multiple owners");
    }
}
