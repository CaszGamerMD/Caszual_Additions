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
    /** Find a moving stair controller by its assigned worker. Called by the
     * mover every tick, only within the previously known 12-block area. */
    public static RackStairsBlockEntity findAssignedStairs(ServerLevel world,
            java.util.UUID mover,BlockPos around){
        if(mover==null||around.equals(BlockPos.ZERO))return null;
        for(int y=-2;y<=2;y++)for(int x=-12;x<=12;x++)
            for(int z=-12;z<=12;z++){
                BlockPos pos=around.offset(x,y,z);
                if(!world.hasChunkAt(pos))continue;
                if(world.getBlockEntity(pos) instanceof RackStairsBlockEntity stairs
                        &&mover.equals(stairs.mover()))return stairs;
            }
        return null;
    }
    /** Ask for one free worker to shepherd the mobile stairs to an upper
     * rack. The warehouse retains all four pallet Golems as separate tasks;
     * this worker comes from another stocked shelf, NEVER from the
     * already assigned cargo group.
     */
    public static boolean requestStairs(ServerLevel world,RackBlockEntity rack){
        BlockPos rackPos=rack.getBlockPos();
        if(rackPos.getY()<=world.getMinY()+2)return false;
        Direction front=rack.facing();
        // Need steps to point TOWARD the rack. Three treads occupy the
        // outside aisle; the highest ends at the elevated access level.
        Direction up=front.getOpposite();
        BlockPos desired=rackPos.relative(front,3).below(3);
        RackStairsBlockEntity stairs=null;
        for(int dx=-16;dx<=16&&stairs==null;dx++)
            for(int dz=-16;dz<=16&&stairs==null;dz++)
                for(int dy=-3;dy<=3;dy++){
                    BlockPos pos=desired.offset(dx,dy,dz);
                    if(!world.hasChunkAt(pos))continue;
                    if(world.getBlockEntity(pos) instanceof RackStairsBlockEntity found
                            &&found.getBlockState().getValue(RackStairsBlock.FACING)==up
                            &&!found.reserved()){stairs=found;break;}
                }
        if(stairs==null)return false;
        // The stairs themselves may be elsewhere; a shelf Golem must
        // physically travel to them, then escort them to this rack.
        for(int dx=-18;dx<=18;dx++)for(int dz=-18;dz<=18;dz++){
            BlockPos pos=rackPos.offset(dx,-3,dz);
            if(!world.hasChunkAt(pos))continue;
            if(!(world.getBlockEntity(pos) instanceof StorageGolemShelfEntity shelf)
                    ||shelf.docked()==0)continue;
            BlockPos exit=StorageGolemShelfDoor.exit(world,shelf);
            if(exit==null)continue;
            StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(world,
                    net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if(worker==null)return false;
            String name=shelf.takeNamed();
            if(name==null)return false;
            worker.assignRackStairMover(shelf.getBlockPos(),stairs.getBlockPos());
            worker.setGolemName(name);
            StorageGolemShelfDoor.emerge(worker,exit,StorageGolemShelfDoor.front(shelf));
            if(!world.addFreshEntity(worker)){
                shelf.addNamed(name);return false;
            }
            if(!stairs.assign(desired,worker.getUUID())){
                worker.discard();shelf.addNamed(name);return false;
            }
            return true;
        }
        return false;
    }
    public static boolean stairsReady(ServerLevel world,RackBlockEntity rack){
        BlockPos desired=rack.getBlockPos().relative(rack.facing(),3).below(3);
        if(!world.hasChunkAt(desired))return false;
        if(world.getBlockEntity(desired) instanceof RackStairsBlockEntity stair
                &&stair.reserved())return true;
        return false;
    }
    /** Finished work triggers an independent RETURN order, which moves
     * the stairs back to where the player LAST positioned them. */
    public static void finishStairAccess(ServerLevel world,BlockPos rackRoot,
            Direction rackFacing){
        BlockPos desired=rackRoot.relative(rackFacing,3).below(3);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            BlockPos at=desired.offset(dx,0,dz);
            if(!world.hasChunkAt(at))continue;
            if(world.getBlockEntity(at) instanceof RackStairsBlockEntity stair
                    &&stair.reserved())stair.release();
        }
    }

}
