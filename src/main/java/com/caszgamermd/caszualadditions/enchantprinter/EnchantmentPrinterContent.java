package com.caszgamermd.caszualadditions.enchantprinter;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class EnchantmentPrinterContent {
    public static EnchantmentPrinterBlock BLOCK;
    public static Item ITEM;
    public static BlockEntityType<EnchantmentPrinterBlockEntity> ENTITY;
    public static MenuType<EnchantmentPrinterMenu> MENU;
    private EnchantmentPrinterContent(){}
    public static void initialize(){
        PayloadTypeRegistry.clientboundPlay().register(
            EnchantmentPrinterStatePayload.TYPE,EnchantmentPrinterStatePayload.CODEC);
        Identifier id=CaszualAdditions.id("enchantment_printer");
        ResourceKey<Block> blockKey=ResourceKey.create(Registries.BLOCK,id);
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,blockKey,
            new EnchantmentPrinterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)
                .strength(3.5f).noOcclusion().setId(blockKey)));
        ResourceKey<Item> itemKey=ResourceKey.create(Registries.ITEM,id);
        ITEM=Registry.register(BuiltInRegistries.ITEM,itemKey,
            new BlockItem(BLOCK,new Item.Properties().stacksTo(1)
                .useBlockDescriptionPrefix().setId(itemKey)));
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,
            FabricBlockEntityTypeBuilder.create(EnchantmentPrinterBlockEntity::new,BLOCK).build());
        MENU=Registry.register(BuiltInRegistries.MENU,id,
            new ExtendedMenuType<>((menuId,inventory,pos)->
                new EnchantmentPrinterMenu(menuId,inventory,pos),BlockPos.STREAM_CODEC));
    }
}
