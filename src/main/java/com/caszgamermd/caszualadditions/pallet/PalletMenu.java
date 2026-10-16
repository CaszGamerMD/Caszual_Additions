package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import com.caszgamermd.caszualadditions.racks.RackContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PalletMenu extends AbstractContainerMenu {
    public static final int COLS = 18;
    public static final int ROWS = 6;
    public static final int PAGE_SIZE = COLS * ROWS;

    private final Container backing;
    private final PalletPageContainer paged;
    private final DataSlot pageData;
    private final PalletFilterCarrier filter;
    private int page, mode, scope, ptpInputMode;
    public static final int FILTER_SLOT=PAGE_SIZE;

    public PalletMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolveClientContainer(inventory, pos));
    }

    public PalletMenu(int containerId, Inventory inventory, Container backing) {
        super(PalletContent.PALLET_MENU, containerId);
        this.backing = backing;
        this.filter = backing instanceof PalletFilterCarrier carrier ? carrier : null;
        this.mode=filter==null?0:filter.filterMode();
        this.scope=filter==null?0:filter.filterScope();
        this.ptpInputMode=backing instanceof com.caszgamermd.caszualadditions.racks.PtpBlockEntity point
                &&point.input()?1:0;
        this.paged = new PalletPageContainer(backing);
        this.pageData = new DataSlot() {
            @Override
            public int get() {
                return page;
            }

            @Override
            public void set(int value) {
                page = Math.max(0, Math.min(1, value));
                paged.setPage(page);
            }
        };
        addDataSlot(pageData);
        addDataSlot(new DataSlot() {
            @Override public int get(){return filter==null?mode:filter.filterMode();}
            @Override public void set(int value){mode=Math.clamp(value,0,1);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get(){return filter==null?scope:filter.filterScope();}
            @Override public void set(int value){scope=Math.clamp(value,0,3);}
        });

        addDataSlot(new DataSlot() {
            @Override public int get(){
                return backing instanceof com.caszgamermd.caszualadditions.racks.PtpBlockEntity point
                        &&point.input()?1:0;
            }
            @Override public void set(int value){ptpInputMode=value;}
        });

        // Shared with PalletScreen: 18 x 6 warehouse shelves (108 per page).
        int startX = 23;
        int startY = 51;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                addSlot(new Slot(paged, x + y * COLS, startX + x * 18, startY + y * 18));
            }
        }

        // Real filter-upgrade slot. Removing the module disables restrictions
        // without discarding saved settings; reinserting restores the rule.
        Container moduleSlot=new Container() {
            @Override public int getContainerSize(){return 1;}
            @Override public boolean isEmpty(){return getItem(0).isEmpty();}
            @Override public ItemStack getItem(int slot){
                return slot==0&&filter!=null?filter.filterUpgrade():ItemStack.EMPTY;
            }
            @Override public ItemStack removeItem(int slot,int count){
                ItemStack previous=getItem(slot);
                if(!previous.isEmpty()&&count>0&&filter!=null)filter.setFilterUpgrade(ItemStack.EMPTY);
                return previous;
            }
            @Override public ItemStack removeItemNoUpdate(int slot){return removeItem(slot,1);}
            @Override public void setItem(int slot,ItemStack stack){
                if(slot==0&&filter!=null)filter.setFilterUpgrade(stack);
            }
            @Override public boolean canPlaceItem(int slot,ItemStack stack){
                return slot==0&&filter!=null&&stack.is(RackContent.PALLET_FILTER);
            }
            @Override public void setChanged(){backing.setChanged();}
            @Override public boolean stillValid(Player player){return backing.stillValid(player);}
            @Override public void clearContent(){setItem(0,ItemStack.EMPTY);}
        };
        addSlot(new Slot(moduleSlot,0,344,76) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(RackContent.PALLET_FILTER) && filter!=null;
            }
            @Override public int getMaxStackSize(){return 1;}
        });
        // Player inventory is shifted down to make space for the rule editor.
        int playerX = 104;
        int playerY = 253;
        addStandardInventorySlots(inventory, playerX, playerY);
    }

    private static Container resolveClientContainer(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if(be instanceof PalletBlockEntity pallet)return pallet;
        if(be instanceof com.caszgamermd.caszualadditions.racks.RackBlockEntity rack)return rack;
        if(be instanceof com.caszgamermd.caszualadditions.racks.PtpBlockEntity ptp)return ptp;
        return new SimpleContainer(PalletBlockEntity.SLOTS);
    }

    public int page(){return page;}
    public int filterMode(){return mode;}
    public int filterScope(){return scope;}
    public String filterTarget(){return filter==null?"":filter.filterTarget();}
    public boolean hasFilter(){return filter!=null&&!filter.filterUpgrade().isEmpty();}
    public boolean isTransferPoint(){
        return backing instanceof com.caszgamermd.caszualadditions.racks.PtpBlockEntity;
    }
    public boolean ptpInput(){return ptpInputMode!=0;}

    public void applyFilterTarget(String raw) {
        if(!hasFilter())return;
        String value=raw.strip().toLowerCase(java.util.Locale.ROOT);
        if(!PalletFilterNet.validTarget(filter.filterScope(),value))return;
        filter.setFilterTarget(value);
    }
    private void sampleOffhand(Player player) {
        if(!hasFilter())return;
        ItemStack sample=player.getOffhandItem();
        if(sample.isEmpty())return;
        var id=BuiltInRegistries.ITEM.getKey(sample.getItem());
        int selected=filter.filterScope();
        if(selected==2)return; // type tag ID in the field instead
        filter.setFilterTarget(selected==1?id.getNamespace():id.toString());
        filter.setFilterComponentSignature(
                selected==3?sample.getComponents().toString():"");
    }

    @Override
    public boolean clickMenuButton(Player player,int id) {
        if(id==5 && backing instanceof com.caszgamermd.caszualadditions.racks.PtpBlockEntity point
                &&stillValid(player)){
            point.setInput(!point.input());
            ptpInputMode=point.input()?1:0;
            broadcastFullState();return true;
        }
        if(id==0||id==1){
            page=id;paged.setPage(page);pageData.set(page);broadcastFullState();return true;
        }
        if(id<2||id>4||!hasFilter()||!stillValid(player))return false;
        if(id==2)filter.setFilterMode(1-filter.filterMode());
        if(id==3){
            filter.setFilterScope((filter.filterScope()+1)%4);
            filter.setFilterTarget(""); // no stale ID from a different scope
            filter.setFilterComponentSignature("");
        }
        if(id==4)sampleOffhand(player);
        mode=filter.filterMode();scope=filter.filterScope();
        broadcastFullState();return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return backing.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (slotIndex < PAGE_SIZE || slotIndex == FILTER_SLOT) {
            if (!moveItemStackTo(stack, FILTER_SLOT+1, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            if(stack.is(RackContent.PALLET_FILTER)
                    && moveItemStackTo(stack,FILTER_SLOT,FILTER_SLOT+1,false)){
                // Filter module moved into the dedicated slot.
            } else {
                // Vanilla's merging pass can grow an already-occupied stack
                // without calling Slot.mayPlace. Check the backing policy
                // BEFORE any mutation, including merging into partial stacks.
                if(!paged.canPlaceItem(0,stack)
                        ||!moveItemStackTo(stack,0,PAGE_SIZE,false))
                    return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
