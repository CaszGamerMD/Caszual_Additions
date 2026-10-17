package com.caszgamermd.caszualadditions.racks;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Exactly ONE globally broadcast pallet glow, expiring 200 ticks after
 * selection. Never highlight every rack returned by a search result.
 */
public final class RackHighlightClient {
    private record Glow(BlockPos rack,long expires){}
    private static final Map<ClientLevel,Glow> ACTIVE=new WeakHashMap<>();
    private RackHighlightClient(){}
    public static void accept(String ignoredSnapshot){
        // Search provides results, not world glowing. Only the server-side
        // Highlight button can select a pallet for all players.
    }
    public static void select(BlockPos rack,int ticks){
        ClientLevel level=Minecraft.getInstance().level;
        if(level==null)return;
        if(ticks<=0)ACTIVE.remove(level);
        else ACTIVE.put(level,new Glow(rack.immutable(),level.getGameTime()+ticks));
    }
    public static boolean highlighted(Level level,BlockPos position){
        if(!(level instanceof ClientLevel client))return false;
        Glow glow=ACTIVE.get(client);
        if(glow==null)return false;
        if(glow.expires()<=level.getGameTime()){
            ACTIVE.remove(client);
            return false;
        }
        return glow.rack().equals(position);
    }
}
