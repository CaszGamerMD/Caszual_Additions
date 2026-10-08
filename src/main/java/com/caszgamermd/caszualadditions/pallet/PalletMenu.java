package com.caszgamermd.caszualadditions.pallet;

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

public final class PalletMenu extends AbstractContainerMenu {
    public static final int COLS = 18;
    public static final int ROWS = 6;
    public static final int PAGE_SIZE = COLS * ROWS;

    private final Container backing;
    private final PalletPageContainer paged;
    private final DataSlot pageData;
    private int page;

    public PalletMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolveClientContainer(inventory, pos));
    }

    public PalletMenu(int containerId, Inventory inventory, Container backing) {
        super(PalletContent.PALLET_MENU, containerId);
        this.backing = backing;
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

        int startX = 8;
        int startY = 18;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                addSlot(new Slot(paged, x + y * COLS, startX + x * 18, startY + y * 18));
            }
        }

        int playerX = 89;
        int playerY = 140;
        addStandardInventorySlots(inventory, playerX, playerY);
    }

    private static Container resolveClientContainer(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof PalletBlockEntity pallet ? pallet : new SimpleContainer(PalletBlockEntity.SLOTS);
    }

    public int page() {
        return page;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 && id != 1) return false;
        page = id;
        paged.setPage(page);
        pageData.set(page);
        broadcastFullState();
        return true;
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

        if (slotIndex < PAGE_SIZE) {
            if (!moveItemStackTo(stack, PAGE_SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, PAGE_SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }
}
