package com.caszgamermd.caszualadditions.racks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Search outlines are private to the player operating this terminal.
 *  Client worlds are weakly referenced; no particles or server-wide effects. */
public final class RackHighlightClient {
    private static final Map<ClientLevel,Map<BlockPos,Long>> ACTIVE=new WeakHashMap<>();
    private RackHighlightClient(){}
    public static void accept(String snapshot){
        ClientLevel level=Minecraft.getInstance().level;
        if(level==null)return;
        JsonObject root;
        try{root=JsonParser.parseString(snapshot).getAsJsonObject();}
        catch(RuntimeException ignored){return;}
        if(!root.has("query")||root.get("query").getAsString().isBlank()
                ||!root.has("highlights"))return;
        Map<BlockPos,Long> map=ACTIVE.computeIfAbsent(level,l->new HashMap<>());
        long until=level.getGameTime()+200;
        for(var value:root.getAsJsonArray("highlights")){
            JsonObject p=value.getAsJsonObject();
            map.put(new BlockPos(p.get("x").getAsInt(),p.get("y").getAsInt(),
                    p.get("z").getAsInt()),until);
        }
    }
    public static boolean highlighted(Level level,BlockPos position){
        if(!(level instanceof ClientLevel client))return false;
        Map<BlockPos,Long> map=ACTIVE.get(client);
        if(map==null)return false;
        Long expires=map.get(position);
        if(expires==null)return false;
        if(expires<=level.getGameTime()){
            map.remove(position);
            return false;
        }
        return true;
    }
}
