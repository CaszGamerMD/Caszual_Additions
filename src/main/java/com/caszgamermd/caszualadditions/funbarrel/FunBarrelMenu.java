package com.caszgamermd.caszualadditions.funbarrel;

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

/**
 * A fixed 9 x 6 set of client/server-synchronized slots is reused for
 * any number of pages. All page changes are validated by the server.
 */
public final class FunBarrelMenu extends AbstractContainerMenu {
    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int PAGE_SIZE = COLS * ROWS;

    private final Container backing;
    private final FunBarrelPageContainer view;
    private int page;
    private int totalPages;
    private int itemCount;

    public FunBarrelMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolveClient(inventory, pos));
    }

    public FunBarrelMenu(int containerId, Inventory inventory, Container backing) {
        super(FunBarrelContent.MENU, containerId);
        this.backing = backing;
        this.view = new FunBarrelPageContainer(backing);
        this.totalPages = Math.max(1, (backing.getContainerSize() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.itemCount = backing instanceof FunBarrelBlockEntity barrel ? barrel.catalogCount() : 0;

        addDataSlot(new DataSlot() {
            @Override public int get() { return page; }
            @Override public void set(int value) {
                page = Math.max(0, value);
                view.setPage(page);
            }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return totalPages; }
            @Override public void set(int value) { totalPages = Math.max(1, value); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return itemCount; }
            @Override public void set(int value) { itemCount = Math.max(0, value); }
        });

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new Slot(view, col + row * COLS, 8 + col * 18, 18 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 140);
    }

    private static Container resolveClient(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof FunBarrelBlockEntity barrel
                ? barrel : new SimpleContainer(PAGE_SIZE);
    }

    public int page() { return page; }
    public int pageCount() { return totalPages; }
    public int itemCount() { return itemCount; }

    @Override
    public boolean clickMenuButton(Player player, int index) {
        if (!player.getAbilities().instabuild || index < 0 || index >= totalPages) return false;
        page = index;
        view.setPage(page);
        broadcastFullState();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getAbilities().instabuild && backing.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= slots.size()) return result;
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        boolean success = slotIndex < PAGE_SIZE
                ? moveItemStackTo(stack, PAGE_SIZE, slots.size(), true)
                : moveItemStackTo(stack, 0, PAGE_SIZE, false);
        if (!success) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
