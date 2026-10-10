package com.caszgamermd.caszualadditions.funbarrel;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A fixed 54-slot viewport over the barrel's dynamically sized inventory. */
public final class FunBarrelPageContainer implements Container {
    private final Container backing;
    private int page;

    public FunBarrelPageContainer(Container backing) {
        this.backing = backing;
    }

    public void setPage(int next) {
        page = Math.max(0, Math.min((backing.getContainerSize() - 1) / FunBarrelBlockEntity.PAGE_SIZE, next));
    }

    private int actualSlot(int index) {
        return page * FunBarrelBlockEntity.PAGE_SIZE + index;
    }

    @Override
    public int getContainerSize() {
        return FunBarrelBlockEntity.PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return backing.getItem(actualSlot(slot)); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return backing.removeItem(actualSlot(slot), count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return backing.removeItemNoUpdate(actualSlot(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        backing.setItem(actualSlot(slot), stack);
    }

    @Override
    public void setChanged() { backing.setChanged(); }

    @Override
    public boolean stillValid(Player player) {
        return backing.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            backing.setItem(actualSlot(i), ItemStack.EMPTY);
        }
        backing.setChanged();
    }
}
