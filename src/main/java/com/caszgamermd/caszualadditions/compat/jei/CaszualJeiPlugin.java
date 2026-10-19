package com.caszgamermd.caszualadditions.compat.jei;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.CaszualTags;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import java.util.List;
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

    /**
     * The real wooden pallet crafting recipe is custom: it remembers which
     * four plank blocks are used and applies their textures to the pallet.
     * JEI cannot infer a 3x3 crafting layout from a CustomRecipe, so add a
     * display-only shaped recipe. It is NOT registered with Minecraft's
     * RecipeManager and cannot override the real NBT-preserving result.
     *
     * P C P
     * C C C   P = any #minecraft:planks, C = chest
     * P C P
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var preview=registration.getVanillaRecipeFactory()
                .createShapedRecipeBuilder(
                        CraftingBookCategory.MISC,
                        new SlotDisplay.ItemStackSlotDisplay(
                                new ItemStackTemplate(PalletContent.WOODEN_PALLET_ITEM)))
                .define('P',Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.PLANKS)))
                .define('C',Ingredient.of(Items.CHEST))
                .pattern("PCP")
                .pattern("CCC")
                .pattern("PCP")
                .group("caszual_additions_wooden_pallet")
                .build();
        registration.addRecipes(RecipeTypes.CRAFTING,
                List.of(new RecipeHolder<>(CaszualAdditions.id("wooden_pallet_jei_preview"),preview)));
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
