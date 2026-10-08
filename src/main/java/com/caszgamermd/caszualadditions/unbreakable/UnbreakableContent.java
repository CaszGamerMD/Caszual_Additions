package com.caszgamermd.caszualadditions.unbreakable;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class UnbreakableContent {
    public static Block UNBREAKABLE_ANVIL;
    public static Item UNBREAKABLE_ANVIL_ITEM;
    public static Item UNBREAKABLE_BOOK;

    private UnbreakableContent() {}

    public static void initialize() {
        var anvilId = CaszualAdditions.id("unbreakable_anvil");
        var anvilBlockKey = ResourceKey.create(Registries.BLOCK, anvilId);
        UNBREAKABLE_ANVIL = Registry.register(
                BuiltInRegistries.BLOCK,
                anvilBlockKey,
                new AnvilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).setId(anvilBlockKey))
        );

        var anvilItemKey = ResourceKey.create(Registries.ITEM, anvilId);
        UNBREAKABLE_ANVIL_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                anvilItemKey,
                new BlockItem(UNBREAKABLE_ANVIL, new Item.Properties().setId(anvilItemKey).useBlockDescriptionPrefix())
        );

        var bookId = CaszualAdditions.id("unbreakable_book");
        var bookKey = ResourceKey.create(Registries.ITEM, bookId);
        UNBREAKABLE_BOOK = Registry.register(
                BuiltInRegistries.ITEM,
                bookKey,
                new Item(new Item.Properties()
                        .setId(bookKey)
                        .stacksTo(1)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
        );
    }
}
