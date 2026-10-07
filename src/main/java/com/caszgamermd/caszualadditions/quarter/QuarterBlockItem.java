package com.caszgamermd.caszualadditions.quarter;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The generic quarter block item doubles as the reusable template/cutter for
 * converting a full block into eight quarter blocks.
 */
public final class QuarterBlockItem extends BlockItem implements FabricItem {
    public QuarterBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        if (stack.isEmpty()) return null;

        ItemStack remainder = stack.copy();
        remainder.setCount(1);
        return ItemStackTemplate.fromNonEmptyStack(remainder);
    }
}
