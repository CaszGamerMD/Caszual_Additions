package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * An explicit, two-click player recovery of a stuck remote pallet exchange.
 *
 * Exactly four warehouse shelf identities are outstanding from dispatch:
 * before lift 3 independent Golems + 1 replacement courier;
 * after lift 1 loaded three-worker rig + 1 replacement courier.
 * Never spawn or credit a fifth Golem, create a second replacement deck,
 * or discard the original pallet's 216 stacks.
 */
public final class RemotePalletReset {
    private RemotePalletReset(){}
    private static final int CONFIRM_TICKS=200;
    private record Confirmation(UUID mission,long expires){}
    private static final Map<UUID,Confirmation> confirmations=new HashMap<>();

    /**
     * Scanner rescue without having to hit the tiny moving entity. The
     * server selects an active REMOTE mission exclusively by the player's
     * saved ownership and rig UUID; proximity or a nearby unrelated
     * pallet can never change the mission being cancelled.
     *
     * Call from sneak-use AIR or a normal ground block. Keep this separate
     * from DIRECT recall and from merely toggling scanner mode.
     */
    public static UUID activeMission(ServerLevel level,ServerPlayer player){
        UUID found=null;
        for(var entity:level.getAllEntities()){
            UUID mission=null;
            if(entity instanceof MovingPallet rig
                    &&rig.remoteExchangeActive()&&rig.owns(player.getUUID()))
                mission=rig.getUUID();
            else if(entity instanceof StorageGolem worker
                    &&worker.ownedBy(player.getUUID())){
                if(worker.remoteCourierActive())
                    mission=worker.remoteCourierMission();
                else if(worker.remotePickupActive())
                    mission=worker.remotePickupMission();
            }
            if(mission==null)continue;
            // A normal mission has up to five world entities but only ONE
            // rig UUID. Do not arbitrarily select a job if the save is
            // corrupted or two remote exchanges are active.
            if(found!=null&&!found.equals(mission))return null;
            found=mission;
        }
        return found;
    }

    public static String requestActive(ServerLevel level,ServerPlayer player){
        UUID mission=activeMission(level,player);
        if(mission==null)
            return "REMOTE RESET: no unique loaded remote pickup found. "
                    +"Load the courier and three-worker carrier; if multiple "
                    +"missions are present, shift-scan the specific Golem.";
        return request(level,player,mission);
    }

    public static String request(ServerLevel level,ServerPlayer player,UUID mission){
        if(mission==null)return "REMOTE RESET: this Golem has no assigned mission.";
        var current=confirmations.get(player.getUUID());
        if(current==null||!current.mission().equals(mission)
                ||current.expires()<level.getGameTime()){
            confirmations.put(player.getUUID(),new Confirmation(mission,
                    level.getGameTime()+CONFIRM_TICKS));
            return "REMOTE RESET ARMED: shift-right-click this same mission "
                    +"with your Warehouse Scanner again within 10 seconds. "
                    +"The recovered pallet is restored at its original location, "
                    +"the empty replacement is refunded, and four Golems are returned.";
        }
        confirmations.remove(player.getUUID());
        return cancel(level,player,mission);
    }

