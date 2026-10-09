package com.caszgamermd.caszualadditions.utils.cosmetics;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
public final class FlowerCosmetics {
 private FlowerCosmetics(){}
 public static boolean isFlower(ItemStack stack){
  return stack.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().is(BlockTags.FLOWERS);
 }
}
