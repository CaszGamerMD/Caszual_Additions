package com.caszgamermd.caszualadditions.pallet;

import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PalletBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos>, PalletFilterCarrier {
    public static final int SLOTS = 216;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private BlockPos rootPos;
    private int part;
    private final Block[] wood = {
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS
    };
    private int plasticColor = PalletData.DEFAULT_PLASTIC_COLOR;
    private boolean glassWrapped;
    private String filterItem = "";
    private ItemStack filterUpgrade = ItemStack.EMPTY;
    private int filterMode, filterScope;
    private String filterSignature = "";

    public void setFilter(ItemStack sample) {
        PalletBlockEntity c=controller();
        c.filterItem=BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();
        c.filterScope=0;c.filterMode=0;c.filterSignature="";
        if(c.filterUpgrade.isEmpty())c.filterUpgrade=new ItemStack(
                com.caszgamermd.caszualadditions.racks.RackContent.PALLET_FILTER);
        c.sync();
    }
    public void clearFilter(){setFilterTarget("");}
    public String filter(){return controller().filterItem;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return controller().allows(stack);}

    @Override public ItemStack filterUpgrade(){return filterUpgrade.copy();}
    @Override public void setFilterUpgrade(ItemStack item){
        filterUpgrade=item.is(com.caszgamermd.caszualadditions.racks.RackContent.PALLET_FILTER)
                ?item.copyWithCount(1):ItemStack.EMPTY;
        controller().sync();
    }
    @Override public int filterMode(){return filterMode;}
    @Override public void setFilterMode(int mode){filterMode=Math.clamp(mode,0,1);controller().sync();}
    @Override public int filterScope(){return filterScope;}
    @Override public void setFilterScope(int scope){filterScope=Math.clamp(scope,0,3);controller().sync();}
    @Override public String filterTarget(){return controller().filterItem;}
    @Override public void setFilterTarget(String target){controller().filterItem=target;controller().sync();}
    @Override public String filterComponentSignature(){return filterSignature;}
    @Override public void setFilterComponentSignature(String signature){filterSignature=signature;controller().sync();}
    public PalletBlockEntity(BlockPos pos, BlockState state) {
        super(PalletContent.PALLET_BLOCK_ENTITY, pos, state);
        this.part = state.hasProperty(PalletBlock.PART) ? state.getValue(PalletBlock.PART) : 0;
        this.rootPos = PalletBlock.rootPos(pos, state);
    }

    public boolean isRoot() {
        return part == 0;
    }

    public int part() {
        return part;
    }

    public BlockPos rootPos() {
        return rootPos;
    }

    public int plasticColor() {
        return plasticColor;
    }
    public boolean glassWrapped(){return controller().glassWrapped;}
    public void setGlassWrapped(boolean wrapped){
        PalletBlockEntity root=controller();
        if(!root.isRoot())return;
        if(root.glassWrapped!=wrapped){
            root.glassWrapped=wrapped;
            root.sync();
        }
    }
    public void unwrapGlass(){
        PalletBlockEntity root=controller();
        if(root.glassWrapped){root.glassWrapped=false;root.sync();}
    }

    public Block woodForBoard(int board) {
        return wood[Math.max(0, Math.min(3, board))];
    }

    public Block woodForPart() {
        return wood[Math.max(0, Math.min(3, part))];
    }

    public void initialize(BlockPos root, int part, ItemStack stack) {
        this.rootPos = root.immutable();
        this.part = part;
        // A carrier killed where there is no safe 2x2 ground area drops
        // ONE intact packed pallet. Rehydrate the exact same 216 slot
        // indices when that item is placed; otherwise empty slots would
        // shift and the stored cargo would appear lost.
        if(isRoot()){
            items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
            stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY)
                    .copyInto(items);
        }
        this.filterItem=PalletData.filter(stack);
        if(isRoot())glassWrapped=GlassPalletWrap.wrapped(stack);
        if(isRoot()){
            filterScope=PalletData.filterScope(stack);
            filterMode=PalletData.filterMode(stack);
            filterSignature=PalletData.filterSignature(stack);
            filterUpgrade=PalletData.filterInstalled(stack)
                    ?new ItemStack(com.caszgamermd.caszualadditions.racks.RackContent.PALLET_FILTER)
                    :ItemStack.EMPTY;
        }

        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            if (pallet.kind() == PalletKind.WOOD) {
                List<Block> materials = PalletData.wood(stack);
                for (int i = 0; i < 4; i++) {
                    wood[i] = materials.get(i);
                }
            } else if (pallet.kind() == PalletKind.PLASTIC) {
                plasticColor = PalletData.plasticColor(stack);
            }
        }

        sync();
    }

    /** Atomic cargo restoration for a player's Pallet Jack. Restoring 216
     * slots individually would broadcast hundreds of block updates and
     * rebuild upper cargo collisions after EVERY stack.
     */
    public void restoreJackCargo(NonNullList<ItemStack> packed){
        if(!isRoot()||packed.size()!=SLOTS)
            throw new IllegalArgumentException("Pallet jack cargo must be 216 slots");
        PalletBlockEntity root=controller();
        for(int i=0;i<SLOTS;i++)
            root.items.set(i,packed.get(i).copy());
        root.sync();
    }

    /** Preserve all slot indices of a loaded carrier in ONE pallet item.
     * The CONTAINER component is read on placement by initialize(). */
    public static ItemStack packedCarrierDrop(ItemStack pallet,
            NonNullList<ItemStack> savedContents){
        ItemStack packed=pallet.copyWithCount(1);
        if(packed.isEmpty())return ItemStack.EMPTY;
        NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        for(int i=0;i<SLOTS&&i<savedContents.size();i++)
            items.set(i,savedContents.get(i).copy());
        packed.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));
        return packed;
    }

    public ItemStack createDropStack() {
        ItemStack stack = new ItemStack(getBlockState().getBlock().asItem());
        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            if (pallet.kind() == PalletKind.WOOD) {
                stack = PalletData.applyWood(stack, List.of(wood));
            } else if (pallet.kind() == PalletKind.PLASTIC) {
                stack = PalletData.applyPlasticColor(stack, plasticColor);
            }
        }
        return GlassPalletWrap.withWrap(
                PalletData.applyFilterSettings(stack,controller()),glassWrapped());
    }

    private PalletBlockEntity controller() {
        if (level == null || isRoot()) return this;
        return level.getBlockEntity(rootPos) instanceof PalletBlockEntity be ? be : this;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        PalletBlockEntity c = controller();
        for (ItemStack stack : c.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        PalletBlockEntity c = controller();
        return slot >= 0 && slot < SLOTS ? c.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        PalletBlockEntity c = controller();
        ItemStack removed = ContainerHelper.removeItem(c.items, slot, count);
        if (!removed.isEmpty()) c.sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        PalletBlockEntity c = controller();
        ItemStack removed = ContainerHelper.takeItem(c.items, slot);
        if (!removed.isEmpty()) c.sync();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        PalletBlockEntity c = controller();
        if (slot < 0 || slot >= SLOTS) return;

        if(!stack.isEmpty() && !c.canPlaceItem(slot,stack))return;
        c.items.set(slot, stack);
        if (stack.getCount() > c.getMaxStackSize(stack)) {
            stack.setCount(c.getMaxStackSize(stack));
        }
        c.sync();
    }

    @Override
    public void clearContent() {
        PalletBlockEntity c = controller();
        for (int i = 0; i < SLOTS; i++) {
            c.items.set(i, ItemStack.EMPTY);
        }
        c.sync();
    }

    @Override
    public boolean stillValid(Player player) {
        PalletBlockEntity root=controller();
        if(root.isRemoved()||root.getLevel()==null||player.level()!=root.getLevel())
            return false;
        BlockState state=root.getBlockState();
        if(!(state.getBlock() instanceof PalletBlock)
                ||state.getValue(PalletBlock.PART)!=0)return false;
        // Vanilla's container check uses the controller's one block center.
        // A free-standing pallet is 2x2, so players opening a FAR corner
        // should not immediately lose their menu. Check all four cells.
        for(int part=0;part<4;part++){
            BlockPos cell=PalletBlock.partPos(root.getBlockPos(),part,state);
            if(player.distanceToSqr(cell.getX()+.5,cell.getY()+.5,
                    cell.getZ()+.5)<=64.0D)return true;
        }
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isRoot() && level instanceof ServerLevel server) {
            PalletCargoCollisionBlock.refresh(server, worldPosition);
        }
        if (isRoot() && level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void sync() {
        super.setChanged();
        if (isRoot() && level instanceof ServerLevel server) {
            PalletCargoCollisionBlock.refresh(server, worldPosition);
        }
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("root", BlockPos.CODEC, rootPos);
        output.putInt("part", part);
        output.putInt("plastic_color", plasticColor);
        output.putBoolean("glass_wrapped",glassWrapped);
        output.putString("filter_item",filterItem);
        output.putInt("filter_mode",filterMode);
        output.putInt("filter_scope",filterScope);
        output.putString("filter_signature",filterSignature);
        if(!filterUpgrade.isEmpty())output.store("filter_upgrade",ItemStack.CODEC,filterUpgrade);

        for (int i = 0; i < 4; i++) {
            output.putString(
                    "plank_" + i,
                    BuiltInRegistries.BLOCK.getKey(wood[i]).toString()
            );
        }

        if (isRoot()) {
            ContainerHelper.saveAllItems(output, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rootPos = input.read("root", BlockPos.CODEC).orElse(worldPosition);
        part = input.getIntOr(
                "part",
                getBlockState().hasProperty(PalletBlock.PART)
                        ? getBlockState().getValue(PalletBlock.PART)
                        : 0
        );
        plasticColor = input.getIntOr("plastic_color", PalletData.DEFAULT_PLASTIC_COLOR);
        glassWrapped=input.getBooleanOr("glass_wrapped",false);
        filterItem=input.getStringOr("filter_item","");
        filterMode=Math.clamp(input.getIntOr("filter_mode",0),0,1);
        filterScope=Math.clamp(input.getIntOr("filter_scope",0),0,3);
        filterSignature=input.getStringOr("filter_signature","");
        filterUpgrade=input.read("filter_upgrade",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if(filterUpgrade.isEmpty() && !filterItem.isBlank())
            filterUpgrade=new ItemStack(com.caszgamermd.caszualadditions.racks.RackContent.PALLET_FILTER);

        for (int i = 0; i < 4; i++) {
            Identifier id = Identifier.tryParse(
                    input.getStringOr("plank_" + i, "minecraft:oak_planks")
            );
            Block block = id == null
                    ? Blocks.OAK_PLANKS
                    : BuiltInRegistries.BLOCK.getValue(id);
            wood[i] = block == null ? Blocks.OAK_PLANKS : block;
        }

        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        if (isRoot()) {
            ContainerHelper.loadAllItems(input, items);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            return switch (pallet.kind()) {
                case WOOD -> Component.translatable("container.caszual_additions.wooden_pallet");
                case PLASTIC -> Component.translatable("container.caszual_additions.plastic_pallet");
                case IRON -> Component.translatable("container.caszual_additions.iron_pallet");
                case COPPER -> Component.translatable("container.caszual_additions.copper_pallet");
            };
        }
        return Component.translatable("container.caszual_additions.pallet");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        PalletBlockEntity c = controller();
        return new PalletMenu(containerId, inventory, c);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return controller().getBlockPos();
    }
}
