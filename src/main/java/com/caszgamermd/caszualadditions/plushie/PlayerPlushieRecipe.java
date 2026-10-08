package com.caszgamermd.caszualadditions.plushie;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class PlayerPlushieRecipe extends CustomRecipe {
    public static final PlayerPlushieRecipe INSTANCE = new PlayerPlushieRecipe();
    public static final MapCodec<PlayerPlushieRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPlushieRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PlayerPlushieRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private PlayerPlushieRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int wool = 0;
        int heads = 0;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.WOOL.white())) {
                wool++;
            } else if (stack.is(Items.PLAYER_HEAD)) {
                heads++;
            } else {
                return false;
            }
        }

        return wool == 8 && heads == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (stack.is(Items.PLAYER_HEAD)) {
                ResolvableProfile profile = stack.get(DataComponents.PROFILE);
                return PlayerPlushies.createStack(profile);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
