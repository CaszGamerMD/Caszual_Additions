package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticSlotPreferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The extra slots live just left of the vanilla inventory bounds.
 * Without this exemption Minecraft classifies their clicks as OUTSIDE (-999)
 * and drops whatever the cursor is carrying instead of picking up the item.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookCosmeticClickMixin {
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void caszual_additions$expandedCosmeticsCountAsInside(
            double mouseX, double mouseY, int left, int top,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object)this instanceof InventoryScreen)) return;
        if (!CosmeticSlotPreferences.expanded(Minecraft.getInstance())) return;
        // Same bounds as the 4 added cosmetic slots plus their visible panel border.
        if (mouseX >= left - 22 && mouseX < left
                && mouseY >= top + 6 && mouseY < top + 80) {
            cir.setReturnValue(false);
        }
    }
}
