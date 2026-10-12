package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One coherent loaded-only warehouse network. Every touching warehouse
 * component conducts the signal. Industrial Grade Concrete conducts in all
 * 16 colors, including from beneath a warehouse block; a computer must
 * physically touch the network to power it.
 *
 * Physical rack frames are multi-block. Their existing horizontal connector
 * networks are followed as a single rack family before the flood continues.
 * Never loads a chunk, and caps traversal against unlimited IGC paving.
 */
public final class WarehouseNetwork {
    public static final int MAX_CELLS=16384;
    /** Reuse expensive adjacency walks only within the SAME server tick.
     * Breaking concrete or rearranging racks is re-evaluated next tick. */
    private record Cache(long tick,Result result){}
    private static final Map<ServerLevel,Map<BlockPos,Cache>> CACHE=
            new java.util.WeakHashMap<>();
    private WarehouseNetwork(){}
    private static Result cached(ServerLevel level,BlockPos start,List<BlockPos> origins){
        Map<BlockPos,Cache> map=CACHE.computeIfAbsent(level,l->new LinkedHashMap<>());
        if(map.size()>256)map.clear();
        Cache entry=map.get(start);
        long now=level.getGameTime();
        if(entry!=null&&entry.tick()==now)return entry.result();
        Result result=traverse(level,origins);
        map.put(start,new Cache(now,result));
        return result;
    }

    public record Result(List<RackBlockEntity> racks,
            List<StorageGolemShelfEntity> shelves,
            List<PtpBlockEntity> points,
            List<ShulkerTankEntity> tanks,
            List<BlockPos> computers,int concreteBlocks){}

    private static boolean member(BlockState state){
        return state.getBlock() instanceof IndustrialGradeConcreteBlock
                ||state.getBlock() instanceof RackBlock
                ||state.getBlock() instanceof PalletBlock
                ||state.getBlock() instanceof RackConnectorBlock
                ||state.getBlock() instanceof PtpBlock
                ||state.getBlock() instanceof StorageGolemShelfBlock
                ||state.getBlock() instanceof ShulkerTankBlock
                ||state.is(RackContent.RACK_COMPUTER);
    }

    public static Result fromComputer(ServerLevel level,BlockPos computer){
        if(!level.hasChunkAt(computer)
                ||!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return empty();
        return cached(level,computer,List.of(computer));
    }
    public static Result fromShelfGroup(ServerLevel level,
            List<StorageGolemShelfEntity> shelves){
        List<BlockPos> origins=new ArrayList<>();
        for(StorageGolemShelfEntity shelf:shelves)origins.add(shelf.getBlockPos());
        return origins.isEmpty()?empty():cached(level,origins.getFirst(),origins);
    }
    private static Result empty(){
        return new Result(List.of(),List.of(),List.of(),List.of(),List.of(),0);
    }
    private static Result traverse(ServerLevel level,List<BlockPos> origins){
        ArrayDeque<BlockPos> queue=new ArrayDeque<>(origins);
        Set<BlockPos> seen=new HashSet<>();
        Set<BlockPos> rackExpanded=new HashSet<>();
        Map<BlockPos,RackBlockEntity> racks=new LinkedHashMap<>();
        Map<BlockPos,StorageGolemShelfEntity> shelves=new LinkedHashMap<>();
        Map<BlockPos,PtpBlockEntity> points=new LinkedHashMap<>();
        Map<BlockPos,ShulkerTankEntity> tanks=new LinkedHashMap<>();
        Set<BlockPos> computers=new java.util.LinkedHashSet<>();
        int concrete=0;
        while(!queue.isEmpty()&&seen.size()<MAX_CELLS){
            BlockPos pos=queue.removeFirst();
            if(!seen.add(pos)||!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            if(!member(state))continue;
            if(state.is(RackContent.RACK_COMPUTER))computers.add(pos);
            if(state.getBlock() instanceof IndustrialGradeConcreteBlock)concrete++;
            if(state.getBlock() instanceof RackBlock){
                RackBlockEntity rack=RackBlock.controller(level,pos);
                if(rack!=null&&rackExpanded.add(rack.getBlockPos())){
                    // Expand actual touching racks and bridged rack beams, then
                    // visit every frame part for attached shelves and IGC.
                    for(RackBlockEntity connected:RackNetwork.network(level,rack)){
                        if(racks.putIfAbsent(connected.getBlockPos(),connected)!=null)continue;
                        rackExpanded.add(connected.getBlockPos());
                        for(int part=0;part<12;part++)
                            queue.add(RackBlock.cell(connected.getBlockPos(),
                                    connected.facing(),part));
                    }
                }
            }
            if(state.getBlock() instanceof StorageGolemShelfBlock
                    &&level.getBlockEntity(pos) instanceof StorageGolemShelfEntity shelf)
                shelves.putIfAbsent(pos,shelf);
            if(state.getBlock() instanceof PtpBlock){
                int part=state.getValue(PtpBlock.PART);
                if(part>0){
                    BlockPos center=PtpBlock.root(pos,part).offset(1,0,1);
                    if(level.hasChunkAt(center)
                            &&level.getBlockEntity(center) instanceof PtpBlockEntity ptp)
                        points.putIfAbsent(center,ptp);
                }
            }
            if(state.getBlock() instanceof ShulkerTankBlock){
                ShulkerTankEntity tank=ShulkerTankBlock.controller(level,pos);
                if(tank!=null)tanks.putIfAbsent(tank.getBlockPos(),tank);
            }
            for(Direction face:Direction.values()){
                BlockPos adjacent=pos.relative(face);
                if(!seen.contains(adjacent)&&level.hasChunkAt(adjacent))
                    queue.add(adjacent);
            }
        }
        return new Result(new ArrayList<>(racks.values()),
                new ArrayList<>(shelves.values()),new ArrayList<>(points.values()),
                new ArrayList<>(tanks.values()),new ArrayList<>(computers),concrete);
    }
}
