package com.caszgamermd.caszualadditions.quarter;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class QuarterBlocks {
    public static final String SOURCE_KEY = "source_block";
    public static Block QUARTER_BLOCK;
    public static Item QUARTER_BLOCK_ITEM;
    public static BlockEntityType<GenericQuarterBlockEntity> QUARTER_BLOCK_ENTITY;
    public static RecipeSerializer<QuarterTextureRecipe> QUARTER_TEXTURE_RECIPE;

    private QuarterBlocks() {}

    public static void initialize() {
        var blockId = CaszualAdditions.id("quarter_block");
        var blockKey = ResourceKey.create(Registries.BLOCK, blockId);
        QUARTER_BLOCK = new GenericQuarterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                .noOcclusion().strength(1.5f).setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, QUARTER_BLOCK);

        var itemKey = ResourceKey.create(Registries.ITEM, blockId);
        QUARTER_BLOCK_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new QuarterBlockItem(QUARTER_BLOCK, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

        QUARTER_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("quarter_block"),
                FabricBlockEntityTypeBuilder.create(GenericQuarterBlockEntity::new, QUARTER_BLOCK).build()
        );

        QUARTER_TEXTURE_RECIPE = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                CaszualAdditions.id("quarter_block_texture"),
                QuarterTextureRecipe.SERIALIZER
        );
    }

    public static ItemStack textured(BlockState source, int count) {
        ItemStack stack = new ItemStack(QUARTER_BLOCK_ITEM, count);
        CompoundTag tag = new CompoundTag();
        tag.putString(SOURCE_KEY, BuiltInRegistries.BLOCK.getKey(source.getBlock()).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable(source.getBlock().getDescriptionId()).append(Component.literal(" Quarter Block")));
        return stack;
    }

    public static BlockState source(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return Blocks.STONE.defaultBlockState();
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String raw = tag.getStringOr(SOURCE_KEY, "minecraft:stone");
        Identifier id = Identifier.tryParse(raw);
        Block block = id == null ? Blocks.STONE : BuiltInRegistries.BLOCK.getValue(id);
        return block == null ? Blocks.STONE.defaultBlockState() : block.defaultBlockState();
    }
}
