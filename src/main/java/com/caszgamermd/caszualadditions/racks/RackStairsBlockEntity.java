package com.caszgamermd.caszualadditions.racks;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent physical mobile stairs. The player-set home is NEVER replaced
 * by automatic service dispatch; after servicing a rack, a Golem escorts
 * the stairs back to that remembered location. Every step waits for riders.
 */
public final class RackStairsBlockEntity extends BlockEntity {
    private BlockPos home,target;
    private UUID mover;
    private boolean requested,returning;
    private long nextMove;
    public record Snapshot(BlockPos home,BlockPos target,UUID mover,
            boolean requested,boolean returning,long nextMove){}
    public RackStairsBlockEntity(BlockPos pos,BlockState state){
        super(RackContent.RACK_STAIRS_ENTITY,pos,state);
        home=pos.immutable();
        target=home;
    }
    public BlockPos home(){return home;}
    public BlockPos destination(){return target;}
    public boolean reserved(){return requested;}
    public UUID mover(){return mover;}
    public Snapshot snapshot(){return new Snapshot(home,target,mover,
            requested,returning,nextMove);}
    public void restore(Snapshot snapshot,BlockPos newHome){
        home=newHome.immutable();
        target=snapshot.target;
        mover=snapshot.mover;
        requested=snapshot.requested;
        returning=snapshot.returning;
        nextMove=snapshot.nextMove;
        setChanged();
    }
    public void setHome(BlockPos home){this.home=home.immutable();setChanged();}
    public boolean assign(BlockPos goal,UUID assigned){
        if(requested||goal==null||assigned==null)return false;
        target=goal.immutable();mover=assigned;
        requested=true;returning=false;setChanged();
        return true;
    }
    public void release(){
        if(!requested)return;
        returning=true;target=home;setChanged();
    }
    public static void tick(ServerLevel level,BlockPos root,RackStairsBlockEntity be){
        // Minecraft doesn't notify diagonal blocks on removal. Reconcile the
        // upper stair and any scaffolds once per second, even when idle.
        if(level.getGameTime()%20==0)
            RackStairsBlock.refreshTopDockState(level,
                    RackStairsBlock.cell(root,be.getBlockState().getValue(RackStairsBlock.FACING),8));
        if(!be.requested||level.getGameTime()<be.nextMove)return;
        if(root.equals(be.target)){
            if(be.returning){be.requested=false;be.mover=null;be.setChanged();}
            return;
        }
        // The work must be performed by the assigned live Storage Golem,
        // not by teleporting a piece of equipment across the warehouse.
        if(!(level.getEntity(be.mover) instanceof StorageGolem escort)
                ||!escort.isAlive())return;
        if(escort.distanceToSqr(root.getX()+.5,root.getY(),
                root.getZ()+.5)>24)return;
        if(RackStairsBlock.occupied(level,root,
                be.getBlockState().getValue(RackStairsBlock.FACING)))return;
        int dx=be.target.getX()-root.getX();
        int dz=be.target.getZ()-root.getZ();
        BlockPos next=root.offset(Integer.signum(dx),0,0);
        if(dx==0)next=root.offset(0,0,Integer.signum(dz));
        be.nextMove=level.getGameTime()+16;
        RackStairsBlock.move(level,root,next,false);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.store("rack_stairs_home",BlockPos.CODEC,home);
        out.store("rack_stairs_target",BlockPos.CODEC,target);
        out.putBoolean("rack_stairs_requested",requested);
        out.putBoolean("rack_stairs_returning",returning);
        out.putLong("rack_stairs_next",nextMove);
        if(mover!=null)out.putString("rack_stairs_mover",mover.toString());
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        home=in.read("rack_stairs_home",BlockPos.CODEC).orElse(worldPosition);
        target=in.read("rack_stairs_target",BlockPos.CODEC).orElse(home);
        requested=in.getBooleanOr("rack_stairs_requested",false);
        returning=in.getBooleanOr("rack_stairs_returning",false);
        nextMove=in.getLongOr("rack_stairs_next",0);
        try{mover=UUID.fromString(in.getStringOr("rack_stairs_mover",""));}
        catch(IllegalArgumentException ex){mover=null;}
    }
}
