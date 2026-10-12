package com.caszgamermd.caszualadditions.racks;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Server-only packing helpers. The box, including its color and custom name,
 * remains a real ItemStack; no item is materialized until a worker takes it. */
public final class StorageGolemPacking {
    private StorageGolemPacking() {}

    public static ItemStack fill(ItemStack emptyBox,List<ItemStack> stacks) {
        if(!ShulkerTankEntity.isEmptyShulker(emptyBox)||stacks.isEmpty()
                ||stacks.size()>27)return ItemStack.EMPTY;
        for(ItemStack stack:stacks)
            if(stack.isEmpty()||!stack.getItem().canFitInsideContainerItems())
                return ItemStack.EMPTY;
        ItemStack filled=emptyBox.copyWithCount(1);
        filled.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(stacks));
        return filled;
    }
    public static int itemCount(ItemStack box) {
        ItemContainerContents contents=box.get(DataComponents.CONTAINER);
        if(contents==null)return 0;
        return contents.nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum();
    }
    public static List<ItemStack> unpack(ItemStack box) {
        ItemContainerContents contents=box.get(DataComponents.CONTAINER);
        return contents==null?List.of():contents.nonEmptyItemCopyStream().toList();
    }
    public static ItemStack emptyVersion(ItemStack filled) {
        ItemStack empty=filled.copyWithCount(1);
        empty.remove(DataComponents.CONTAINER);
        return empty;
    }
}
