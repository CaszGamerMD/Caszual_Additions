package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Nonintrusive warehouse feedback, with verbose chat opt-in only. */
public final class WarehouseGolemDebug {
    private WarehouseGolemDebug(){}
    public static boolean chat(ServerLevel world,BlockPos home){
        return world.hasChunkAt(home)
                &&world.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                &&shelf.debugChat();
    }
    public static void message(Player player,BlockPos home,Component message){
        boolean verbose=player.level() instanceof ServerLevel world&&chat(world,home);
        // true = transient action bar, false = permanent chat log.
        player.displayClientMessage(message,!verbose);
    }
}
