package com.caszgamermd.caszualadditions.plushie;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushies {
    public static Block PLAYER_PLUSHIE;
    public static Item PLAYER_PLUSHIE_ITEM;
    public static BlockEntityType<PlayerPlushieBlockEntity> PLAYER_PLUSHIE_ENTITY;
    public static RecipeSerializer<PlayerPlushieRecipe> PLAYER_PLUSHIE_RECIPE;

    private PlayerPlushies() {}

    public static void initialize() {
        var id = CaszualAdditions.id("player_plushie");
        var blockKey = ResourceKey.create(Registries.BLOCK, id);
        PLAYER_PLUSHIE = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new PlayerPlushieBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.white())
                        .strength(0.8F)
                        .noOcclusion()
                        .setId(blockKey))
        );

        var itemKey = ResourceKey.create(Registries.ITEM, id);
        PLAYER_PLUSHIE_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(PLAYER_PLUSHIE, new Item.Properties().setId(itemKey).stacksTo(1).useBlockDescriptionPrefix())
        );

        PLAYER_PLUSHIE_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id,
                FabricBlockEntityTypeBuilder.create(PlayerPlushieBlockEntity::new, PLAYER_PLUSHIE).build()
        );

        PLAYER_PLUSHIE_RECIPE = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                CaszualAdditions.id("player_plushie"),
                PlayerPlushieRecipe.SERIALIZER
        );
    }

    public static ItemStack createStack(@Nullable ResolvableProfile profile) {
        ItemStack stack = new ItemStack(PLAYER_PLUSHIE_ITEM);
        if (profile != null) {
            stack.set(DataComponents.PROFILE, profile);
            profile.name().ifPresent(name -> stack.set(DataComponents.ITEM_NAME, Component.literal(name + " Plushie")));
        }
        return stack;
    }
}
