package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;
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

public final class ShulkerTankEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {
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

    @Override public Component getDisplayName(){
        return Component.literal("Shulker Station");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,
            Inventory inventory,Player player){
        return new ShulkerStationMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){
        return worldPosition;
    }

    /** These lanes are the real saved ItemStacks; the screen never copies
     * goods into a client-only fake inventory. */
    public ItemStack workingBox(int lane){
        return lane>=0&&lane<STATIONS?working.get(lane):ItemStack.EMPTY;
    }
    public ItemStack readyBox(int lane){
        return lane>=0&&lane<STATIONS?finished.get(lane):ItemStack.EMPTY;
    }
    private static boolean validBox(ItemStack stack){
        return stack.isEmpty()||(stack.is(ItemTags.SHULKER_BOXES)
                &&!stack.has(DataComponents.CONTAINER_LOOT)&&stack.getCount()==1);
    }
    public void setWorkingBox(int lane,ItemStack item){
        if(lane<0||lane>=STATIONS||!validBox(item))return;
        bufferedItems=Math.max(0,bufferedItems
                -StorageGolemPacking.itemCount(working.get(lane))
                +StorageGolemPacking.itemCount(item));
        working.set(lane,item.isEmpty()?ItemStack.EMPTY:item.copyWithCount(1));
        setChanged();
    }
    public void setReadyBox(int lane,ItemStack item){
        if(lane<0||lane>=STATIONS||!validBox(item))return;
        bufferedItems=Math.max(0,bufferedItems
                -StorageGolemPacking.itemCount(finished.get(lane))
                +StorageGolemPacking.itemCount(item));
        finished.set(lane,item.isEmpty()?ItemStack.EMPTY:item.copyWithCount(1));
        setChanged();
    }
    /** Take one physical empty box from the magazine into a chosen idle lane. */
    public boolean loadEmptyIntoLane(int lane){
        if(lane<0||lane>=STATIONS||!working.get(lane).isEmpty()
                ||!finished.get(lane).isEmpty())return false;
        ItemStack next=take();
        if(next.isEmpty())return false;
        working.set(lane,next.copyWithCount(1));
        setChanged();
        return true;
    }
    private static NonNullList<ItemStack> boxSlots(ItemStack box){
        NonNullList<ItemStack> slots=NonNullList.withSize(27,ItemStack.EMPTY);
        if(!box.isEmpty()){
            ItemContainerContents content=box.get(DataComponents.CONTAINER);
            if(content!=null)content.copyInto(slots);
        }
        return slots;
    }
    public ItemStack workingItem(int lane,int slot){
        if(slot<0||slot>=27||lane<0||lane>=STATIONS)return ItemStack.EMPTY;
        return boxSlots(working.get(lane)).get(slot);
    }
    public boolean canEditWorking(int lane,ItemStack item){
        return lane>=0&&lane<STATIONS&&!working.get(lane).isEmpty()
                &&(item.isEmpty()||item.getItem().canFitInsideContainerItems());
    }
    /** Keep the actual 27 Minecraft inventory positions, including holes
     * created by dragging items out, not only a compact list of nonempties. */
    public void editWorking(int lane,int slot,ItemStack item){
        if(slot<0||slot>=27||!canEditWorking(lane,item))return;
        ItemStack old=working.get(lane);
        NonNullList<ItemStack> values=boxSlots(old);
        values.set(slot,item.isEmpty()?ItemStack.EMPTY:item.copy());
        boolean any=false;
        for(ItemStack stack:values)if(!stack.isEmpty()){any=true;break;}
        ItemStack replacement=StorageGolemPacking.emptyVersion(old);
        if(any)replacement.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(values));
        setWorkingBox(lane,replacement);
    }
    public void replaceWorkingContents(int lane,List<ItemStack> values){
        if(lane<0||lane>=STATIONS||values.size()!=27
                ||working.get(lane).isEmpty())return;
        for(ItemStack value:values)
            if(!value.isEmpty()&&!value.getItem().canFitInsideContainerItems())
                return;
        ItemStack updated=StorageGolemPacking.emptyVersion(working.get(lane));
        if(values.stream().anyMatch(item->!item.isEmpty()))
            updated.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(values));
        setWorkingBox(lane,updated);
    }
    /** Commit only the selected real shulker to the physically adjacent
     * OUTPUT adapter's PTP; failed insertion retains the boxed inventory. */
    public boolean sendSelected(ServerLevel level,int lane){
        if(lane<0||lane>=STATIONS)return false;
        PtpBlockEntity output=attachedOutput(level);
        if(output==null)return false;
        if(!working.get(lane).isEmpty()){
            if(!finished.get(lane).isEmpty())return false;
            finished.set(lane,working.get(lane));
            working.set(lane,ItemStack.EMPTY);
            setChanged();
        }
        ItemStack box=finished.get(lane);
        if(box.isEmpty())return false;
        ItemStack moving=box.copy();
        if(StorageGolemDispatch.insert(level,output.getBlockPos(),moving)!=1
                ||!moving.isEmpty())return false;
        bufferedItems=Math.max(0,bufferedItems-StorageGolemPacking.itemCount(box));
        finished.set(lane,ItemStack.EMPTY);
        setChanged();
        return true;
    }
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
                if(current.isEmpty()&&!nextFinished.get(station).isEmpty())
                    continue; // no phantom consumption of an empty shulker
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
    /** Builder orders obey the same sealing rule as player orders:
     * transfer only FULL shulkers, or finish partial ones once the ENTIRE
     * submitted order is packed. Never flush a part-filled shulker
     * between individual worker trips.
     */
    public void flushBuilderBatch(ServerLevel level,PtpBlockEntity ptp){
        if(ptp==null||!ptp.builderReserved()||attachedOutput(level)!=ptp)return;
        if(ptp.pendingOutputOrders()==0&&!ptp.outputInFlight())
            finishOrder(level,ptp);
        else flushFinished(level,ptp,true);
    }
    /** Full shulkers are ready immediately; partially filled shulkers
     * become ready when the whole submitted order queue finishes. */
    public void finishOrder(ServerLevel level,PtpBlockEntity ptp){
        if(ptp==null||ptp.pendingOutputOrders()!=0||ptp.outputInFlight())return;
        for(int i=0;i<STATIONS;i++){
            if(working.get(i).isEmpty()||!finished.get(i).isEmpty())continue;
            finished.set(i,working.get(i));
            working.set(i,ItemStack.EMPTY);
        }
        setChanged();
        flushFinished(level,ptp,true);
    }
    public void flushFinished(ServerLevel level){
        PtpBlockEntity output=attachedOutput(level);
        if(output!=null&&output.pendingOutputOrders()==0&&!output.outputInFlight())
            finishOrder(level,output);
        flushFinished(level,output,true);
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
    public List<ItemStack> takePackedOnBreak(){
        List<ItemStack> result=new ArrayList<>();
        for(int i=0;i<STATIONS;i++){
            if(!working.get(i).isEmpty())result.add(working.get(i).copy());
            if(!finished.get(i).isEmpty())result.add(finished.get(i).copy());
            working.set(i,ItemStack.EMPTY);
            finished.set(i,ItemStack.EMPTY);
        }
        bufferedItems=0;
        setChanged();
        return result;
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
