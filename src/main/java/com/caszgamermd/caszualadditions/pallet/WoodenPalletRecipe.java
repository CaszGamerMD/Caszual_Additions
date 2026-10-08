package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class WoodenPalletRecipe extends CustomRecipe {
    public static final WoodenPalletRecipe INSTANCE = new WoodenPalletRecipe();
    public static final MapCodec<WoodenPalletRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WoodenPalletRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<WoodenPalletRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private WoodenPalletRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int planks = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (!stack.is(ItemTags.PLANKS) || !(stack.getItem() instanceof BlockItem)) return false;
            planks++;
        }
        return planks == 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<Block> planks = new ArrayList<>(4);
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                planks.add(blockItem.getBlock());
            }
        }
        if (planks.size() != 4) return ItemStack.EMPTY;
        return PalletData.applyWood(new ItemStack(PalletContent.WOODEN_PALLET_ITEM), planks);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
