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
import com.caszgamermd.caszualadditions.racks.RackContent;

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
                            // No recipe, no survival acquisition: explicitly
                            // surface the sampler in the creative Caszual tab.
                            output.accept(new ItemStack(FunBarrelContent.ITEM));
                            for (var item : java.util.List.of(RackContent.RACK_ITEM,
                                    RackContent.COMPUTER_ITEM, RackContent.STORAGE_SCANNER_ITEM,
                                    RackContent.RACK_TAG,
                                    RackContent.AISLE_TAG,RackContent.CONNECTOR,
                                    RackContent.PALLET_FILTER,RackContent.HUB_ITEM,
                                    RackContent.TANK_ITEM,RackContent.ORDER_COLLECTOR,
                                    RackContent.PTP_TRANSFER_ITEM,RackContent.PTP_HOPPER_ITEM,
                                    RackContent.PTP_WRAP_ITEM,
                                    RackContent.STORAGE_SHELF_ITEM,RackContent.STORAGE_GOLEM_ITEM))
                                output.accept(new ItemStack(item));
                            BuiltInRegistries.ITEM.forEach(item -> {
                                if (item == FunBarrelContent.ITEM) return;
                                ItemStack stack = item.getDefaultInstance();
                                if (stack.is(CaszualTags.CASZUAL_CONTENT)) output.accept(stack);
                            });
                        })
                        .build()
        );
    }
}
