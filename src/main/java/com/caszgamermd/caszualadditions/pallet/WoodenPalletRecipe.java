package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class WoodenPalletRecipe extends CustomRecipe {
    public static final WoodenPalletRecipe INSTANCE = new WoodenPalletRecipe();
    public static final MapCodec<WoodenPalletRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WoodenPalletRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<WoodenPalletRecipe> SERIALIZER =
            new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private WoodenPalletRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack stack = input.getItem(x, y);
                boolean corner = (x == 0 || x == 2) && (y == 0 || y == 2);

                if (corner) {
                    if (!stack.is(ItemTags.PLANKS) || !(stack.getItem() instanceof BlockItem)) {
                        return false;
                    }
                } else if (!stack.is(Items.CHEST)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return ItemStack.EMPTY;

        ItemStack nw = input.getItem(0, 0);
        ItemStack ne = input.getItem(2, 0);
        ItemStack sw = input.getItem(0, 2);
        ItemStack se = input.getItem(2, 2);

        if (!(nw.getItem() instanceof BlockItem nwBlock)
                || !(ne.getItem() instanceof BlockItem neBlock)
                || !(sw.getItem() instanceof BlockItem swBlock)
                || !(se.getItem() instanceof BlockItem seBlock)) {
            return ItemStack.EMPTY;
        }

        List<Block> planks = List.of(
                nwBlock.getBlock(),
                neBlock.getBlock(),
                swBlock.getBlock(),
                seBlock.getBlock()
        );

        return PalletData.applyWood(
                new ItemStack(PalletContent.WOODEN_PALLET_ITEM),
                planks
        );
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
