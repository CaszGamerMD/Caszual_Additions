package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persist the glass-panel wrap in the pallet item during PTP -> carried
 * crew -> rack or ground transfer, without storing duplicate slot contents. */
public final class GlassPalletWrap {
    private static final String KEY="caszual_glass_wrapped";
    private GlassPalletWrap(){}

    public static boolean wrapped(ItemStack pallet){
        if(pallet.isEmpty())return false;
        CustomData data=pallet.get(DataComponents.CUSTOM_DATA);
        return data!=null&&data.copyTag().getBooleanOr(KEY,false);
    }
    public static ItemStack withWrap(ItemStack pallet,boolean wrapped){
        if(pallet.isEmpty())return ItemStack.EMPTY;
        ItemStack updated=pallet.copy();
        CustomData old=updated.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=old==null?new CompoundTag():old.copyTag();
        tag.putBoolean(KEY,wrapped);
        updated.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return updated;
    }
}
