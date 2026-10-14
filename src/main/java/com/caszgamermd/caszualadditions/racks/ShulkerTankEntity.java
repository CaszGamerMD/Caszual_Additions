package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ShulkerTankEntity extends BlockEntity implements Container {
    public static final int CAPACITY=64;
    private NonNullList<ItemStack> boxes=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
    // Existing 64-slot empty-box magazine is preserved in saved worlds.
    // Three active packing positions and three finished-box output buffers
    // are separate from the empty magazine and saved with the block entity.
    public static final int STATIONS=3;
    private NonNullList<ItemStack> working=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
    private NonNullList<ItemStack> finished=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
    private int bufferedItems;
    public ShulkerTankEntity(BlockPos pos,BlockState state){super(RackContent.SHULKER_TANK_ENTITY,pos,state);}

    public int activeCount(){
        int count=0;
        for(ItemStack box:working)if(!box.isEmpty())count++;
        return count;
    }
    public int finishedCount(){
        int count=0;
        for(ItemStack box:finished)if(!box.isEmpty())count++;
        return count;
    }
    public int bufferedItems(){return bufferedItems;}
    /** Only a physically touching corner Hopper Adapter is allowed to
     * push packed shulkers into an OUTPUT PTP pallet. Network connectivity
     * alone is deliberately NOT enough. */
    public PtpBlockEntity attachedOutput(ServerLevel level){
        for(int y=0;y<=2;y++){
            BlockPos center=worldPosition.above(y);
            for(Direction side:Direction.Plane.HORIZONTAL){
                BlockPos portAt=center.relative(side);
                if(!level.hasChunkAt(portAt))continue;
                if(level.getBlockEntity(portAt) instanceof PtpHopperPort port){
                    PtpBlockEntity ptp=PtpBlock.controller(level,portAt);
                    if(ptp!=null&&!ptp.input()&&ptp.hasAdapter()
                            &&ptp.readyForOutput())return ptp;
                }
            }
        }
        return null;
    }
    /** Simulation is side-effect free; a nine-stack trip either fits as a
     * WHOLE or the Golem retains everything and can safely return it. */
    public boolean canAccept(List<ItemStack> delivery,ServerLevel level,
            PtpBlockEntity requested){
        return pack(delivery,level,requested,false);
    }
    public boolean accept(List<ItemStack> delivery,ServerLevel level,
            PtpBlockEntity requested){
        return pack(delivery,level,requested,true);
    }
    private boolean pack(List<ItemStack> delivery,ServerLevel level,
            PtpBlockEntity requested,boolean commit){
        if(delivery.isEmpty()||requested==null||attachedOutput(level)!=requested)
            return false;
        NonNullList<ItemStack> nextWorking=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
        NonNullList<ItemStack> nextFinished=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
        NonNullList<ItemStack> nextMagazine=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        for(int i=0;i<STATIONS;i++){
            nextWorking.set(i,working.get(i).copy());
            nextFinished.set(i,finished.get(i).copy());
        }
        for(int i=0;i<CAPACITY;i++)nextMagazine.set(i,boxes.get(i).copy());
        for(ItemStack input:delivery){
            if(input.isEmpty()||!input.getItem().canFitInsideContainerItems())
                return false;
            boolean added=false;
            for(int station=0;station<STATIONS&&!added;station++){
                ItemStack current=nextWorking.get(station);
                ItemStack base=current.isEmpty()?ItemStack.EMPTY
                        :StorageGolemPacking.emptyVersion(current);
                if(current.isEmpty()){
                    for(int slot=0;slot<CAPACITY;slot++)
                        if(isEmptyShulker(nextMagazine.get(slot))){
                            base=nextMagazine.get(slot);
                            nextMagazine.set(slot,ItemStack.EMPTY);
                            break;
                        }
                }
                if(base.isEmpty())continue;
                List<ItemStack> old=current.isEmpty()?new ArrayList<>()
                        :new ArrayList<>(StorageGolemPacking.unpack(current));
                if(old.size()>=27)continue;
                old.add(input.copy());
                ItemStack newBox=StorageGolemPacking.fill(base,old);
                if(newBox.isEmpty())return false;
                if(old.size()==27){
                    if(!nextFinished.get(station).isEmpty())continue;
                    nextFinished.set(station,newBox);
                    nextWorking.set(station,ItemStack.EMPTY);
                }else nextWorking.set(station,newBox);
                added=true;
            }
            if(!added)return false;
        }
        if(commit){
            boxes=nextMagazine;
            working=nextWorking;
            finished=nextFinished;
            bufferedItems+=delivery.stream().mapToInt(ItemStack::getCount).sum();
            setChanged();
            flushFinished(level,requested,false);
        }
        return true;
    }
    /** Full shulkers are ready immediately; partially filled shulkers
     * become ready when the whole submitted order queue finishes. */
    public void finishOrder(ServerLevel level,PtpBlockEntity ptp){
        if(ptp==null||ptp.pendingOutputOrders()!=0)return;
        for(int i=0;i<STATIONS;i++){
            if(working.get(i).isEmpty()||!finished.get(i).isEmpty())continue;
            finished.set(i,working.get(i));
            working.set(i,ItemStack.EMPTY);
        }
        setChanged();
        flushFinished(level,ptp,true);
    }
    public void flushFinished(ServerLevel level){
        flushFinished(level,attachedOutput(level),true);
    }
    private void flushFinished(ServerLevel level,PtpBlockEntity ptp,boolean safeOnly){
        if(ptp==null||!ptp.readyForOutput())return;
        for(int i=0;i<STATIONS;i++){
            ItemStack box=finished.get(i);
            if(box.isEmpty())continue;
            ItemStack moving=box.copy();
            if(StorageGolemDispatch.insert(level,ptp.getBlockPos(),moving)==1
                    &&moving.isEmpty()){
                bufferedItems=Math.max(0,bufferedItems-
                        StorageGolemPacking.itemCount(box));
                finished.set(i,ItemStack.EMPTY);
                setChanged();
            }
        }
    }

    public static boolean isEmptyShulker(ItemStack stack) {
        if(stack.isEmpty()||!stack.is(ItemTags.SHULKER_BOXES)
                ||stack.has(DataComponents.CONTAINER_LOOT))return false;
        ItemContainerContents contents=stack.get(DataComponents.CONTAINER);
        return contents==null || contents.nonEmptyItemCopyStream().findAny().isEmpty();
    }
    public int count() {
        int count=0;
        for(ItemStack stack:boxes)if(!stack.isEmpty())count++;
        return count;
    }
    public boolean add(ItemStack box) {
        if(!isEmptyShulker(box))return false;
        for(int i=0;i<CAPACITY;i++){
            if(boxes.get(i).isEmpty()){
                boxes.set(i,box.copyWithCount(1));
                setChanged();
                return true;
            }
        }
        return false;
    }
    public ItemStack peek() {
        for (int i=CAPACITY-1;i>=0;i--)
            if(!boxes.get(i).isEmpty())return boxes.get(i).copyWithCount(1);
        return ItemStack.EMPTY;
    }
    public ItemStack take() {
        for(int i=CAPACITY-1;i>=0;i--){
            ItemStack box=boxes.get(i);
            if(!box.isEmpty()){
                boxes.set(i,ItemStack.EMPTY);
                setChanged();
                return box;
            }
        }
        return ItemStack.EMPTY;
    }
    @Override public int getContainerSize(){return CAPACITY;}
    @Override public boolean isEmpty(){return count()==0;}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<CAPACITY?boxes.get(slot):ItemStack.EMPTY;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return isEmptyShulker(stack);}
    @Override public ItemStack removeItem(int slot,int n){
        ItemStack item=ContainerHelper.removeItem(boxes,slot,n);
        if(!item.isEmpty())setChanged();
        return item;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        ItemStack item=ContainerHelper.takeItem(boxes,slot);
        if(!item.isEmpty())setChanged();
        return item;
    }
    @Override public void setItem(int slot,ItemStack stack) {
        if(slot<0||slot>=CAPACITY||(!stack.isEmpty()&&!canPlaceItem(slot,stack)))return;
        boxes.set(slot,stack);
        setChanged();
    }
    @Override public void clearContent(){boxes=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player,6.0f);}
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output,boxes);
        for(int i=0;i<STATIONS;i++){
            if(!working.get(i).isEmpty())
                output.store("station_active_"+i,ItemStack.CODEC,working.get(i));
            if(!finished.get(i).isEmpty())
                output.store("station_ready_"+i,ItemStack.CODEC,finished.get(i));
        }
        output.putInt("station_buffered",bufferedItems);
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        boxes=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,boxes);
        working=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
        finished=NonNullList.withSize(STATIONS,ItemStack.EMPTY);
        for(int i=0;i<STATIONS;i++){
            working.set(i,input.read("station_active_"+i,ItemStack.CODEC)
                    .orElse(ItemStack.EMPTY));
            finished.set(i,input.read("station_ready_"+i,ItemStack.CODEC)
                    .orElse(ItemStack.EMPTY));
        }
        bufferedItems=Math.max(0,input.getIntOr("station_buffered",0));
    }
}
