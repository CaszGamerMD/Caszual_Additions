package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 8x8 real empty-shulker magazine, plus three separate packing statuses. */
public final class ShulkerStationMenu extends AbstractContainerMenu {
    public static final int COLS=8,ROWS=8,STOCK=64;
    private final Container station;
    private int emptyCount,packingCount,readyCount,bufferedCount,connected;

    public ShulkerStationMenu(int id,Inventory inventory,BlockPos pos){
        this(id,inventory,ShulkerTankBlock.controller(inventory.player.level(),pos));
    }
    public ShulkerStationMenu(int id,Inventory inventory,Container source){
        super(RackContent.SHULKER_STATION_MENU,id);
        station=source==null?new SimpleContainer(STOCK):source;
        addDataSlot(new DataSlot(){
            @Override public int get(){return station instanceof ShulkerTankEntity tank
                    ?tank.count():emptyCount;}
            @Override public void set(int value){emptyCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return station instanceof ShulkerTankEntity tank
                    ?tank.activeCount():packingCount;}
            @Override public void set(int value){packingCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return station instanceof ShulkerTankEntity tank
                    ?tank.finishedCount():readyCount;}
            @Override public void set(int value){readyCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return station instanceof ShulkerTankEntity tank
                    ?tank.bufferedItems():bufferedCount;}
            @Override public void set(int value){bufferedCount=value;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){
                if(!(station instanceof ShulkerTankEntity tank)
                        ||!(tank.getLevel() instanceof net.minecraft.server.level.ServerLevel server))
                    return connected;
                return tank.attachedOutput(server)!=null?1:0;
            }
            @Override public void set(int value){connected=value;}
        });
        for(int row=0;row<ROWS;row++)
            for(int col=0;col<COLS;col++)
                addSlot(new Slot(station,col+row*COLS,
                        17+col*18,40+row*18){
                    @Override public boolean mayPlace(ItemStack item){
                        return ShulkerTankEntity.isEmptyShulker(item);
                    }
                    @Override public int getMaxStackSize(){return 1;}
                });
        addStandardInventorySlots(inventory,8,203);
    }
    public int emptyCount(){return emptyCount;}
    public int packingCount(){return packingCount;}
    public int readyCount(){return readyCount;}
    public int bufferedCount(){return bufferedCount;}
    public boolean outputConnected(){return connected!=0;}
    @Override public boolean stillValid(Player player){return station.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int slotIndex){
        if(slotIndex<0||slotIndex>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(slotIndex);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack original=slot.getItem();
        ItemStack result=original.copy();
        boolean moved=slotIndex<STOCK
                ?moveItemStackTo(original,STOCK,slots.size(),true)
                :ShulkerTankEntity.isEmptyShulker(original)
                        &&moveItemStackTo(original,0,STOCK,false);
        if(!moved)return ItemStack.EMPTY;
        if(original.isEmpty())slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
