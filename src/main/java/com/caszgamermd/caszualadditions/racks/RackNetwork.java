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
    public static final int MAX_CONNECTOR_GAP=4;

    /** Follow a solid row of physical connectors, never through empty air. */
    private static RackBlockEntity bridged(Level level,BlockPos edge,Direction side){
        if(!side.getAxis().isHorizontal())return null;
        Boolean corner=null;
        for(int n=1;n<=MAX_CONNECTOR_GAP;n++){
            BlockPos cell=edge.relative(side,n);
            if(!level.hasChunkAt(cell))return null;
            BlockState state=level.getBlockState(cell);
            if(!(state.getBlock() instanceof RackConnectorBlock)
                    ||state.getValue(RackConnectorBlock.FACING).getAxis()!=side.getAxis())
                return null;
            boolean currentCorner=state.getValue(RackConnectorBlock.CORNER_POSITIVE);
            if(corner!=null&&corner!=currentCorner)return null;
            corner=currentCorner;
            BlockPos next=cell.relative(side);
            if(!level.hasChunkAt(next))return null;
            RackBlockEntity target=RackBlock.controller(level,next);
            if(target!=null)return target;
        }
        return null;
    }
    private static boolean backToBack(Level level,RackBlockEntity a,RackBlockEntity b){
        if(a.facing()!=b.facing().getOpposite())return false;
        for(int part=0;part<12;part++){
            BlockPos cell=RackBlock.cell(a.getBlockPos(),a.facing(),part);
            BlockPos touching=cell.relative(a.facing().getOpposite());
            if(!level.hasChunkAt(touching))continue;
            BlockState state=level.getBlockState(touching);
            if(state.getBlock() instanceof RackBlock
                    &&RackBlock.root(touching,state).equals(b.getBlockPos()))
                return true;
        }
        return false;
    }

    public static List<RackBlockEntity> touching(Level level,BlockPos root) {
        List<RackBlockEntity> neighbors=new ArrayList<>();
        BlockState state=level.getBlockState(root);
        if (!(state.getBlock() instanceof RackBlock))return neighbors;
        Direction facing=state.getValue(RackBlock.FACING);
        Set<BlockPos> seen=new HashSet<>();
        for(int i=0;i<12;i++){
            BlockPos p=RackBlock.cell(root,facing,i);
            for(Direction side:Direction.values()){
                if(side.getAxis().isHorizontal()){
                    RackBlockEntity bridged=bridged(level,p,side);
                    if(bridged!=null&&!bridged.getBlockPos().equals(root)
                            &&seen.add(bridged.getBlockPos()))neighbors.add(bridged);
                }
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
        // Rods that bridge the physical SIDES of parallel-facing racks
        // also extend the SAME aisle name across a clear horizontal gap.
        // Front-to-front links join the computer network, but do not
        // overwrite the names of independently tagged aisles.
        if(a.facing()==b.facing())
            for(int part=0;part<12;part++){
                BlockPos cell=RackBlock.cell(a.getBlockPos(),a.facing(),part);
                for(Direction side:Direction.Plane.HORIZONTAL){
                    if(side.getAxis()==a.facing().getAxis())continue;
                    RackBlockEntity end=bridged(level,cell,side);
                    if(end!=null&&end.getBlockPos().equals(b.getBlockPos()))
                        return true;
                }
            }
        return false;
    }

    public static boolean connected(ServerLevel level,RackBlockEntity a,RackBlockEntity b) {
        if(aisleNeighbors(level,a,b)||backToBack(level,a,b))return true;
        // A continuous line of 1-4 horizontal connector blocks is both
        // a physical beam and a network edge. It works across gaps.
        for(int part=0;part<12;part++){
            BlockPos cell=RackBlock.cell(a.getBlockPos(),a.facing(),part);
            for(Direction direction:Direction.Plane.HORIZONTAL){
                RackBlockEntity bridged=bridged(level,cell,direction);
                if(bridged!=null&&bridged.getBlockPos().equals(b.getBlockPos()))
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
