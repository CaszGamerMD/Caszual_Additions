package com.caszgamermd.caszualadditions.utils.cosmetics;
import net.minecraft.world.item.ItemStack;
public final class AppearanceRules {
    private AppearanceRules() {}
    public static ItemStack visible(ItemStack cosmetic, ItemStack normal, boolean hide) {
        return !cosmetic.isEmpty() ? cosmetic : hide ? ItemStack.EMPTY : normal;
    }
}