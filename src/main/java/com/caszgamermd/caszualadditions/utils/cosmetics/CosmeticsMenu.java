package com.caszgamermd.caszualadditions.utils.cosmetics;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CosmeticsMenu extends AbstractContainerMenu {
    private final Player owner;
    public CosmeticsMenu(int id, Inventory inventory) {
        super(Cosmetics.MENU, id);
        owner = inventory.player;
        class CosmeticContainer extends SimpleContainer {
            private boolean initialized;
            CosmeticContainer() {
                super(4);
                for (int i = 0; i < 4; i++) setItem(i, Cosmetics.get(owner, Cosmetics.SLOTS[i]));
                initialized = true;
            }
            @Override public void setChanged() {
                super.setChanged();
                if (initialized && !owner.level().isClientSide()) for (int i = 0; i < 4; i++) Cosmetics.set(owner, Cosmetics.SLOTS[i], getItem(i));
            }
        }
        var cosmetics = new CosmeticContainer();
        String[] icons = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            addSlot(new Slot(cosmetics, i, 8, 18 + 18 * i) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return Cosmetics.accepts(stack, Cosmetics.SLOTS[index]); }
                @Override public Identifier getNoItemIcon() { return Identifier.withDefaultNamespace("container/slot/" + icons[index]); }
            });
        }
        addStandardInventorySlots(inventory, 8, 110);
    }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, slots.size(), false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < 4; i++) if (!slots.get(i).hasItem() && slots.get(i).mayPlace(stack)) {
                moved = moveItemStackTo(stack, i, i + 1, false); if (moved) break;
            }
            if (!moved && !(index < 31 ? moveItemStackTo(stack, 31, 40, false) : moveItemStackTo(stack, 4, 31, false))) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack); return original;
    }
}