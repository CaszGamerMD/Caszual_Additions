package com.caszgamermd.caszualadditions.funbarrel;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One stack of one item per registered Caszual-series item ID.
 * Inventory length is derived from the registry, never hard-coded to a
 * chest/barrel size. Saved items and removed items persist across restarts.
 * Added companion mods cause new entries to be appended on next opening.
 */
public final class FunBarrelBlockEntity extends BlockEntity
        implements Container, ExtendedMenuProvider<BlockPos> {
    public static final int PAGE_SIZE = 54;
    public static final int MAX_CAPACITY = 16_384;

    private NonNullList<ItemStack> items = NonNullList.withSize(PAGE_SIZE, ItemStack.EMPTY);
    private final Set<String> cataloged = new LinkedHashSet<>();
    private boolean initialized;

    public FunBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(FunBarrelContent.BLOCK_ENTITY, pos, state);
    }

    private static boolean caszualFamily(Identifier id) {
        String namespace = id.getNamespace();
        return namespace.startsWith("caszual_")
                || namespace.equals("prettyfrogs")
                || namespace.equals("linked_aquariums")
                || namespace.equals("caszutils")
                || namespace.equals("colorful_rods");
    }

    /**
     * Add only item IDs not previously encountered. This is intentional:
     * taking an item out does not cause infinite restocks each time the
     * player opens the barrel. Replacing the barrel creates a new sampler.
     */
    public void refreshCatalog() {
        if (level == null || level.isClientSide()) return;

        List<Item> registered = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (item != Items.AIR && id != null && caszualFamily(id)) {
                registered.add(item);
            }
        });
        registered.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));

        boolean changed = !initialized;
        initialized = true;
        for (Item item : registered) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (cataloged.contains(id)) continue;
            if (cataloged.size() >= MAX_CAPACITY) break;

            ItemStack stack = item.getDefaultInstance();
            if (stack.isEmpty()) continue;
            stack.setCount(1);
            int target = firstEmptySlot();
            if (target == -1) {
                if (items.size() >= MAX_CAPACITY) break;
                grow(items.size() + PAGE_SIZE);
                target = firstEmptySlot();
            }
            items.set(target, stack);
            cataloged.add(id);
            changed = true;
        }
        if (changed) setChanged();
    }

    private int firstEmptySlot() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private void grow(int capacity) {
        int newCapacity = Math.min(MAX_CAPACITY, Math.max(PAGE_SIZE, capacity));
        if (newCapacity <= items.size()) return;
        NonNullList<ItemStack> expanded = NonNullList.withSize(newCapacity, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) expanded.set(i, items.get(i));
        items = expanded;
    }

    public int catalogCount() { return cataloged.size(); }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getAbilities().instabuild
                && Container.stillValidBlockEntity(this, player, 6.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("capacity", items.size());
        output.putInt("initialized", initialized ? 1 : 0);
        output.store("cataloged", Codec.STRING.listOf(), List.copyOf(cataloged));
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int capacity = Math.clamp(input.getIntOr("capacity", PAGE_SIZE), PAGE_SIZE, MAX_CAPACITY);
        // Always preserve a whole number of visible pages.
        capacity = Math.min(MAX_CAPACITY, ((capacity + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE);
        items = NonNullList.withSize(capacity, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        initialized = input.getIntOr("initialized", 0) != 0;
        cataloged.clear();
        cataloged.addAll(input.read("cataloged", Codec.STRING.listOf()).orElse(List.of()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.caszual_additions.barrel_of_caszual_fun");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (!player.getAbilities().instabuild) return null;
        return new FunBarrelMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
