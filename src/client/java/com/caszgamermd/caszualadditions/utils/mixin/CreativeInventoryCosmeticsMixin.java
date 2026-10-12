package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticSlotContainer;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The standard InventoryMenu always has four additional cosmetic slots so
 * that cosmetics remain synced and usable when changing game modes.
 *
 * CreativeModeInventoryScreen copies InventoryMenu slots into its own tab.
 * Those copies must NOT be embedded in the creative inventory: its layout and
 * click routing only handle the vanilla inventory slots, and our former code
 * tried to find them using getContainerSlot() == 46..49. But that index is
 * LOCAL to the cosmetic container (0..3), not the index within InventoryMenu.
 *
 * Leave the server-backed slots untouched; remove only their temporary
 * creative-screen wrappers. Creative players can still open the dedicated
 * cosmetics screen via the armor-stand button on the inventory tab.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryCosmeticsMixin {
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void caszual_additions$keepCosmeticsOutOfCreativeTabs(
            CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;

        CreativeModeInventoryScreen screen =
                (CreativeModeInventoryScreen)(Object)this;
        screen.getMenu().slots.removeIf(slot ->
                slot.container instanceof CosmeticSlotContainer);
    }
}
