package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Saved-world compatibility shell for the retired 2x2 Order Collector Hub.
 *
 * Keeps the old 216-slot inventory, pallet/filter settings and collector count
 * readable. It intentionally exposes NO order queue, no virtual workers, no
 * dispatch, no new installation and no new inventory insertion. Existing
 * payload can be salvaged through the GUI or by breaking the old block.
 */
public final class OrderHubEntity extends BlockEntity implements Container,ExtendedMenuProvider<BlockPos>, com.caszgamermd.caszualadditions.pallet.PalletFilterCarrier {
    public static final int SLOTS=216;
    private NonNullList<ItemStack> output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private ItemStack pallet=ItemStack.EMPTY;
    private String palletFilter="";
    private ItemStack filterUpgrade=ItemStack.EMPTY;
    private int filterMode,filterScope;
    private String filterSignature="";
    private int collectors;
    public OrderHubEntity(BlockPos pos,BlockState state){super(RackContent.HUB_ENTITY,pos,state);}

    public boolean hasPallet(){return !pallet.isEmpty();}
    public ItemStack removePallet() {
        if(!isEmpty())return ItemStack.EMPTY;
        return dropPallet();
    }
    /** Breaking the hub separately drops its output inventory and pallet. */
    public ItemStack dropPallet() {
        if(!hasPallet())return ItemStack.EMPTY;
        ItemStack old=com.caszgamermd.caszualadditions.pallet.PalletData.applyFilterSettings(
                pallet,this);
        pallet=ItemStack.EMPTY;palletFilter="";filterUpgrade=ItemStack.EMPTY;
        filterSignature="";setChanged();return old;
    }
    public void removeCollector(){if(collectors>0){collectors--;setChanged();}}
    public int collectors(){return collectors;}
    public static @Nullable OrderHubEntity nextToComputer(ServerLevel level,BlockPos computer){
        for(Direction side:Direction.values()){
            BlockPos at=computer.relative(side);
            if(!level.hasChunkAt(at))continue;
            OrderHubEntity hub=OrderHubBlock.controller(level,at);
            if(hub!=null)return hub;
        }
        return null;
    }
    @Override public ItemStack filterUpgrade(){return filterUpgrade.copy();}
    @Override public void setFilterUpgrade(ItemStack item){
        filterUpgrade=item.is(com.caszgamermd.caszualadditions.racks.RackContent.PALLET_FILTER)
                ?item.copyWithCount(1):ItemStack.EMPTY;
        setChanged();
    }
    @Override public int filterMode(){return filterMode;}
    @Override public void setFilterMode(int mode){filterMode=Math.clamp(mode,0,1);setChanged();}
    @Override public int filterScope(){return filterScope;}
    @Override public void setFilterScope(int scope){filterScope=Math.clamp(scope,0,3);setChanged();}
    @Override public String filterTarget(){return palletFilter;}
    @Override public void setFilterTarget(String target){palletFilter=target;setChanged();}
    @Override public String filterComponentSignature(){return filterSignature;}
    @Override public void setFilterComponentSignature(String signature){filterSignature=signature;setChanged();}

    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){for(ItemStack i:output)if(!i.isEmpty())return false;return true;}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<SLOTS?output.get(slot):ItemStack.EMPTY;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        // Saved Hubs can be emptied, but no longer accept deliveries.
        return false;
    }
    @Override public ItemStack removeItem(int slot,int count){
        ItemStack r=ContainerHelper.removeItem(output,slot,count);
        if(!r.isEmpty())setChanged();return r;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        ItemStack r=ContainerHelper.takeItem(output,slot);
        if(!r.isEmpty())setChanged();return r;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS)return;
        // Permit extracting items through the old PalletMenu, but not adding
        // new material to a retired machine.
        ItemStack previous=output.get(slot);
        if(!stack.isEmpty()&&(previous.isEmpty()
                ||!ItemStack.isSameItemSameComponents(previous,stack)
                ||stack.getCount()>previous.getCount()))return;
        output.set(slot,stack);setChanged();
    }
    @Override public void clearContent(){output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player,6.0f);}
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output,this.output);
        if(hasPallet())output.store("output_pallet",ItemStack.CODEC,pallet);
        output.putInt("collectors",collectors);
        output.putString("pallet_filter",palletFilter);
        output.putInt("filter_mode",filterMode);
        output.putInt("filter_scope",filterScope);
        output.putString("filter_signature",filterSignature);
        if(!filterUpgrade.isEmpty())output.store("filter_upgrade",ItemStack.CODEC,filterUpgrade);

    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,output);
        pallet=input.read("output_pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        collectors=Math.clamp(input.getIntOr("collectors",0),0,3);
        palletFilter=input.getStringOr("pallet_filter","");
        filterMode=Math.clamp(input.getIntOr("filter_mode",0),0,1);
        filterScope=Math.clamp(input.getIntOr("filter_scope",0),0,3);
        filterSignature=input.getStringOr("filter_signature","");
        filterUpgrade=input.read("filter_upgrade",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if(filterUpgrade.isEmpty()&&!palletFilter.isBlank())
            filterUpgrade=new ItemStack(RackContent.PALLET_FILTER);

    }
    @Override public Component getDisplayName(){
        return Component.literal("Retired Warehouse Hub (salvage only)");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new PalletMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
}
