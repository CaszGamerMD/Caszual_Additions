package com.caszgamermd.caszualadditions.quarter;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class QuarterTextureRecipe extends CustomRecipe {
    public static final QuarterTextureRecipe INSTANCE = new QuarterTextureRecipe();
    public static final MapCodec<QuarterTextureRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, QuarterTextureRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<QuarterTextureRecipe> SERIALIZER =
            new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private QuarterTextureRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        BlockItem source = null;
        boolean quarterTemplate = false;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;

            if (QuarterBlocks.isQuarterPiece(stack)) {
                if (quarterTemplate) return false;
                quarterTemplate = true;
                continue;
            }

            if (!(stack.getItem() instanceof BlockItem blockItem) || source != null) {
                return false;
            }

            source = blockItem;
        }

        return quarterTemplate && source != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty() || QuarterBlocks.isQuarterPiece(stack)) continue;

            if (stack.getItem() instanceof BlockItem blockItem) {
                return QuarterBlocks.pieceFor(blockItem.getBlock().defaultBlockState(), 8);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
