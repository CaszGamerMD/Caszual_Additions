package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Only follows loaded, physically touching 2x2x3 rack frames. */
public final class RackNetwork {
    public static final int LIMIT = 256;
    private RackNetwork() {}

    public record Link(RackBlockEntity rack, boolean aisle) {}

    public static List<RackBlockEntity> touching(Level level,BlockPos root) {
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

    public static boolean aisleNeighbors(Level level,RackBlockEntity a,RackBlockEntity b) {
        if(a.getBlockPos().getY()!=b.getBlockPos().getY())return false;
        // Parallel side-by-side bays share an aisle automatically.
        if(a.facing()==b.facing()){
            Direction right=a.facing().getCounterClockWise();
            int dx=b.getBlockPos().getX()-a.getBlockPos().getX();
            int dz=b.getBlockPos().getZ()-a.getBlockPos().getZ();
            if((dx==right.getStepX()*2&&dz==right.getStepZ()*2)
                    ||(dx==-right.getStepX()*2&&dz==-right.getStepZ()*2))return true;
        }
        // Paired connectors on the actual SIDE faces extend an aisle even
        // across differently-oriented bays. Front/back/vertical connectors
        // link the computer network but start/retain distinct aisles.
        for(int i=0;i<4;i++) {
            BlockPos p=RackBlock.cell(a.getBlockPos(),a.facing(),i);
            for(Direction direction:Direction.Plane.HORIZONTAL){
                if(direction.getAxis()==a.facing().getAxis()
                        || direction.getAxis()==b.facing().getAxis()
                        || !a.hasConnector(direction)
                        || !b.hasConnector(direction.getOpposite()))continue;
                BlockPos neighboring=p.relative(direction);
                if(!level.hasChunkAt(neighboring))continue;
                BlockState other=level.getBlockState(neighboring);
                if(other.getBlock() instanceof RackBlock
                        && RackBlock.root(neighboring,other).equals(b.getBlockPos()))
                    return true;
            }
        }
        return false;
    }

    public static boolean connected(ServerLevel level,RackBlockEntity a,RackBlockEntity b) {
        if(aisleNeighbors(level,a,b))return true;
        // Connectors must match the ACTUAL shared face. Checking only whether
        // the racks have some complementary connector sides could connect
        // touching racks across a completely different axis by mistake.
        for(int part=0;part<12;part++){
            BlockPos from=RackBlock.cell(a.getBlockPos(),a.facing(),part);
            for(Direction direction:Direction.values()){
                if(!a.hasConnector(direction)
                        || !b.hasConnector(direction.getOpposite()))continue;
                BlockPos target=from.relative(direction);
                if(!level.hasChunkAt(target))continue;
                BlockState other=level.getBlockState(target);
                if(other.getBlock() instanceof RackBlock
                        && RackBlock.root(target,other).equals(b.getBlockPos()))
                    return true;
            }
        }
        return false;
    }

    public static List<RackBlockEntity> network(ServerLevel level,RackBlockEntity start) {
        return network(level,List.of(start));
    }

    public static List<RackBlockEntity> network(
            ServerLevel level,List<RackBlockEntity> startingRacks) {
        List<RackBlockEntity> result=new ArrayList<>();
        ArrayDeque<RackBlockEntity> queue=new ArrayDeque<>();
        Set<BlockPos> seen=new HashSet<>();
        for(RackBlockEntity rack:startingRacks)
            if(rack!=null && seen.add(rack.getBlockPos()))queue.add(rack);
        while(!queue.isEmpty() && result.size()<LIMIT){
            RackBlockEntity rack=queue.removeFirst();
            result.add(rack);
            for(RackBlockEntity neighbor:touching(level,rack.getBlockPos())){
                if(connected(level,rack,neighbor)&&seen.add(neighbor.getBlockPos()))
                    queue.add(neighbor);
            }
        }
        return result;
    }

    public static String aisle(Level level,RackBlockEntity start) {
        ArrayDeque<RackBlockEntity> queue=new ArrayDeque<>();
        Set<BlockPos> seen=new HashSet<>();
        queue.add(start);seen.add(start.getBlockPos());
        int n=0;
        while(!queue.isEmpty()&&n++<LIMIT){
            RackBlockEntity rack=queue.removeFirst();
            if(!rack.aisleName().isBlank())return rack.aisleName();
            for(RackBlockEntity other:touching(level,rack.getBlockPos()))
                if(aisleNeighbors(level,rack,other)&&seen.add(other.getBlockPos()))queue.add(other);
        }
        return "";
    }
    public static String label(Level level,RackBlockEntity rack) {
        String aisle=aisle(level,rack);
        // Only physically tagged racks receive the concise aisle prefix:
        // Aisle "A" + Rack Tag "12" = "A-12".
        if(!aisle.isBlank() && rack.hasRackPlate() && !rack.rackName().isBlank())
            return aisle+"-"+rack.rackName();
        return rack.displayName();
    }

    /** A computer can bridge ALL rack groups physically touching its faces. */
    public static List<RackBlockEntity> fromComputer(ServerLevel level,BlockPos computer) {
        List<RackBlockEntity> attached=new ArrayList<>();
        Set<BlockPos> seen=new HashSet<>();
        for(Direction side:Direction.values()){
            BlockPos p=computer.relative(side);
            if(!level.hasChunkAt(p))continue;
            RackBlockEntity root=RackBlock.controller(level,p);
            if(root!=null && seen.add(root.getBlockPos()))attached.add(root);
        }
        return attached.isEmpty()?List.of():network(level,attached);
    }
}
