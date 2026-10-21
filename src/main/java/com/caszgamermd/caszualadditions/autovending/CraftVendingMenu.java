package com.caszgamermd.caszualadditions.autovending;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Nine live recipe samples, 27 real stock slots, one read-only preview.
 * The button controls adjust complete recipe operations, and the screen
 * shows the exact item output count after multiplying by the recipe yield.
 */
public final class CraftVendingMenu extends AbstractContainerMenu {
    public static final int TEMPLATE=9,STOCK=27,PREVIEW=36,PLAYER_START=37;
    private final Container machine;
    private final CraftVendingBlockEntity entity;
    private final SimpleContainer preview=new SimpleContainer(1);
    private int batchCount=1;

    public CraftVendingMenu(int id,Inventory player,BlockPos pos){
        this(id,player,resolve(player,pos));
    }
    private static Container resolve(Inventory player,BlockPos pos){
        BlockEntity be=player.player.level().getBlockEntity(pos);
        return be instanceof CraftVendingBlockEntity machine
                ?machine:new SimpleContainer(TEMPLATE+STOCK);
    }
    public CraftVendingMenu(int id,Inventory player,Container backing){
        super(CraftVendingContent.MENU,id);
        machine=backing;
        entity=backing instanceof CraftVendingBlockEntity machine?machine:null;
        addDataSlot(new DataSlot(){
            @Override public int get(){
                return entity!=null?entity.batches():batchCount;
            }
            @Override public void set(int value){batchCount=Math.clamp(value,1,CraftVendingBlockEntity.MAX_BATCHES);}
        });

        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            addSlot(new Slot(machine,row*3+col,29+col*18,39+row*18){
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(machine,TEMPLATE+row*9+col,28+col*18,127+row*18));
        addSlot(new Slot(preview,0,160,56){
            @Override public boolean mayPickup(Player player){return false;}
            @Override public boolean mayPlace(ItemStack stack){return false;}
        });
        addStandardInventorySlots(player,28,208);
        updateResult();
    }

    public int batchCount(){return batchCount;}
    public int outputCount(){
        ItemStack item=preview.getItem(0);
        return item.isEmpty()?0:item.getCount()*batchCount;
    }
    public ItemStack output(){return preview.getItem(0);}

    private void updateResult(){
        if(entity==null)return;
        ItemStack result=entity.preview();
        if(!ItemStack.matches(result,preview.getItem(0)))
            preview.setItem(0,result);
    }
    @Override public void broadcastChanges(){
        updateResult();
        super.broadcastChanges();
    }
    @Override public void slotsChanged(Container changed){
        updateResult();
        super.slotsChanged(changed);
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(id<0||id>3||!stillValid(player))return false;
        int change=switch(id){case 0->-1;case 1->1;case 2->-8;default->8;};
        if(entity!=null)
            entity.setBatches(entity.batches()+change);
        else batchCount=Math.clamp(batchCount+change,1,
                CraftVendingBlockEntity.MAX_BATCHES);
        broadcastFullState();
        return true;
    }
    @Override public boolean stillValid(Player player){
        return machine.stillValid(player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||index==PREVIEW)return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack moving=slot.getItem(),original=moving.copy();
        boolean moved=index<PREVIEW
                ?moveItemStackTo(moving,PLAYER_START,slots.size(),true)
                :moveItemStackTo(moving,TEMPLATE,PREVIEW,false);
        if(!moved)return ItemStack.EMPTY;
        if(moving.isEmpty())slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
