package com.caszgamermd.caszualadditions;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.caszgamermd.caszualadditions.funbarrel.FunBarrelContent;
import com.caszgamermd.caszualadditions.antiwarden.AntiWardenContent;
import com.caszgamermd.caszualadditions.racks.RackContent;
import com.caszgamermd.caszualadditions.painting.IndustrialPaintContent;

public final class CaszualItemGroups {
    public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            CaszualAdditions.id("main")
    );

    private CaszualItemGroups() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                MAIN,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.caszual_additions.main"))
                        .icon(() -> new ItemStack(Items.MELON_SLICE))
                        .displayItems((parameters, output) -> {
                            // Several items are exposed both explicitly
                            // and through the shared Caszual content tag.
                            // Minecraft 26.2 throws on duplicate creative
                            // stacks, so every path must share ONE seen set.
                            java.util.Set<net.minecraft.world.item.Item> seen =
                                    new java.util.HashSet<>();
                            java.util.function.Consumer<net.minecraft.world.item.Item> add =
                                    item -> {
                                        if (seen.add(item))output.accept(new ItemStack(item));
                                    };
                            // No recipe, no survival acquisition: explicitly
                            // surface the sampler in the creative Caszual tab.
                            add.accept(FunBarrelContent.ITEM);
                            for (var item : java.util.List.of(RackContent.RACK_ITEM,
                                    RackContent.COMPUTER_ITEM, RackContent.STORAGE_SCANNER_ITEM,
                                    RackContent.WAREHOUSE_TEST_KIT,
                                    RackContent.RACK_STAIRS_ITEM,
                                    RackContent.RACK_LADDER_ITEM,
                                    RackContent.RACK_SCAFFOLDING_ITEM,
                                    RackContent.RACK_TAG,
                                    RackContent.AISLE_TAG,RackContent.CONNECTOR,
                                    RackContent.PALLET_FILTER,
                                    RackContent.TANK_ITEM,
                                    RackContent.PTP_TRANSFER_ITEM,RackContent.PTP_HOPPER_ITEM,
                                    RackContent.PTP_WRAP_ITEM,
                                    RackContent.STORAGE_SHELF_ITEM,RackContent.STORAGE_GOLEM_ITEM))
                                add.accept(item);
                            for(var paintItem:java.util.List.of(
                                    IndustrialPaintContent.PAINT_CAN_ITEM,
                                    IndustrialPaintContent.PRIMER_CAN_ITEM,
                                    IndustrialPaintContent.BRUSH,
                                    IndustrialPaintContent.REMOVER))add.accept(paintItem);
                            add.accept(RackContent.PORTABLE_WRAPPER);
                            add.accept(RackContent.STACK_OF_PALLETS);
                            add.accept(com.caszgamermd.caszualadditions.xp.XpBlocks.XP_COLLECTOR.asItem());
                            add.accept(AntiWardenContent.WOOL_HELMET);
                            add.accept(AntiWardenContent.WOOL_BOOTS);
                            add.accept(AntiWardenContent.WARDEN_TRAP.asItem());
                            add.accept(com.caszgamermd.caszualadditions.curtains.PlasticCurtainsContent.ITEM);
                            for(var blockItem:RackContent.INDUSTRIAL_CONCRETE_ITEMS.values())
                                add.accept(blockItem);
                            BuiltInRegistries.ITEM.forEach(item -> {
                                if (item == FunBarrelContent.ITEM)return;
                                ItemStack stack = item.getDefaultInstance();
                                if (stack.is(CaszualTags.CASZUAL_CONTENT)
                                        && seen.add(item)) output.accept(stack);
                            });
                        })
                        .build()
        );
    }
}
