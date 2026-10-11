package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
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
    public ShulkerTankEntity(BlockPos pos,BlockState state){super(RackContent.SHULKER_TANK_ENTITY,pos,state);}

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
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        boxes=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,boxes);
    }
}
