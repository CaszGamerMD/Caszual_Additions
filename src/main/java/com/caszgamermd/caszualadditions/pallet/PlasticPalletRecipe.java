package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PlasticPalletRecipe extends CustomRecipe {
    public static final PlasticPalletRecipe INSTANCE = new PlasticPalletRecipe();
    public static final MapCodec<PlasticPalletRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlasticPalletRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PlasticPalletRecipe> SERIALIZER =
            new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private PlasticPalletRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) return false;

        boolean pallet = false;
        boolean dye = false;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;

            if (stack.is(PalletContent.PLASTIC_PALLET_ITEM)) {
                if (pallet) return false;
                pallet = true;
                continue;
            }

            if (stack.get(DataComponents.DYE) != null) {
                if (dye) return false;
                dye = true;
                continue;
            }

            return false;
        }

        return pallet && dye;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack pallet = ItemStack.EMPTY;
        DyeColor dye = null;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;

            if (stack.is(PalletContent.PLASTIC_PALLET_ITEM)) {
                pallet = stack;
            } else if (stack.get(DataComponents.DYE) != null) {
                dye = stack.get(DataComponents.DYE);
            }
        }

        if (pallet.isEmpty() || dye == null) return ItemStack.EMPTY;

        ItemStack result = pallet.copy();
        result.setCount(1);

        int color = dye.getTextureDiffuseColor();
        int argb = 0xff000000 | (color & 0x00ffffff);
        return PalletData.applyPlasticColor(result, argb);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
