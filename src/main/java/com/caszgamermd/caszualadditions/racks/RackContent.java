package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public final class RackContent {
    public static RackBlock RACK;
    public static Block RACK_COMPUTER;
    public static Item RACK_ITEM, COMPUTER_ITEM;
    public static Item RACK_TAG, AISLE_TAG, CONNECTOR, PALLET_FILTER;
    public static BlockEntityType<RackBlockEntity> RACK_ENTITY;

    private RackContent() {}
    private static Item item(String name) {
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new Item(new Item.Properties().stacksTo(64).setId(key)));
    }
    private static Item blockItem(String name, Block block) {
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new BlockItem(block,new Item.Properties().stacksTo(16).setId(key).useBlockDescriptionPrefix()));
    }
    public static void initialize() {
        Identifier id=CaszualAdditions.id("pallet_rack");
        ResourceKey<Block> key=ResourceKey.create(Registries.BLOCK,id);
        RACK=Registry.register(BuiltInRegistries.BLOCK,key,
                new RackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(4.0f).noOcclusion().setId(key)));
        RACK_ITEM=blockItem("pallet_rack",RACK);

        Identifier computerId=CaszualAdditions.id("rack_computer");
        ResourceKey<Block> computerKey=ResourceKey.create(Registries.BLOCK,computerId);
        RACK_COMPUTER=Registry.register(BuiltInRegistries.BLOCK,computerKey,
                new RackComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5f).setId(computerKey)));
        COMPUTER_ITEM=blockItem("rack_computer",RACK_COMPUTER);
        RACK_TAG=item("rack_tag");
        AISLE_TAG=item("aisle_tag");
        CONNECTOR=item("rack_connector");
        PALLET_FILTER=Registry.register(BuiltInRegistries.ITEM,
                ResourceKey.create(Registries.ITEM,CaszualAdditions.id("pallet_filter")),
                new PalletFilterItem(new Item.Properties().stacksTo(16)
                        .setId(ResourceKey.create(Registries.ITEM,CaszualAdditions.id("pallet_filter")))));
        RACK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("rack"),
                FabricBlockEntityTypeBuilder.create(RackBlockEntity::new,RACK).build());
        RackComputerNet.initialize();
    }
}
