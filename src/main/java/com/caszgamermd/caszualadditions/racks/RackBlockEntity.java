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
public final class RackBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {
    public static final int CAPACITY = 216;
    private NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private ItemStack pallet = ItemStack.EMPTY;
    private String rackName = "";
    private String aisleName = "";
    private String filterItem = "";
    private int connectors;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(RackContent.RACK_ENTITY, pos, state);
    }

    public boolean hasPallet() { return !pallet.isEmpty(); }
    public void install(ItemStack item) {
        if (hasPallet()) return;
        pallet = item.copyWithCount(1);
        setChanged();
    }
    public ItemStack takePallet() {
        if (!isEmpty()) return ItemStack.EMPTY;
        ItemStack result = pallet;
        pallet = ItemStack.EMPTY;
        filterItem = "";
        setChanged();
        return result;
    }
    public String rackName() { return rackName; }
    public String aisleName() { return aisleName; }
    public void setRackName(String name) { rackName = trim(name); setChanged(); }
    public void setAisleName(String name) { aisleName = trim(name); setChanged(); }
    public String displayName() {
        return rackName.isBlank() ? "Rack " + worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ() : rackName;
    }
    private static String trim(String value) { return value == null ? "" : value.strip().substring(0,Math.min(48,value.strip().length())); }

    public void setFilter(ItemStack sample) {
        filterItem = BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();
        setChanged();
    }
    public String filter() { return filterItem; }
    public void clearFilter() { filterItem = ""; setChanged(); }
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
        return hasPallet() && (filterItem.isBlank()
            || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(filterItem));
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
        output.putString("filter_item",filterItem);
        output.putInt("connectors",connectors);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,items);
        pallet=input.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        rackName=input.getStringOr("rack_name","");
        aisleName=input.getStringOr("aisle_name","");
        filterItem=input.getStringOr("filter_item","");
        connectors=input.getIntOr("connectors",0);
    }
    @Override public Component getDisplayName(){return Component.literal(displayName());}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new PalletMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
}
