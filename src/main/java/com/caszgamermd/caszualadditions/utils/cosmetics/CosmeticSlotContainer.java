package com.caszgamermd.caszualadditions.utils.cosmetics;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A cosmetic inventory is backed by player attachments, not by a one-time
 * snapshot taken in the player's InventoryMenu constructor.
 *
 * That constructor runs before persistent attachments have necessarily been
 * restored from player data. Without refresh, old snapshots can display empty
 * slots and a later slot click may erase otherwise valid cosmetics.
 */
public final class CosmeticSlotContainer extends SimpleContainer {
    private final Player owner;
    private boolean refreshing;
    private boolean initialized;

    public CosmeticSlotContainer(Player owner) {
        super(Cosmetics.SLOTS.length);
        this.owner = owner;
        refreshFromOwner();
        initialized = true;
    }

    /** Called before the server broadcasts inventory menu slot contents. */
    public void refreshFromOwner() {
        // The client menu's cursor state must not be overwritten by an
        // attachment sync that is still in flight. The server is authoritative.
        if (owner == null || (initialized && owner.level().isClientSide())) return;
        refreshing = true;
        try {
            for (int i = 0; i < Cosmetics.SLOTS.length; i++) {
                ItemStack actual = Cosmetics.get(owner, Cosmetics.SLOTS[i]);
                if (!ItemStack.matches(super.getItem(i), actual)) {
                    super.setItem(i, actual);
                }
            }
        } finally {
            refreshing = false;
        }
    }

    @Override public void setChanged() {
        super.setChanged();
        if (owner == null || refreshing || owner.level().isClientSide()) return;
        for (int i = 0; i < Cosmetics.SLOTS.length; i++) {
            Cosmetics.set(owner, Cosmetics.SLOTS[i], super.getItem(i));
        }
    }
}
