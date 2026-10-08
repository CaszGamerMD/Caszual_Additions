package com.casz.colorfulrods;

import com.caszgamermd.caszualadditions.quarter.QuarterPlacement;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

public final class RgbQuarterItem extends Item implements FabricItem {
    public RgbQuarterItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return QuarterPlacement.place(context, RgbBuildingBlocks.RGB_BLOCK.defaultBlockState());
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ItemStack remainder = stack.copy();
        remainder.setCount(1);
        return ItemStackTemplate.fromNonEmptyStack(remainder);
    }
}
