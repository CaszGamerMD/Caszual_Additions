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
    public static void actionBar(Player player,Component message){
        if(player instanceof net.minecraft.server.level.ServerPlayer user)
            user.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(message));
        else player.sendSystemMessage(message);
    }
    public static void message(Player player,BlockPos home,Component message){
        boolean verbose=player.level() instanceof ServerLevel world&&chat(world,home);
        if(verbose)player.sendSystemMessage(message);
        else actionBar(player,message);
    }
}
