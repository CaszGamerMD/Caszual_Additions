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
    // The names are individual shelf tokens, not generated on every spawn.
    private final java.util.ArrayDeque<String> storedNames=new java.util.ArrayDeque<>(2);
    private boolean debugChat,debugStatuses;
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
    public boolean debugChat(){return debugChat;}
    public boolean debugStatuses(){return debugStatuses;}
    public void setDebug(boolean chat,boolean statuses){
        debugChat=chat;debugStatuses=statuses;setChanged();
    }
    public String nextName(){return storedNames.peekFirst();}
    public String newestName(){return storedNames.peekLast();}
    /** Adding a new golem token assigns a friendly name once. Returning
     * entities pass their existing name and keep it across redocking. */
    public boolean add(){return addNamed(null);}
    public boolean addNamed(String existing){
        if(docked>=2)return false;
        String name=existing==null||existing.isBlank()
                ?WarehouseGolemNames.random(level==null
                        ?net.minecraft.util.RandomSource.create()
                        :level.getRandom()):existing;
        storedNames.addLast(name);
        docked++;setChanged();return true;
    }
    public String takeNamed(){
        if(docked<=0)return null;
        docked--;
        String name=storedNames.isEmpty()
                ?WarehouseGolemNames.random(level==null
                        ?net.minecraft.util.RandomSource.create()
                        :level.getRandom()):storedNames.removeFirst();
        setChanged();
        return name;
    }
    public boolean take(){return takeNamed()!=null;}
    public void clearDocked(){docked=0;storedNames.clear();setChanged();}
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
        out.putBoolean("debug_chat",debugChat);
        out.putBoolean("debug_statuses",debugStatuses);
        int i=0;
        for(String name:storedNames)out.putString("stored_golem_name_"+i++,name);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);docked=Math.clamp(in.getIntOr("docked_golems",0),0,2);
        sortingEnabled=in.getBooleanOr("sorting_enabled",true);
        recallSerial=Math.max(0,in.getLongOr("warehouse_recall_serial",0L));
        debugChat=in.getBooleanOr("debug_chat",false);
        debugStatuses=in.getBooleanOr("debug_statuses",false);
        storedNames.clear();
        for(int i=0;i<docked;i++){
            String name=in.getStringOr("stored_golem_name_"+i,"");
            storedNames.addLast(name.isBlank()
                    ?WarehouseGolemNames.random(net.minecraft.util.RandomSource.create())
                    :name);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider p){
        return saveWithoutMetadata(p);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    public static void tick(ServerLevel level,StorageGolemShelfEntity shelf){
        if(level.getGameTime()%20!=0)return;
        // The warehouse computer owns Builder order state. Queue allocation
        // cannot depend on a Builder/player visiting the computer or on
        // automatic INPUT sorting being enabled.
        var group=StorageGolemShelfEntity.linked(level,shelf.getBlockPos());
        BlockPos computer=WarehouseConnections.computerForShelves(level,group);
        if(computer!=null)
            BuilderWarehouseFulfillment.progressWaiting(level,computer);
        if(shelf.docked()==0)return;
        // SORT: OFF only stops automatic INPUT sorting. An explicit Builder
        // order still summons available pickers and never requires a player
        // to toggle the warehouse back to automatic sorting.
        // A single 2-second tick previously launched only ONE packer
        // even when four order slots and shelf Golems were free. Fill the
        // configured crew in this dispatch cycle; persisted per-PTP
        // reservations prevent duplicate claims and overdispatch.
        for(int launched=0;launched<4&&shelf.docked()>0;launched++)
            if(!StorageGolemDispatch.tryDispatchQueuedOutput(level,shelf))break;
        // Automatic inbound sorting is independent of explicit orders.
        if(shelf.sortingEnabled&&shelf.docked()>0)
            StorageGolemDispatch.tryDispatch(level,shelf);
    }
}
