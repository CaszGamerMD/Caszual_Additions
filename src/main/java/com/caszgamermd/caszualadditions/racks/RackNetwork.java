package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Only follows loaded, physically touching 2x2x3 rack frames. */
public final class RackNetwork {
    public static final int LIMIT = 256;
    private RackNetwork() {}

    public record Link(RackBlockEntity rack, boolean aisle) {}

    public static List<RackBlockEntity> touching(ServerLevel level,BlockPos root) {
        List<RackBlockEntity> neighbors=new ArrayList<>();
        BlockState state=level.getBlockState(root);
        if (!(state.getBlock() instanceof RackBlock))return neighbors;
        Direction facing=state.getValue(RackBlock.FACING);
        Set<BlockPos> seen=new HashSet<>();
        for(int i=0;i<12;i++){
            BlockPos p=RackBlock.cell(root,facing,i);
            for(Direction side:Direction.values()){
                BlockPos adjacent=p.relative(side);
                if(!level.hasChunkAt(adjacent))continue;
                BlockState other=level.getBlockState(adjacent);
                if(!(other.getBlock() instanceof RackBlock))continue;
                BlockPos otherRoot=RackBlock.root(adjacent,other);
                if(otherRoot.equals(root)||!seen.add(otherRoot))continue;
                if(level.getBlockEntity(otherRoot) instanceof RackBlockEntity rack)
                    neighbors.add(rack);
            }
        }
        return neighbors;
    }

    public static boolean aisleNeighbors(RackBlockEntity a,RackBlockEntity b) {
        if(a.facing()!=b.facing() || a.getBlockPos().getY()!=b.getBlockPos().getY())return false;
        Direction right=a.facing().getCounterClockWise();
        int dx=b.getBlockPos().getX()-a.getBlockPos().getX();
        int dz=b.getBlockPos().getZ()-a.getBlockPos().getZ();
        return (dx==right.getStepX()*2 && dz==right.getStepZ()*2)
                || (dx==-right.getStepX()*2 && dz==-right.getStepZ()*2);
    }

    public static boolean connected(RackBlockEntity a,RackBlockEntity b) {
        if(aisleNeighbors(a,b))return true;
        // Connector cards on any contacting face add the racks to the network.
        // Side-to-side automatic links remain one aisle; front/back/vertical
        // links let a warehouse contain multiple named aisles.
        for(Direction dir:Direction.values()){
            if(a.hasConnector(dir) && b.hasConnector(dir.getOpposite()))return true;
        }
        return false;
    }

    public static List<RackBlockEntity> network(ServerLevel level,RackBlockEntity start) {
        List<RackBlockEntity> result=new ArrayList<>();
        ArrayDeque<RackBlockEntity> queue=new ArrayDeque<>();
        Set<BlockPos> seen=new HashSet<>();
        queue.add(start);seen.add(start.getBlockPos());
        while(!queue.isEmpty() && result.size()<LIMIT){
            RackBlockEntity rack=queue.removeFirst();
            result.add(rack);
            for(RackBlockEntity neighbor:touching(level,rack.getBlockPos())){
                if(connected(rack,neighbor)&&seen.add(neighbor.getBlockPos()))
                    queue.add(neighbor);
            }
        }
        return result;
    }

    public static String aisle(ServerLevel level,RackBlockEntity start) {
        ArrayDeque<RackBlockEntity> queue=new ArrayDeque<>();
        Set<BlockPos> seen=new HashSet<>();
        queue.add(start);seen.add(start.getBlockPos());
        int n=0;
        while(!queue.isEmpty()&&n++<LIMIT){
            RackBlockEntity rack=queue.removeFirst();
            if(!rack.aisleName().isBlank())return rack.aisleName();
            for(RackBlockEntity other:touching(level,rack.getBlockPos()))
                if(aisleNeighbors(rack,other)&&seen.add(other.getBlockPos()))queue.add(other);
        }
        return "";
    }
    public static String label(ServerLevel level,RackBlockEntity rack) {
        String aisle=aisle(level,rack);
        return aisle.isEmpty()?rack.displayName():aisle+" - "+rack.displayName();
    }

    /** A computer can attach to any cell of one neighboring rack. */
    public static List<RackBlockEntity> fromComputer(ServerLevel level,BlockPos computer) {
        for(Direction side:Direction.values()){
            BlockPos p=computer.relative(side);
            if(!level.hasChunkAt(p))continue;
            RackBlockEntity root=RackBlock.controller(level,p);
            if(root!=null)return network(level,root);
        }
        return List.of();
    }
}
