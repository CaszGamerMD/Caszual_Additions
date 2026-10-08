package com.caszgamermd.caszualadditions.pallet;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class PalletData {
    public static final int DEFAULT_PLASTIC_COLOR = 0xffb060d8;

    private PalletData() {}

    public static ItemStack applyWood(ItemStack stack, List<Block> planks) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (int i = 0; i < 4; i++) {
            Block block = i < planks.size() ? planks.get(i) : Blocks.OAK_PLANKS;
            tag.putString("plank_" + i, BuiltInRegistries.BLOCK.getKey(block).toString());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ItemStack applyPlasticColor(ItemStack stack, int argb) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("plastic_color", argb);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static List<Block> wood(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<Block> out = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            String raw = tag.getStringOr("plank_" + i, "minecraft:oak_planks");
            Identifier id = Identifier.tryParse(raw);
            Block block = id == null ? Blocks.OAK_PLANKS : BuiltInRegistries.BLOCK.getValue(id);
            out.add(block == null ? Blocks.OAK_PLANKS : block);
        }
        return out;
    }

    public static int plasticColor(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr("plastic_color", DEFAULT_PLASTIC_COLOR);
    }
}
