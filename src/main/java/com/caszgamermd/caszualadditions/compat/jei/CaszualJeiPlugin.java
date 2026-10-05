package com.caszgamermd.caszualadditions.compat.jei;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.CaszualTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IIngredientAliasRegistration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class CaszualJeiPlugin implements IModPlugin {
    private static final Identifier UID = CaszualAdditions.id("jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = item.getDefaultInstance();
            if (stack.is(CaszualTags.CASZUAL_CONTENT)) {
                registration.addAlias(stack, "Caszual Additions");
                registration.addAlias(stack, "Caszual");
            }
        });
    }
}
