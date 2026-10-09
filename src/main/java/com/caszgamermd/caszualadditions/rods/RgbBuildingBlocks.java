package com.caszgamermd.caszualadditions.rods;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class RgbBuildingBlocks {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

    public static Block RGB_BLOCK, RGB_SLAB, RGB_STAIRS, RGB_WALL, RGB_FENCE, RGB_DOOR, RGB_TRAPDOOR,
            RGB_FENCE_GATE, RGB_BARS, RGB_VERTICAL_STAIRS, RGB_VERTICAL_SLAB, RGB_QUARTER_BLOCK,
            RGB_CARPET, RGB_WALLPAPER;

    public static Item RGB_QUARTER_ITEM;

    private RgbBuildingBlocks() {}

    private static Block register(
            String name,
            Block template,
            Function<BlockBehaviour.Properties, Block> factory
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(ColorfulRods.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(template)
                .lightLevel(state -> 14)
                .setId(blockKey);

        if (name.contains("carpet") || name.equals("rgb_wallpaper")) {
            props.noOcclusion();
        }

        Block block = factory.apply(props);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())
        );

        BLOCKS.put(name, block);
        return block;
    }

    private static Block registerQuarter() {
        String name = "rgb_quarter_block";
        Identifier id = Identifier.fromNamespaceAndPath(ColorfulRods.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
                .lightLevel(state -> 14)
                .noOcclusion()
                .setId(blockKey);

        RGB_QUARTER_BLOCK = new RgbQuarterBlock(props);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, RGB_QUARTER_BLOCK);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        RGB_QUARTER_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new RgbQuarterItem(new Item.Properties().setId(itemKey))
        );

        BLOCKS.put(name, RGB_QUARTER_BLOCK);
        return RGB_QUARTER_BLOCK;
    }

    public static void initialize() {
        RGB_BLOCK = register("rgb_block", Blocks.AMETHYST_BLOCK, Block::new);
        RGB_SLAB = register("rgb_slab", Blocks.STONE_SLAB, SlabBlock::new);
        RGB_STAIRS = register("rgb_stairs", Blocks.STONE_STAIRS, p -> new StairBlock(RGB_BLOCK.defaultBlockState(), p));
        RGB_WALL = register("rgb_wall", Blocks.COBBLESTONE_WALL, WallBlock::new);
        RGB_FENCE = register("rgb_fence", Blocks.OAK_FENCE, FenceBlock::new);
        RGB_DOOR = register("rgb_door", Blocks.OAK_DOOR, p -> new DoorBlock(BlockSetType.COPPER, p));
        RGB_TRAPDOOR = register("rgb_trapdoor", Blocks.OAK_TRAPDOOR, p -> new TrapDoorBlock(BlockSetType.COPPER, p));
        RGB_FENCE_GATE = register("rgb_fence_gate", Blocks.OAK_FENCE_GATE, p -> new FenceGateBlock(WoodType.OAK, p));
        RGB_BARS = register("rgb_bars", Blocks.IRON_BARS, IronBarsBlock::new);
        RGB_VERTICAL_STAIRS = register("rgb_vertical_stairs", Blocks.STONE_STAIRS, RgbVerticalStairsBlock::new);
        RGB_VERTICAL_SLAB = register("rgb_vertical_slab", Blocks.STONE_SLAB, RgbVerticalSlabBlock::new);
        RGB_QUARTER_BLOCK = registerQuarter();
        RGB_CARPET = register("rgb_carpet", Blocks.AMETHYST_BLOCK, CarpetBlock::new);
        RGB_WALLPAPER = register("rgb_wallpaper", Blocks.AMETHYST_BLOCK, RgbWallpaperBlock::new);
    }
}
