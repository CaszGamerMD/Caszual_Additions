package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/** Short-lived, automatically expiring simulation tickets for ACTIVE warehouse
 * work. The Computer and its real physical PTP, shulker, source racks and
 * shelving stay awake only while jobs are outstanding.
 *
 * No player /forceload state, chunk scans or permanent persistent tickets.
 * Bounded to sixteen centers, renewed every 40 ticks and expired after 200.
 */
public final class WarehouseOrderTickets {
    private static TicketType type;
    private static final int RADIUS=2,MAX_CENTERS=16;
    private WarehouseOrderTickets(){}
    public static void initialize(){
        if(type!=null)return;
        type=Registry.register(BuiltInRegistries.TICKET_TYPE,
                CaszualAdditions.id("warehouse_active_orders"),
                new TicketType(200,TicketType.FLAG_LOADING
                        |TicketType.FLAG_SIMULATION
                        |TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    }
    private static void add(Set<ChunkPos> centers,BlockPos pos){
        if(pos!=null&&centers.size()<MAX_CENTERS)
            centers.add(new ChunkPos(pos.getX()>>4,pos.getZ()>>4));
    }
    public static void keep(ServerLevel level,BlockPos computer,
            WarehouseNetwork.Result network){
        if(type==null||!level.hasChunkAt(computer))return;
        boolean active=level.getBlockEntity(computer)
                instanceof WarehouseComputerBlockEntity config
                &&!config.waitingBuilders().isEmpty();
        for(PtpBlockEntity point:network.points())if(point.builderReserved()
                ||point.preparingForBuilder()||point.needsBuilderPalletReturn()
                ||point.pendingOutputOrders()>0||point.outputInFlight()){
            active=true;break;
        }
        if(!active)return;
        Set<ChunkPos> centers=new LinkedHashSet<>();
        add(centers,computer);
        for(PtpBlockEntity point:network.points())
            if(point.builderReserved()||point.preparingForBuilder()
                    ||point.needsBuilderPalletReturn()||point.outputInFlight())
                add(centers,point.getBlockPos());
        for(ShulkerTankEntity station:network.tanks())
            add(centers,station.getBlockPos());
        for(StorageGolemShelfEntity shelf:network.shelves())
            add(centers,shelf.getBlockPos());
        for(StackOfPalletsBlockEntity dispenser:network.magazines())
            add(centers,dispenser.getBlockPos());
        for(RackBlockEntity rack:network.racks())
            if(rack.hasPallet()&&!rack.isEmpty())add(centers,rack.getBlockPos());
        for(ChunkPos center:centers)
            level.getChunkSource().addTicketWithRadius(type,center,RADIUS);
    }
}
