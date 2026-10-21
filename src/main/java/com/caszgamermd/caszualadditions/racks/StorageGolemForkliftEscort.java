package com.caszgamermd.caszualadditions.racks;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/** Forklift-mode Scanner summon: one actual Golem leaves a connected shelf,
 * follows the player, then boards the vehicle the user chooses. */
public final class StorageGolemForkliftEscort {
    private StorageGolemForkliftEscort(){}
    public static String summon(ServerLevel level,ServerPlayer player,BlockPos computer,
            BlockPos chosenShelf){
        if(level.getBlockEntity(chosenShelf)
                instanceof StorageGolemShelfEntity shelf
                &&WarehouseNetwork.fromComputer(level,computer).shelves().stream()
                        .anyMatch(s->s.getBlockPos().equals(chosenShelf))){
            for(StorageGolem golem:level.getEntitiesOfClass(StorageGolem.class,
                    new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(64),
                    g->g.escortFor(player.getUUID())))
                return "FORKLIFT: an operator is already following you.";
            if(shelf.docked()<1)return "FORKLIFT: no Golem docked in selected shelf.";
            BlockPos exit=StorageGolemShelfDoor.exit(level,shelf);
            if(exit==null)return "FORKLIFT: clear the shelf's front exit first.";
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(
                    level,EntitySpawnReason.EVENT);
            if(worker==null)return "FORKLIFT: unable to create operator.";
            String name=shelf.takeNamed();
            if(name==null)return "FORKLIFT: no available operator.";
            worker.setGolemName(name);
            worker.assignForkliftEscort(chosenShelf,player.getUUID());
            StorageGolemShelfDoor.emerge(worker,exit,StorageGolemShelfDoor.front(shelf));
            if(!level.addFreshEntity(worker)){
                shelf.addNamed(name);
                return "FORKLIFT: shelf exit unavailable; no Golem consumed.";
            }
            return "FORKLIFT: Golem "+name+" following. Right-click a forklift "
                    +"with the Scanner to assign a driver.";
        }
        return "FORKLIFT: select a Golem Shelf connected to this warehouse.";
    }
    public static StorageGolem escort(ServerLevel level,UUID owner,BlockPos near){
        StorageGolem closest=null;
        double best=64*64;
        for(StorageGolem golem:level.getEntitiesOfClass(StorageGolem.class,
                new net.minecraft.world.phys.AABB(near).inflate(64),
                g->g.escortFor(owner))){
            double d=golem.blockPosition().distSqr(near);
            if(d<best){best=d;closest=golem;}
        }
        return closest;
    }
}
