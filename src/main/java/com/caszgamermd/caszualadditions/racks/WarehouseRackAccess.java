package com.caszgamermd.caszualadditions.racks;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Physical rack-access clearance coordinator. Pallets wait for workers to
 * leave an occupied scaffold; guardrails and deck fold together, and both
 * automatically unfold after a completed transfer once the path is clear.
 */
public final class WarehouseRackAccess {
    private WarehouseRackAccess(){}
    private record Pending(ServerLevel level,BlockPos pos){}
    private static final Set<Pending> UNFOLD=new HashSet<>();
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%20!=0)return;
            UNFOLD.removeIf(task->{
                if(!task.level.hasChunkAt(task.pos))return true;
                var state=task.level.getBlockState(task.pos);
                if(!(state.getBlock() instanceof RackScaffoldingBlock))return true;
                return RackScaffoldingBlock.setFolded(task.level,task.pos,false);
            });
        });
    }
    private static java.util.List<BlockPos> atAccess(ServerLevel world,
            RackBlockEntity rack){
        java.util.List<BlockPos> positions=new java.util.ArrayList<>();
        BlockPos root=rack.getBlockPos();
        Direction side=rack.facing();
        // A pallet is 2 across and requires an aisle to approach or
        // lower. Platforms mounted at raised rack bay heights fold first.
        for(int h=0;h<=2;h++)for(int col=0;col<2;col++)
            for(int dist=1;dist<=2;dist++){
                BlockPos at=RackBlock.cell(root,side,col)
                        .relative(side,dist).above(h);
                if(world.hasChunkAt(at)
                        &&world.getBlockState(at).getBlock()
                                instanceof RackScaffoldingBlock)
                    positions.add(at);
            }
        return positions;
    }
    public static boolean prepare(ServerLevel level,RackBlockEntity rack){
        var positions=atAccess(level,rack);
        // Validate ALL walkways before folding ANYTHING. Don't leave
        // half the elevated aisle folded when an occupant blocks the rest.
        for(BlockPos at:positions)
            if(RackScaffoldingBlock.occupied(level,at))return false;
        for(BlockPos at:positions)
            if(!RackScaffoldingBlock.setFolded(level,at,true))return false;
        return true;
    }
    public static void release(ServerLevel level,RackBlockEntity rack){
        for(BlockPos at:atAccess(level,rack)){
            if(!RackScaffoldingBlock.setFolded(level,at,false))
                UNFOLD.add(new Pending(level,at));
        }
    }
    /** If a mobile stair blocks a bay, a pallet cannot pass through it.
     * Golems wait for the stair mover instead of clipping through. */
    public static boolean clearOfStairs(ServerLevel level,RackBlockEntity rack){
        var pos=rack.getBlockPos();
        for(int h=0;h<=2;h++)for(int col=0;col<2;col++)
            for(int dist=1;dist<=3;dist++){
                BlockPos at=RackBlock.cell(pos,rack.facing(),col)
                        .relative(rack.facing(),dist).above(h);
                if(level.hasChunkAt(at)
                        &&level.getBlockState(at).getBlock()
                                instanceof RackStairsBlock)return false;
            }
        return true;
    }
}
