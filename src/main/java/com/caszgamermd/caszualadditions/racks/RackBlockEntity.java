package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import java.util.ArrayList;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Persistent rack state; only part=0 owns a block entity and inventory. */
public final class RackBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos>, com.caszgamermd.caszualadditions.pallet.PalletFilterCarrier {
    public static final int CAPACITY = 216;
    private NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private ItemStack pallet = ItemStack.EMPTY;
    private String rackName = "";
    private String aisleName = "";
    // These are physical signs on the FRONT of a rack, rather than names
    // stored only in the computer interface. Default to legacy names on load.
    private boolean rackPlate, aislePlate;
    private String filterItem = "";
    private ItemStack filterUpgrade = ItemStack.EMPTY;
    private int filterMode,filterScope;
    private String filterSignature="";
    private int connectors;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(RackContent.RACK_ENTITY, pos, state);
    }

    public boolean hasPallet() { return !pallet.isEmpty(); }
    public ItemStack palletStack(){return pallet.copy();}
    public void install(ItemStack item) {
        if (hasPallet()) return;
        pallet = item.copyWithCount(1);
        filterItem=com.caszgamermd.caszualadditions.pallet.PalletData.filter(item);
        filterMode=com.caszgamermd.caszualadditions.pallet.PalletData.filterMode(item);
        filterScope=com.caszgamermd.caszualadditions.pallet.PalletData.filterScope(item);
        filterSignature=com.caszgamermd.caszualadditions.pallet.PalletData.filterSignature(item);
        filterUpgrade=com.caszgamermd.caszualadditions.pallet.PalletData.filterInstalled(item)
                ?new ItemStack(RackContent.PALLET_FILTER):ItemStack.EMPTY;
        setChanged();
    }
    public ItemStack takePallet() {
        if (!isEmpty()) return ItemStack.EMPTY;
        return dropPallet();
    }
    /** Explicit break path: inventory is separately dropped by the rack. */
    public ItemStack dropPallet() {
        if(!hasPallet())return ItemStack.EMPTY;
        ItemStack result = com.caszgamermd.caszualadditions.pallet.PalletData.applyFilterSettings(
                pallet,this);
        pallet = ItemStack.EMPTY;
        filterItem = "";
        filterUpgrade=ItemStack.EMPTY;
        filterSignature="";
        setChanged();
        return result;
    }
    public String rackName() { return rackName; }
    public String aisleName() { return aisleName; }
    public void setRackName(String name) {
        rackName=trim(name);rackPlate=true;setChanged();
    }
    public void setAisleName(String name) {
        aisleName=trim(name);aislePlate=true;setChanged();
    }
    public boolean hasRackPlate(){return rackPlate;}
    public boolean hasAislePlate(){return aislePlate;}
    public void clearRackPlate(){rackPlate=false;rackName="";setChanged();}
    public void clearAislePlate(){aislePlate=false;aisleName="";setChanged();}
    public String displayName() {
        return rackName.isBlank() ? "Rack " + worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ() : rackName;
    }
    private static String trim(String value) { return value == null ? "" : value.strip().substring(0,Math.min(48,value.strip().length())); }

    public void setFilter(ItemStack sample) {
        if(!hasPallet())return;
        filterItem=BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();
        filterScope=0;filterMode=0;filterSignature="";
        if(filterUpgrade.isEmpty())filterUpgrade=new ItemStack(RackContent.PALLET_FILTER);
        setChanged();
    }
    public String filter() {return filterItem;}
    public void clearFilter(){setFilterTarget("");}
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
    @Override public String filterTarget(){return filterItem;}
    @Override public void setFilterTarget(String target){filterItem=target;setChanged();}
    @Override public String filterComponentSignature(){return filterSignature;}
    @Override public void setFilterComponentSignature(String signature){filterSignature=signature;setChanged();}

    public boolean hasConnector(Direction side) { return (connectors & (1 << side.get3DDataValue())) != 0; }
    public void toggleConnector(Direction side) { connectors ^= 1 << side.get3DDataValue(); setChanged(); }
    public Direction facing() { return getBlockState().getValue(RackBlock.FACING); }

    public int occupiedSlots() {
        int used=0;
        for (ItemStack item:items) if (!item.isEmpty()) used++;
        return used;
    }
    public long totalItems() {
        long count=0;
        for (ItemStack item:items) count += item.getCount();
        return count;
    }

    @Override public int getContainerSize() { return CAPACITY; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return slot>=0&&slot<CAPACITY ? items.get(slot):ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int count) {
        ItemStack take=ContainerHelper.removeItem(items,slot,count);
        if(!take.isEmpty())setChanged();
        return take;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack take=ContainerHelper.takeItem(items,slot);
        if(!take.isEmpty())setChanged();
        return take;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        return hasPallet() && allows(stack);
    }
    @Override public void setItem(int slot,ItemStack stack) {
        if(slot<0||slot>=CAPACITY)return;
        // Extraction is always permitted, but insertion obeys the pallet filter.
        if(!stack.isEmpty() && !canPlaceItem(slot,stack))return;
        items.set(slot,stack);
        if(stack.getCount()>stack.getMaxStackSize())stack.setCount(stack.getMaxStackSize());
        setChanged();
    }
    @Override public void clearContent() { items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);setChanged(); }
    @Override public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this,player,6.0F);
    }
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null){BlockState state=getBlockState();level.sendBlockUpdated(worldPosition,state,state,3);}
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output,items);
        if(!pallet.isEmpty()) output.store("pallet",ItemStack.CODEC,pallet);
        output.putString("rack_name",rackName);
        output.putString("aisle_name",aisleName);
        output.putBoolean("rack_plate",rackPlate);
        output.putBoolean("aisle_plate",aislePlate);
        output.putString("filter_item",filterItem);
        output.putInt("filter_mode",filterMode);
        output.putInt("filter_scope",filterScope);
        output.putString("filter_signature",filterSignature);
        if(!filterUpgrade.isEmpty())output.store("filter_upgrade",ItemStack.CODEC,filterUpgrade);
        output.putInt("connectors",connectors);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,items);
        pallet=input.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        rackName=input.getStringOr("rack_name","");
        aisleName=input.getStringOr("aisle_name","");
        // Existing rack names from pre-plate builds automatically appear as
        // physical plates after update, without requiring players to retag.
        rackPlate=input.getBooleanOr("rack_plate",!rackName.isBlank());
        aislePlate=input.getBooleanOr("aisle_plate",!aisleName.isBlank());
        filterItem=input.getStringOr("filter_item","");
        filterMode=Math.clamp(input.getIntOr("filter_mode",0),0,1);
        filterScope=Math.clamp(input.getIntOr("filter_scope",0),0,3);
        filterSignature=input.getStringOr("filter_signature","");
        filterUpgrade=input.read("filter_upgrade",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if(filterUpgrade.isEmpty()&&!filterItem.isBlank())
            filterUpgrade=new ItemStack(RackContent.PALLET_FILTER);
        connectors=input.getIntOr("connectors",0);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){
        return saveWithoutMetadata(provider);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public Component getDisplayName(){return Component.literal(displayName());}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new PalletMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
}