    private static String cancel(ServerLevel level,ServerPlayer player,UUID mission){
        MovingPallet rig=level.getEntity(mission) instanceof MovingPallet carrier
                ?carrier:null;
        ArrayList<StorageGolem> trio=new ArrayList<>();
        UUID courierId;
        BlockPos[] homes;
        BlockPos target;

        if(rig!=null){
            if(!rig.remoteExchangeActive()||!rig.owns(player.getUUID()))
                return "REMOTE RESET DENIED: this loaded carrier is not your active remote mission.";
            courierId=rig.remoteCourierId();
            homes=rig.remoteShelfHomes();
            // For a live rig, the original pallet must already have been
            // lifted. No cargo-less placeholder rig is a valid rollback.
            if(!rig.remoteLoadedForReset())
                return "REMOTE RESET WAITING: the replacement is already placed "
                        +"or the original pallet has not finished lifting. No items changed.";
            target=rig.remoteOriginalTarget();
        }else{
            // During outbound walk NO rig exists yet. All three distinct
            // physical workers must be present in loaded chunks before
            // consuming their identities. Missing workers require waiting,
            // never creating new Golems to make up a lost entity.
            for(var entity:level.getAllEntities()){
                if(entity instanceof StorageGolem golem
                        &&golem.remotePickupActive()
                        &&mission.equals(golem.remotePickupMission())){
                    trio.add(golem);
                }
            }
            if(trio.size()!=3)return "REMOTE RESET WAITING: load all THREE "
                    +"independent pickup workers first ("+trio.size()+"/3 found).";
            boolean[] seen=new boolean[3];
            for(StorageGolem worker:trio){
                int index=worker.remotePickupIndex();
                if(index<0||index>=3||seen[index]
                        ||!player.getUUID().equals(worker.remotePickupOwner()))
                    return "REMOTE RESET DENIED: crew identity mismatch; nothing changed.";
                seen[index]=true;
            }
            StorageGolem worker=trio.get(0);
            courierId=worker.remotePickupCourier();
            homes=worker.remotePickupHomes();
            target=worker.remotePickupTarget();
            for(StorageGolem other:trio)
                if(!courierId.equals(other.remotePickupCourier())
                        ||!target.equals(other.remotePickupTarget()))
                    return "REMOTE RESET DENIED: worker mission mismatch; nothing changed.";
        }
        if(courierId==null||homes.length!=4)
            return "REMOTE RESET WAITING: lost fourth-worker reference.";
        if(!(level.getEntity(courierId) instanceof StorageGolem courier)
                ||!courier.remoteCourierActive()
                ||!mission.equals(courier.remoteCourierMission())
                ||!courier.ownedBy(player.getUUID()))
            return "REMOTE RESET WAITING: the replacement courier must be "
                    +"loaded and present. Neither pallet nor Golems were changed.";
        if(!courier.remoteReplacementAvailableForReset())
            return "REMOTE RESET WAITING: replacement already handed off "
                    +"or missing from courier. Nothing was deleted.";

        // COMMIT: all four worker identities and the real replacement item
        // are present. Restore all 216 stored stacks to the ORIGINAL 2x2
        // floor pallet before removing the rig. If a different pallet or
        // world block is in that footprint, abort without ANY destructive
        // changes and ask the player to clear the original site.
        if(rig!=null&&!rig.restoreRemoteOriginal(level))
            return "REMOTE RESET BLOCKED: clear the ORIGINAL 2x2 pallet "
                    +"footprint at "+target.toShortString()+" and retry. "
                    +"The loaded rig keeps its pallet and all 216 inventory slots.";

        ItemStack replacement=courier.takeRemoteReplacementForReset();
        if(replacement.isEmpty())
            return "REMOTE RESET WAITING: replacement courier changed during recovery.";

        // The player's carried fresh replacement is the ONE withdrawn
        // warehouse pallet, not a newly generated replacement. Full
        // inventories are handled by Minecraft's inventory return logic.
        player.getInventory().placeItemBackInInventory(replacement);

        // Return the four original, consumed shelf identities exactly once.
        // If a shelf is full/missing/unloaded, give that worker as an item
        // rather than losing or silently duplicating its docked count.
        int docked=0,returned=0;
        for(BlockPos home:homes){
            if(level.hasChunkAt(home)
                    &&level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                    &&shelf.add()){
                docked++;
            }else{
                player.getInventory().placeItemBackInInventory(
                        new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
                returned++;
            }
        }
        if(rig!=null)rig.discard();
        for(StorageGolem worker:trio)worker.discard();
        courier.discard();
        StorageScannerItem.cancelQueuedForTarget(player,target);
        return "REMOTE RESET COMPLETE: "
                +(rig==null?"original target pallet left in place"
                        :"original pallet and 216 stored slots restored at "
                            +target.toShortString())
                +"; empty replacement returned; "+docked
                +" Golems redocked and "+returned+" returned as items."
                +" You may dispatch another remote pickup.";
    }
}
