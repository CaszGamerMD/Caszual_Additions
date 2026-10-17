package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Bounded, expiring chunk tickets for PHYSICAL automatic pallet exchanges.
 *
 * A remote scanner request must wake its linked warehouse even when the
 * player is standing beside the distant wrapped pallet. A moving rig keeps
 * only its local walking area and the active destination awake. No /forceload
 * flags are written and no inventory is moved between unloaded chunks.
 *
 * Tickets time out automatically when a Scanner is lost, a job completes,
 * or the loaded carrier is destroyed. Persist for a brief server restart so
 * the saved carrier can resume rather than becoming stranded on unload.
 */
public final class WarehouseRemoteTickets {
    private WarehouseRemoteTickets(){}
    private static TicketType type;
    private static final int RADIUS=2;
    public static void initialize(){
        if(type!=null)return;
        type=Registry.register(BuiltInRegistries.TICKET_TYPE,
                CaszualAdditions.id("warehouse_remote_haul"),
                new TicketType(240,
                        TicketType.FLAG_LOADING
                        |TicketType.FLAG_SIMULATION
                        |TicketType.FLAG_KEEP_DIMENSION_ACTIVE
                        |TicketType.FLAG_PERSIST));
    }
    public static void keep(ServerLevel level,BlockPos pos){
        if(type==null)throw new IllegalStateException("Warehouse remote ticket not registered");
        level.getChunkSource().addTicketWithRadius(type,new ChunkPos(pos),RADIUS);
    }
}
