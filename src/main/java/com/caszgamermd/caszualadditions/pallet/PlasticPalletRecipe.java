package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PlasticPalletRecipe extends CustomRecipe {
    public static final PlasticPalletRecipe INSTANCE = new PlasticPalletRecipe();
    public static final MapCodec<PlasticPalletRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlasticPalletRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PlasticPalletRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private PlasticPalletRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int purpur = 0;
        int dyes = 0;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.PURPUR_BLOCK)) {
                purpur++;
                continue;
            }
            if (stack.get(DataComponents.DYE) != null) {
                dyes++;
                continue;
            }
            return false;
        }

        return purpur == 4 && dyes >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        long red = 0;
        long green = 0;
        long blue = 0;
        int dyes = 0;

        for (ItemStack stack : input.items()) {
            DyeColor dye = stack.get(DataComponents.DYE);
            if (dye == null) continue;

            int color = dye.getTextureDiffuseColor();
            red += color >> 16 & 255;
            green += color >> 8 & 255;
            blue += color & 255;
            dyes++;
        }

        if (dyes == 0) return ItemStack.EMPTY;

        int argb = 0xff000000
                | ((int)(red / dyes) << 16)
                | ((int)(green / dyes) << 8)
                | (int)(blue / dyes);

        return PalletData.applyPlasticColor(new ItemStack(PalletContent.PLASTIC_PALLET_ITEM), argb);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
