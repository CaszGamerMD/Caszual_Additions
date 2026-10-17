package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A golem exists in precisely one place: stored as an item count on a shelf,
 * or deployed as a world entity. Dispatch removes a count before spawning.
 */
public final class StorageGolemShelfEntity extends BlockEntity {
    private static final int MAX_LINKED=128;
    private int docked;
    private boolean sortingEnabled=true;
    private long recallSerial;
    public StorageGolemShelfEntity(BlockPos pos,BlockState state){
        super(RackContent.STORAGE_SHELF_ENTITY,pos,state);
    }
    public int docked(){return docked;}
    public boolean sortingEnabled(){return sortingEnabled;}
    public long recallSerial(){return recallSerial;}
    public void requestRecall(){
        recallSerial=recallSerial==Long.MAX_VALUE?1:recallSerial+1;
        sortingEnabled=false;
        setChanged();
    }
    public void setSortingEnabled(boolean enabled){sortingEnabled=enabled;setChanged();}
    public boolean add(){if(docked>=2)return false;docked++;setChanged();return true;}
    public boolean take(){if(docked<=0)return false;docked--;setChanged();return true;}
    public void clearDocked(){docked=0;setChanged();}
    public static List<StorageGolemShelfEntity> linked(Level level,BlockPos start){
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Set<BlockPos> seen=new HashSet<>();
        List<StorageGolemShelfEntity> linked=new ArrayList<>();
        queue.add(start);seen.add(start);
        while(!queue.isEmpty() &&linked.size()<MAX_LINKED){
            BlockPos p=queue.removeFirst();
            if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof StorageGolemShelfEntity s))
                continue;
            linked.add(s);
            for(Direction d:Direction.values()){
                BlockPos other=p.relative(d);
                if(level.hasChunkAt(other)&&level.getBlockState(other).is(RackContent.STORAGE_SHELF)
                        &&seen.add(other))queue.addLast(other);
            }
        }
        return linked;
    }
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null){BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,3);}
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("docked_golems",docked);
        out.putBoolean("sorting_enabled",sortingEnabled);
        out.putLong("warehouse_recall_serial",recallSerial);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);docked=Math.clamp(in.getIntOr("docked_golems",0),0,2);
        sortingEnabled=in.getBooleanOr("sorting_enabled",true);
        recallSerial=Math.max(0,in.getLongOr("warehouse_recall_serial",0L));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider p){
        return saveWithoutMetadata(p);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    public static void tick(ServerLevel level,StorageGolemShelfEntity shelf){
        if(level.getGameTime()%40==0 &&shelf.sortingEnabled&&shelf.docked()>0)
            StorageGolemDispatch.tryDispatch(level,shelf);
    }
}
