package com.caszgamermd.caszualadditions.antiwarden;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Scoped registry of ACTIVE (captured-Warden) snare traps. Only active
 * traps silence Warden + sculk triggers. Avoid scanning 4913 block
 * positions for every emitted sound or game event.
 */
public final class WardenTrapSilence {
    public static final double RADIUS=9.0;
    private static final WeakHashMap<ServerLevel,Set<BlockPos>> ACTIVE=
            new WeakHashMap<>();
    private WardenTrapSilence(){}
    public static void update(ServerLevel world,BlockPos at,boolean active){
        Set<BlockPos> positions=ACTIVE.computeIfAbsent(world,x->new HashSet<>());
        if(active)positions.add(at.immutable());else positions.remove(at);
    }
    public static boolean near(ServerLevel level,Vec3 source){
        Set<BlockPos> positions=ACTIVE.get(level);
        if(positions==null||positions.isEmpty())return false;
        // On stale registry entries (e.g. block broken), drop them rather
        // than muting the whole area for the lifetime of the server.
        positions.removeIf(pos->!level.hasChunkAt(pos)
                ||!(level.getBlockEntity(pos) instanceof WardenTrapBlockEntity trap)
                ||!trap.isHolding());
        for(BlockPos pos:positions){
            if(source.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)
                    <=RADIUS*RADIUS)return true;
        }
        return false;
    }
    public static boolean sculkOrWardenSound(String soundId){
        return soundId.contains("warden")||soundId.contains("sculk")
                ||soundId.contains("shriek");
    }
}
