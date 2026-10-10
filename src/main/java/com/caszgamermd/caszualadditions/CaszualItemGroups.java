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
