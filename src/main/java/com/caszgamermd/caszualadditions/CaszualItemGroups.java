package com.caszgamermd.caszualadditions;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CaszualItemGroups {
    public static final ResourceKey<CreativeModeTab> MAIN =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(CaszualAdditions.MOD_ID, "main"));

    private CaszualItemGroups() {}

    public static void register() {
        // Registration body will be finalized against the exact 26.2 mappings/API revision.
        // The intent is one shared Caszual Additions tab populated by this mod and companion mods.
    }

    public static CreativeModeTab.Builder builder() {
        return FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.caszual_additions.main"))
                .icon(() -> new ItemStack(Items.WATERMELON_SLICE));
    }
}
