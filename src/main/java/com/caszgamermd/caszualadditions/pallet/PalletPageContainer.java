package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PalletPageContainer implements Container {
    public static final int PAGE_SIZE = 108;

    private final Container backing;
    private int page;

    public PalletPageContainer(Container backing) {
        this.backing = backing;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(1, page));
    }

    public int page() {
        return page;
    }

    private int actual(int slot) {
        return page * PAGE_SIZE + slot;
    }

    @Override
    public int getContainerSize() {
        return PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return backing.getItem(actual(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return backing.removeItem(actual(slot), count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return backing.removeItemNoUpdate(actual(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        backing.setItem(actual(slot), stack);
    }

    @Override
    public void setChanged() {
        backing.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return backing.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            backing.setItem(actual(i), ItemStack.EMPTY);
        }
        backing.setChanged();
    }
}
