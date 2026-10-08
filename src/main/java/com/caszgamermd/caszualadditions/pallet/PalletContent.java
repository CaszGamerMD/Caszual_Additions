package com.caszgamermd.caszualadditions.pallet;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class PalletContent {
    public static PalletBlock WOODEN_PALLET;
    public static PalletBlock PLASTIC_PALLET;
    public static PalletBlock IRON_PALLET;
    public static PalletBlock COPPER_PALLET;
    public static PalletBlock EXPOSED_COPPER_PALLET;
    public static PalletBlock WEATHERED_COPPER_PALLET;
    public static PalletBlock OXIDIZED_COPPER_PALLET;

    public static Item WOODEN_PALLET_ITEM;
    public static Item PLASTIC_PALLET_ITEM;
    public static Item IRON_PALLET_ITEM;
    public static Item COPPER_PALLET_ITEM;
    public static Item EXPOSED_COPPER_PALLET_ITEM;
    public static Item WEATHERED_COPPER_PALLET_ITEM;
    public static Item OXIDIZED_COPPER_PALLET_ITEM;

    public static Block PALLET_RENDER_PROXY;
    public static BlockEntityType<PalletBlockEntity> PALLET_BLOCK_ENTITY;
    public static MenuType<PalletMenu> PALLET_MENU;
    public static RecipeSerializer<WoodenPalletRecipe> WOODEN_RECIPE;
    public static RecipeSerializer<PlasticPalletRecipe> PLASTIC_RECIPE;

    private PalletContent() {}

    public static void initialize() {
        WOODEN_PALLET = register("wooden_pallet", PalletKind.WOOD, 0, Blocks.OAK_PLANKS, false);
        PLASTIC_PALLET = register("plastic_pallet", PalletKind.PLASTIC, 0, Blocks.PURPUR_BLOCK, false);
        IRON_PALLET = register("iron_pallet", PalletKind.IRON, 0, Blocks.IRON_BLOCK, false);

        COPPER_PALLET = register("copper_pallet", PalletKind.COPPER, 0, Blocks.COPPER_BLOCK.unaffected(), true);
        EXPOSED_COPPER_PALLET = register("exposed_copper_pallet", PalletKind.COPPER, 1, Blocks.COPPER_BLOCK.exposed(), true);
        WEATHERED_COPPER_PALLET = register("weathered_copper_pallet", PalletKind.COPPER, 2, Blocks.COPPER_BLOCK.weathered(), true);
        OXIDIZED_COPPER_PALLET = register("oxidized_copper_pallet", PalletKind.COPPER, 3, Blocks.COPPER_BLOCK.oxidized(), false);

        WOODEN_PALLET_ITEM = WOODEN_PALLET.asItem();
        PLASTIC_PALLET_ITEM = PLASTIC_PALLET.asItem();
        IRON_PALLET_ITEM = IRON_PALLET.asItem();
        COPPER_PALLET_ITEM = COPPER_PALLET.asItem();
        EXPOSED_COPPER_PALLET_ITEM = EXPOSED_COPPER_PALLET.asItem();
        WEATHERED_COPPER_PALLET_ITEM = WEATHERED_COPPER_PALLET.asItem();
        OXIDIZED_COPPER_PALLET_ITEM = OXIDIZED_COPPER_PALLET.asItem();

        Identifier proxyId = CaszualAdditions.id("pallet_render_proxy");
        ResourceKey<Block> proxyKey = ResourceKey.create(Registries.BLOCK, proxyId);
        PALLET_RENDER_PROXY = Registry.register(
                BuiltInRegistries.BLOCK,
                proxyKey,
                new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white()).setId(proxyKey))
        );

        PALLET_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("pallet"),
                FabricBlockEntityTypeBuilder.create(
                        PalletBlockEntity::new,
                        WOODEN_PALLET,
                        PLASTIC_PALLET,
                        IRON_PALLET,
                        COPPER_PALLET,
                        EXPOSED_COPPER_PALLET,
                        WEATHERED_COPPER_PALLET,
                        OXIDIZED_COPPER_PALLET
                ).build()
        );

        PALLET_MENU = Registry.register(
                BuiltInRegistries.MENU,
                CaszualAdditions.id("pallet"),
                new ExtendedMenuType<>(
                        (containerId, inventory, pos) -> new PalletMenu(containerId, inventory, pos),
                        BlockPos.STREAM_CODEC
                )
        );

        WOODEN_RECIPE = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                CaszualAdditions.id("wooden_pallet"),
                WoodenPalletRecipe.SERIALIZER
        );
        PLASTIC_RECIPE = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                CaszualAdditions.id("plastic_pallet"),
                PlasticPalletRecipe.SERIALIZER
        );
    }

    private static PalletBlock register(
            String name,
            PalletKind kind,
            int age,
            Block template,
            boolean randomTicks
    ) {
        Identifier id = CaszualAdditions.id(name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(template)
                .strength(1.5F)
                .noOcclusion()
                .setId(blockKey);
        if (randomTicks) properties.randomTicks();

        PalletBlock block = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new PalletBlock(kind, age, properties)
        );

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).stacksTo(16).useBlockDescriptionPrefix())
        );
        return block;
    }

    public static void advanceCopper(ServerLevel level, BlockPos root, int age) {
        PalletBlock next = switch (age) {
            case 0 -> EXPOSED_COPPER_PALLET;
            case 1 -> WEATHERED_COPPER_PALLET;
            case 2 -> OXIDIZED_COPPER_PALLET;
            default -> null;
        };
        if (next == null) return;

        for (int part = 0; part < 4; part++) {
            BlockPos pos = PalletBlock.partPos(root, part);
            BlockState old = level.getBlockState(pos);
            if (!(old.getBlock() instanceof PalletBlock pallet) || pallet.kind() != PalletKind.COPPER) return;
        }

        for (int part = 0; part < 4; part++) {
            BlockPos pos = PalletBlock.partPos(root, part);
            level.setBlock(pos, next.defaultBlockState().setValue(PalletBlock.PART, part), 3);
        }
    }

    public static Block copperTextureBlock(int age) {
        return switch (age) {
            case 1 -> Blocks.COPPER_BLOCK.exposed();
            case 2 -> Blocks.COPPER_BLOCK.weathered();
            case 3 -> Blocks.COPPER_BLOCK.oxidized();
            default -> Blocks.COPPER_BLOCK.unaffected();
        };
    }

    public static List<PalletBlock> allPalletBlocks() {
        return List.of(
                WOODEN_PALLET,
                PLASTIC_PALLET,
                IRON_PALLET,
                COPPER_PALLET,
                EXPOSED_COPPER_PALLET,
                WEATHERED_COPPER_PALLET,
                OXIDIZED_COPPER_PALLET
        );
    }
}
