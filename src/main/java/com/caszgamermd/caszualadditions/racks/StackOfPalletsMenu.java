package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Actual 12 separate inventory slots, plus ordinary player inventory. */
public final class StackOfPalletsMenu extends AbstractContainerMenu {
    public static final int CAPACITY=12;
    private final Container magazine;
    public StackOfPalletsMenu(int id,Inventory inventory,BlockPos pos){
        this(id,inventory,
                inventory.player.level().getBlockEntity(pos) instanceof StackOfPalletsBlockEntity box
                        ?box:null);
    }
    public StackOfPalletsMenu(int id,Inventory inventory,Container source){
        super(RackContent.PALLET_STACK_MENU,id);
        magazine=source==null?new SimpleContainer(CAPACITY):source;
        for(int i=0;i<CAPACITY;i++)
            addSlot(new Slot(magazine,i,52+(i%4)*18,35+(i/4)*18){
                @Override public boolean mayPlace(ItemStack stack){
                    return StackOfPalletsItem.isPallet(stack);
                }
                @Override public int getMaxStackSize(){return 1;}
            });
        addStandardInventorySlots(inventory,8,111);
    }
    @Override public boolean stillValid(Player player){
        return magazine.stillValid(player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot source=slots.get(index);
        if(!source.hasItem())return ItemStack.EMPTY;
        ItemStack original=source.getItem(),result=original.copy();
        boolean moved=index<CAPACITY
                ?moveItemStackTo(original,CAPACITY,slots.size(),true)
                :StackOfPalletsItem.isPallet(original)
                    &&moveItemStackTo(original,0,CAPACITY,false);
        if(!moved)return ItemStack.EMPTY;
        if(original.isEmpty())source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return result;
    }
}
