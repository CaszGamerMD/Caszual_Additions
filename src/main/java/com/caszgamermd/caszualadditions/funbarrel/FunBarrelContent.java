package com.caszgamermd.caszualadditions.funbarrel;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Creative-only sampler of every registered item in the Caszual mod family. */
public final class FunBarrelContent {
    public static FunBarrelBlock BLOCK;
    public static Item ITEM;
    public static BlockEntityType<FunBarrelBlockEntity> BLOCK_ENTITY;
    public static MenuType<FunBarrelMenu> MENU;

    private FunBarrelContent() {}

    public static void initialize() {
        Identifier id = CaszualAdditions.id("barrel_of_caszual_fun");
        ResourceKey<net.minecraft.world.level.block.Block> blockKey =
                ResourceKey.create(Registries.BLOCK, id);
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, blockKey,
                new FunBarrelBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                        .strength(2.5f)
                        .setId(blockKey)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(BLOCK, new Item.Properties()
                        .setId(itemKey).stacksTo(1).useBlockDescriptionPrefix()));

        BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(FunBarrelBlockEntity::new, BLOCK).build());

        MENU = Registry.register(BuiltInRegistries.MENU, id,
                new ExtendedMenuType<>(
                        (containerId, inventory, pos) -> new FunBarrelMenu(containerId, inventory, pos),
                        BlockPos.STREAM_CODEC));
    }
}
