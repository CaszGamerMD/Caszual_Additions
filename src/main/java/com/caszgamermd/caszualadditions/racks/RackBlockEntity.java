package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import java.util.ArrayList;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import com.caszgamermd.caszualadditions.pallet.GlassPalletWrap;
import com.caszgamermd.caszualadditions.pallet.PalletData;
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
    // Rack tags are physical front plates; aisle labels are inherited
    // throughout a row and rendered on its exposed side ends.
    // Preserve existing sign flags and names on world load.
    private boolean rackPlate, aislePlate;
    private String filterItem = "";
    private ItemStack filterUpgrade = ItemStack.EMPTY;
    private int filterMode,filterScope;
    private String filterSignature="";
    private int connectors;
    // Visual-only two-worker extraction; rack retains the contents until
    // the leader accepts all 216 slots after four workers assemble.
    private String pullOwner="";
    private long pullStartTick,pullLastTick;
    private int pullFloorY;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(RackContent.RACK_ENTITY, pos, state);
    }

    public boolean hasPallet() { return !pallet.isEmpty(); }

    /** Reserve the pallet for ONE player's two-worker extraction animation.
     * Stale reservations expire so interrupted jobs cannot lock a rack. */
    public boolean startPull(java.util.UUID owner,int floorY){
        if(level==null||level.isClientSide()||!hasPallet()||!glassWrapped())return false;
        if(!WarehouseRackAccess.clearOfStairs((net.minecraft.server.level.ServerLevel)level,this)
                ||!WarehouseRackAccess.prepare((net.minecraft.server.level.ServerLevel)level,this))
            return false;
        long now=level.getGameTime();
        String id=owner.toString();
        if(!pullOwner.isBlank()&&now-pullLastTick>200)resetPull();
        if(!pullOwner.isBlank()&&!pullOwner.equals(id))return false;
        if(pullOwner.isBlank()){
            pullOwner=id;
            pullStartTick=now;
            pullFloorY=floorY;
            setChanged();
        }
        pullLastTick=now;
        return true;
    }
    public boolean pulling(java.util.UUID owner){return pullOwner.equals(owner.toString());}
    public int pullProgress(){
        if(pullOwner.isBlank()||level==null)return 0;
        return (int)Math.clamp(level.getGameTime()-pullStartTick,0,RackPickupPlan.PULL_TICKS);
    }
    public int pullFloorY(){return pullFloorY;}
    public void cancelPull(java.util.UUID owner){
        if(pulling(owner)){
            resetPull();
            if(level instanceof net.minecraft.server.level.ServerLevel world)
                WarehouseRackAccess.release(world,this);
        }
    }
    private void resetPull(){
        if(pullOwner.isBlank())return;
        pullOwner="";
        pullStartTick=0;
        pullLastTick=0;
        setChanged();
    }
    public ItemStack palletStack(){return pallet.copy();}
    public boolean glassWrapped(){
        return com.caszgamermd.caszualadditions.pallet.GlassPalletWrap.wrapped(pallet);
    }
    public void setGlassWrapped(boolean wrap){
        if(pallet.isEmpty())return;
        pallet=com.caszgamermd.caszualadditions.pallet.GlassPalletWrap.withWrap(pallet,wrap);
        setChanged();
    }
    public void unwrapGlass(){
        if(glassWrapped()){
            pallet=com.caszgamermd.caszualadditions.pallet.GlassPalletWrap.withWrap(pallet,false);
            setChanged();
        }
    }
    public void install(ItemStack item) {
        if (hasPallet()||item.isEmpty()||!isEmpty()) return;
        // Creative Ctrl+Pick Block stores the ORIGINAL 216 slot positions
        // in the pallet ItemStack's CONTAINER component. Placing that item
        // in a rack must import its cargo exactly like placing it on ground.
        // Without this, the rack UI appears empty while the deck hides its
        // own copy of the inventory (and an eventual rack break can dupe it).
        if(level instanceof net.minecraft.server.level.ServerLevel world
                &&(!WarehouseRackAccess.clearOfStairs(world,this)
                    ||!WarehouseRackAccess.prepare(world,this)))return;
        NonNullList<ItemStack> carried=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        item.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY)
                .copyInto(carried);
        resetPull();
        items=carried;
        pallet=item.copyWithCount(1);
        pallet.remove(DataComponents.CONTAINER); // ONLY the rack now owns cargo
        filterItem=PalletData.filter(item);
        filterMode=PalletData.filterMode(item);
        filterScope=PalletData.filterScope(item);
        filterSignature=PalletData.filterSignature(item);
        filterUpgrade=PalletData.filterInstalled(item)
                ?new ItemStack(RackContent.PALLET_FILTER):ItemStack.EMPTY;
        setChanged();
        if(level instanceof net.minecraft.server.level.ServerLevel world)
            WarehouseRackAccess.release(world,this);
    }
    /**
     * Transfer the entire installed pallet, its filter and all 216 slots to
     * ONE Golem leader. The rack is cleared only after loadPallet succeeds,
     * so a failed pickup never loses items or duplicates a pallet.
     * Invoked on the server after all four assigned workers arrive.
     */
    public boolean liftToCarrier(StorageGolem leader) {
        if (leader == null || !hasPallet() || !glassWrapped() || leader.holdsPallet()
                ||pullProgress()<RackPickupPlan.PULL_TICKS)return false;
        if(level instanceof net.minecraft.server.level.ServerLevel world
                &&(!WarehouseRackAccess.clearOfStairs(world,this)
                    ||!WarehouseRackAccess.prepare(world,this)))return false;
        ItemStack deck = com.caszgamermd.caszualadditions.pallet.GlassPalletWrap.withWrap(
                com.caszgamermd.caszualadditions.pallet.PalletData.applyFilterSettings(pallet,this),
                true);
        if (deck.isEmpty()) return false;
        NonNullList<ItemStack> stock=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        for (int i=0;i<CAPACITY;i++)stock.set(i,items.get(i).copy());
        if (!leader.loadPallet(deck,stock))return false;

        // From this point on only the Golem holds the inventory.
        items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        pallet=ItemStack.EMPTY;
        filterItem="";
        filterUpgrade=ItemStack.EMPTY;
        filterSignature="";
        filterMode=0;
        filterScope=0;
        resetPull();
        setChanged();
        if(level instanceof net.minecraft.server.level.ServerLevel world)
            WarehouseRackAccess.release(world,this);
        return true;
    }

    /**
     * Manual rack-to-Jack transfer, preserving the complete pallet deck,
     * filter settings, wrapping and all 216 cargo slot positions.
     *
     * A Golem currently extracting this rack owns its pallet: the Jack may
     * not interrupt an active pull. Reuse the same physical rack-access
     * validation as a Golem's pickup, including occupied stairs/scaffolds.
     *
     * The Jack acquires the payload BEFORE the rack removes its copy.
     * Neither ordinary takePallet() nor dropPallet() can transfer full cargo.
     */
    public boolean liftToJack(ItemStack jack){return liftToTransport(jack,false);}
    public boolean liftToForklift(ItemStack jack){return liftToTransport(jack,true);}
    private boolean liftToTransport(ItemStack jack,boolean forklift){
        if(!(level instanceof net.minecraft.server.level.ServerLevel world)
                ||!jack.is(RackContent.PALLET_JACK)
                ||PalletJackItem.loaded(jack)||!hasPallet()||!glassWrapped())return false;
        if(!pullOwner.isBlank()&&world.getGameTime()-pullLastTick<=200)return false;
        if((forklift?RackPickupPlan.forklift(world,this):RackPickupPlan.find(world,this))==null)
            return false;
        try{
            // Golem pull could have completed during a prior tick; do not
            // extract a rack reserved by another carrier.
            if(!pullOwner.isBlank()&&world.getGameTime()-pullLastTick<=200)return false;
            ItemStack deck=GlassPalletWrap.withWrap(
                    PalletData.applyFilterSettings(pallet,this),true);
            if(deck.isEmpty())return false;
            NonNullList<ItemStack> packed=NonNullList.withSize(217,ItemStack.EMPTY);
            packed.set(0,deck.copyWithCount(1));
            for(int i=0;i<CAPACITY;i++)packed.set(i+1,items.get(i).copy());
            jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed));

            items=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
            pallet=ItemStack.EMPTY;
            filterItem="";
            filterUpgrade=ItemStack.EMPTY;
            filterSignature="";
            filterMode=0;
            filterScope=0;
            resetPull();
            setChanged();
            return true;
        }finally{
            WarehouseRackAccess.release(world,this);
        }
    }

    /** Restore the same pallet directly into another vacant rack. Only
     * clear the Jack AFTER the new rack owns every original cargo slot.
     * The restored stacks are already part of this pallet and must not be
     * rejected merely because its saved filter currently excludes them.
     */
    public boolean installFromJack(ItemStack jack){return installFromTransport(jack,false);}
    public boolean installFromForklift(ItemStack jack){return installFromTransport(jack,true);}
    private boolean installFromTransport(ItemStack jack,boolean forklift){
        if(!(level instanceof net.minecraft.server.level.ServerLevel world)
                ||!PalletJackItem.loaded(jack)||hasPallet()||!isEmpty())return false;
        NonNullList<ItemStack> packed=PalletJackItem.unpack(jack);
        ItemStack deck=packed.get(0);
        if(deck.isEmpty()||!GlassPalletWrap.wrapped(deck))return false;
        if((forklift?RackPickupPlan.forklift(world,this):RackPickupPlan.find(world,this))==null
                ||!WarehouseRackAccess.clearOfStairs(world,this)
                ||!WarehouseRackAccess.prepare(world,this))return false;
        try{
            if(hasPallet()||!isEmpty())return false;
            install(deck.copyWithCount(1));
            if(!hasPallet())return false;
            for(int i=0;i<CAPACITY;i++)items.set(i,packed.get(i+1).copy());
            setChanged();
            jack.remove(DataComponents.CONTAINER);
            return true;
        }finally{
            WarehouseRackAccess.release(world,this);
        }
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
        resetPull();
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
        if(!pullOwner.isBlank()){
            output.putString("pull_owner",pullOwner);
            output.putLong("pull_start",pullStartTick);
            output.putLong("pull_last",pullLastTick);
            output.putInt("pull_floor",pullFloorY);
        }
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
        pullOwner=input.getStringOr("pull_owner","");
        pullStartTick=input.getLongOr("pull_start",0);
        pullLastTick=input.getLongOr("pull_last",0);
        pullFloorY=input.getIntOr("pull_floor",worldPosition.getY());
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
