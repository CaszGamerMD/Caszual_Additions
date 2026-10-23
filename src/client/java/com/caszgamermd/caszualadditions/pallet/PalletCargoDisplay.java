package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Shared decision for ground-pallet and rack cargo visuals.
 *
 * A BlockItem is not necessarily a full-size cube: signs, carpets, fences,
 * doors, stairs, slabs, and other modded partial blocks can protrude through
 * the neighboring cargo cells when rendered at full block scale. Only a
 * full-collision cube is rendered as an exposed item/block model. Everything
 * else is a regular 2/3-block cardboard carton with the item's GUI icon.
 *
 * This only affects rendering; the exact original ItemStack and its components
 * remain in the 216 inventory slots. Golem/forklift cargo is already boxed.
 */
public final class PalletCargoDisplay {
    private PalletCargoDisplay(){}

    public static boolean shouldBox(ItemStack stack,@Nullable BlockGetter level,
            BlockPos palletPos){
        if(!(stack.getItem() instanceof BlockItem blockItem)||level==null)
            return true;
        BlockState block=blockItem.getBlock().defaultBlockState();
        return !block.isCollisionShapeFullBlock(level,palletPos);
    }
}
