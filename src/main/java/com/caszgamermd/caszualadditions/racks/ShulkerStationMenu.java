package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 64 real empty shulkers + 3 active and 3 ready boxes + a 27-slot editable
 * viewport into ONE selected real working shulker. No ghost item storage.
 */
public final class ShulkerStationMenu extends AbstractContainerMenu {
    public static final int COLS=8,ROWS=8,STOCK=64,LANES=3,BOX_SLOTS=27;
    public static final int WORK_START=STOCK,READY_START=STOCK+LANES;
    public static final int CONTENT_START=STOCK+LANES*2,PLAYER_START=CONTENT_START+BOX_SLOTS;
    private final Container station;
    private final ShulkerTankEntity tank;
    private int emptyCount,packingCount,readyCount,bufferedCount,connected,selectedLane;

    public ShulkerStationMenu(int id,Inventory inventory,BlockPos pos){
        this(id,inventory,ShulkerTankBlock.controller(inventory.player.level(),pos));
    }
    public ShulkerStationMenu(int id,Inventory inventory,Container source){
        super(RackContent.SHULKER_STATION_MENU,id);
        station=source==null?new SimpleContainer(STOCK):source;
        tank=station instanceof ShulkerTankEntity entity?entity:null;
        addDataSlot(new DataSlot(){
            @Override public int get(){return tank==null?emptyCount:tank.count();}
            @Override public void set(int value){emptyCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return tank==null?packingCount:tank.activeCount();}
            @Override public void set(int value){packingCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return tank==null?readyCount:tank.finishedCount();}
            @Override public void set(int value){readyCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return tank==null?bufferedCount:tank.bufferedItems();}
            @Override public void set(int value){bufferedCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){
                if(tank==null||!(tank.getLevel() instanceof ServerLevel level))return connected;
                return tank.attachedOutput(level)!=null?1:0;
            }
            @Override public void set(int value){connected=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return selectedLane;}
            @Override public void set(int value){selectedLane=Math.clamp(value,0,LANES-1);}
        });
        for(int row=0;row<ROWS;row++)
            for(int col=0;col<COLS;col++)
                addSlot(new Slot(station,col+row*COLS,16+col*18,48+row*18){
                    @Override public boolean mayPlace(ItemStack item){
                        return ShulkerTankEntity.isEmptyShulker(item);
                    }
                    @Override public int getMaxStackSize(){return 1;}
                });
        Container boxLanes=new LaneContainer();
        for(int i=0;i<LANES;i++){
            final int lane=i;
            addSlot(new Slot(boxLanes,lane,207+50*i,54){
                @Override public boolean mayPlace(ItemStack stack){
                    return ShulkerTankEntity.isEmptyShulker(stack);
                }
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int i=0;i<LANES;i++)
            addSlot(new Slot(boxLanes,LANES+i,207+50*i,85){
                @Override public boolean mayPlace(ItemStack stack){return false;}
                @Override public int getMaxStackSize(){return 1;}
            });
        Container selected=new SelectedShulkerContainer();
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(selected,col+row*9,184+col*18,135+row*18){
                @Override public boolean mayPlace(ItemStack item){
                    return tank!=null&&tank.canEditWorking(selectedLane,item);
                }
            });
        addStandardInventorySlots(inventory,110,283);
    }
    public int emptyCount(){return emptyCount;}
    public int packingCount(){return packingCount;}
    public int readyCount(){return readyCount;}
    public int bufferedCount(){return bufferedCount;}
    public int selectedLane(){return selectedLane;}
    public boolean outputConnected(){return connected!=0;}
    @Override public boolean stillValid(Player player){return station.stillValid(player);}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player))return false;
        if(id>=0&&id<LANES){
            selectedLane=id;
            broadcastFullState();
            return true;
        }
        if(tank==null||!(player.level() instanceof ServerLevel level))return false;
        if(id==3){
            boolean success=tank.loadEmptyIntoLane(selectedLane);
            if(!success)player.sendSystemMessage(Component.literal(
                    "Selected lane occupied, or no empty Shulker Boxes available."));
            broadcastFullState();
            return success;
        }
        if(id==4){
            boolean success=tank.sendSelected(level,selectedLane);
            if(!success)player.sendSystemMessage(Component.literal(
                    "Cannot send selected shulker: connect a free OUTPUT PTP via a touching Hopper Adapter."));
            broadcastFullState();
            return success;
        }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int slotIndex){
        if(slotIndex<0||slotIndex>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(slotIndex);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack original=slot.getItem(),result=original.copy();
        // Shulker contents are decoded from a data component on every read.
        // Their ItemStacks are snapshots, not mutable Container references:
        // explicitly write the remaining count back after shift-extraction.
        if(slotIndex>=CONTENT_START&&slotIndex<PLAYER_START){
            ItemStack remainder=original.copy();
            if(!moveItemStackTo(remainder,PLAYER_START,slots.size(),true))
                return ItemStack.EMPTY;
            slot.setByPlayer(remainder);
            return result;
        }
        boolean moved;
        if(slotIndex<PLAYER_START){
            moved=moveItemStackTo(original,PLAYER_START,slots.size(),true);
        }else if(ShulkerTankEntity.isEmptyShulker(original)){
            moved=moveItemStackTo(original,0,STOCK,false);
            if(!moved)moved=moveItemStackTo(original,WORK_START,READY_START,false);
        }else{
            // Prevent silent loss: a generic vanilla shift-merge into a
            // component-backed box would only mutate a temporary snapshot.
            // Players may drag items directly to its 27 live editable slots.
            moved=false;
        }
        if(!moved)return ItemStack.EMPTY;
        if(original.isEmpty())slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private final class LaneContainer implements Container {
        @Override public int getContainerSize(){return LANES*2;}
        @Override public boolean isEmpty(){
            if(tank==null)return true;
            for(int i=0;i<getContainerSize();i++)if(!getItem(i).isEmpty())return false;
            return true;
        }
        @Override public ItemStack getItem(int slot){
            if(tank==null||slot<0||slot>=LANES*2)return ItemStack.EMPTY;
            return slot<LANES?tank.workingBox(slot):tank.readyBox(slot-LANES);
        }
        @Override public ItemStack removeItem(int slot,int amount){
            if(amount<=0)return ItemStack.EMPTY;
            ItemStack old=getItem(slot);
            if(old.isEmpty())return ItemStack.EMPTY;
            ItemStack taken=old.copyWithCount(1);
            setItem(slot,ItemStack.EMPTY);
            return taken;
        }
        @Override public ItemStack removeItemNoUpdate(int slot){return removeItem(slot,1);}
        @Override public void setItem(int slot,ItemStack item){
            if(tank==null||slot<0||slot>=LANES*2)return;
            if(slot<LANES)tank.setWorkingBox(slot,item);
            else tank.setReadyBox(slot-LANES,item);
        }
        @Override public boolean canPlaceItem(int slot,ItemStack item){
            return slot>=0&&slot<LANES&&ShulkerTankEntity.isEmptyShulker(item);
        }
        @Override public void setChanged(){if(tank!=null)tank.setChanged();}
        @Override public boolean stillValid(Player player){return station.stillValid(player);}
        @Override public void clearContent(){}
    }

    private final class SelectedShulkerContainer implements Container {
        // Minecraft vanilla Slot mutates ItemStacks IN PLACE before invoking
        // Container.setChanged(). Return stable cache entries and commit the
        // whole 27-slot component once setChanged() fires. Returning detached
        // streams here silently deletes or duplicates shift/drag transfers.
        private net.minecraft.core.NonNullList<ItemStack> cache=
                net.minecraft.core.NonNullList.withSize(BOX_SLOTS,ItemStack.EMPTY);
        private ItemStack baseline=ItemStack.EMPTY;
        private int cachedLane=-1;
        private void sync(){
            if(tank==null)return;
            ItemStack current=tank.workingBox(selectedLane);
            if(cachedLane==selectedLane&&ItemStack.matches(baseline,current))return;
            cache=net.minecraft.core.NonNullList.withSize(BOX_SLOTS,ItemStack.EMPTY);
            for(int i=0;i<BOX_SLOTS;i++)
                cache.set(i,tank.workingItem(selectedLane,i).copy());
            baseline=current.copy();
            cachedLane=selectedLane;
        }
        private void commit(){
            if(tank==null||cachedLane!=selectedLane||baseline.isEmpty())return;
            tank.replaceWorkingContents(selectedLane,cache);
            baseline=tank.workingBox(selectedLane).copy();
        }
        @Override public int getContainerSize(){return BOX_SLOTS;}
        @Override public boolean isEmpty(){
            sync();
            for(ItemStack stack:cache)if(!stack.isEmpty())return false;
            return true;
        }
        @Override public ItemStack getItem(int slot){
            if(slot<0||slot>=BOX_SLOTS)return ItemStack.EMPTY;
            sync();
            return cache.get(slot);
        }
        @Override public ItemStack removeItem(int slot,int count){
            if(slot<0||slot>=BOX_SLOTS||count<=0)return ItemStack.EMPTY;
            sync();
            ItemStack taken=net.minecraft.world.ContainerHelper.removeItem(cache,slot,count);
            if(!taken.isEmpty())commit();
            return taken;
        }
        @Override public ItemStack removeItemNoUpdate(int slot){
            if(slot<0||slot>=BOX_SLOTS)return ItemStack.EMPTY;
            sync();
            ItemStack taken=net.minecraft.world.ContainerHelper.takeItem(cache,slot);
            if(!taken.isEmpty())commit();
            return taken;
        }
        @Override public void setItem(int slot,ItemStack stack){
            if(slot<0||slot>=BOX_SLOTS||tank==null
                    ||!tank.canEditWorking(selectedLane,stack))return;
            sync();
            cache.set(slot,stack.copy());
            commit();
        }
        @Override public boolean canPlaceItem(int slot,ItemStack stack){
            return slot>=0&&slot<BOX_SLOTS&&tank!=null
                    &&tank.canEditWorking(selectedLane,stack);
        }
        @Override public void setChanged(){commit();}
        @Override public boolean stillValid(Player player){return station.stillValid(player);}
        @Override public void clearContent(){}
    }

}
