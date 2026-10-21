package com.caszgamermd.caszualadditions.autovending;

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

/** Single shared registry for the physical craft-to-order vending machine. */
public final class CraftVendingContent {
    public static CraftVendingBlock BLOCK;
    public static Item ITEM;
    public static BlockEntityType<CraftVendingBlockEntity> ENTITY;
    public static MenuType<CraftVendingMenu> MENU;
    private CraftVendingContent(){}
    public static void initialize(){
        Identifier id=CaszualAdditions.id("crafting_vending_machine");
        ResourceKey<net.minecraft.world.level.block.Block> blockKey=
                ResourceKey.create(Registries.BLOCK,id);
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,blockKey,
                new CraftVendingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5F).requiresCorrectToolForDrops()
                        .setId(blockKey)));
        ResourceKey<Item> itemKey=ResourceKey.create(Registries.ITEM,id);
        ITEM=Registry.register(BuiltInRegistries.ITEM,itemKey,
                new BlockItem(BLOCK,new Item.Properties().stacksTo(16)
                        .useBlockDescriptionPrefix().setId(itemKey)));
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,
                FabricBlockEntityTypeBuilder.create(CraftVendingBlockEntity::new,BLOCK).build());
        MENU=Registry.register(BuiltInRegistries.MENU,id,
                new ExtendedMenuType<>(
                        (menuId,inventory,pos)->new CraftVendingMenu(menuId,inventory,pos),
                        BlockPos.STREAM_CODEC));
    }
}
