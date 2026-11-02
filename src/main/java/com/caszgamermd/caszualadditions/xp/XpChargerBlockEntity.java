package com.caszgamermd.caszualadditions.xp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Three independent one-item Mending docks. The original single-slot save
 * key remains readable so older chargers retain their existing item. */
public final class XpChargerBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOTS=3;
    private static final int[] ALL_SLOTS={0,1,2};
    private NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    public XpChargerBlockEntity(BlockPos pos,BlockState state){
        super(XpBlockEntities.CHARGER,pos,state);
    }
    /** Compatibility accessor for older Jade integrations. */
    public ItemStack item(){
        for(ItemStack stack:items)if(!stack.isEmpty())return stack;
        return ItemStack.EMPTY;
    }
    public int occupied(){
        int total=0;
        for(ItemStack stack:items)if(!stack.isEmpty())total++;
        return total;
    }
    public int firstFree(){
        for(int i=0;i<SLOTS;i++)if(items.get(i).isEmpty())return i;
        return -1;
    }
    public int firstReady(){
        for(int i=0;i<SLOTS;i++)
            if(!items.get(i).isEmpty()&&!items.get(i).isDamaged())return i;
        return -1;
    }
    public int firstOccupied(){
        for(int i=0;i<SLOTS;i++)if(!items.get(i).isEmpty())return i;
        return -1;
    }
    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){return occupied()==0;}
    @Override public ItemStack getItem(int slot){
        return slot>=0&&slot<SLOTS?items.get(slot):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot,int amount){
        if(slot<0||slot>=SLOTS||amount<=0)return ItemStack.EMPTY;
        ItemStack taken=ContainerHelper.removeItem(items,slot,amount);
        if(!taken.isEmpty())setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        if(slot<0||slot>=SLOTS)return ItemStack.EMPTY;
        ItemStack taken=ContainerHelper.takeItem(items,slot);
        if(!taken.isEmpty())setChanged();
        return taken;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS)return;
        items.set(slot,stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1));
        setChanged();
    }
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean stillValid(Player player){
        return level!=null&&level.getBlockEntity(worldPosition)==this
                &&player.distanceToSqr(worldPosition.getX()+.5,
                    worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;
    }
    @Override public void clearContent(){
        items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();
    }
    @Override public int[] getSlotsForFace(Direction side){return ALL_SLOTS;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return slot>=0&&slot<SLOTS&&items.get(slot).isEmpty()
                &&XpRepair.hasMending(stack)&&stack.isDamaged();
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){
        return canPlaceItem(slot,stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){
        return slot>=0&&slot<SLOTS&&!items.get(slot).isEmpty()
                &&!items.get(slot).isDamaged();
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out,items);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,items);
        // Existing worlds saved a single item under "item".
        if(items.get(0).isEmpty())
            items.set(0,in.read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY));
        for(int i=0;i<SLOTS;i++){
            ItemStack stack=items.get(i);
            if(!stack.isEmpty()&&stack.getCount()>1)
                items.set(i,stack.copyWithCount(1));
        }
    }
}
