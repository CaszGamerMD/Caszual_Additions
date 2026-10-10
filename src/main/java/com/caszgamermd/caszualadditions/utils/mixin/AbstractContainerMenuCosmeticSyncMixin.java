package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticSlotContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player inventory menus live for the player's entire session and can be
 * created before attachment data is loaded. Synchronize their cosmetic slots
 * from the authoritative attachments before Minecraft broadcasts any updates.
 * Slot changes are still saved in CosmeticSlotContainer.setChanged().
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuCosmeticSyncMixin {
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void caszual_additions$refreshCosmeticSlots(CallbackInfo ci) {
        if (!((Object)this instanceof InventoryMenu menu)) return;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof CosmeticSlotContainer container) {
                container.refreshFromOwner();
                break;
            }
        }
    }
}
