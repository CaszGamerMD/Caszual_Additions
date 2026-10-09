package com.caszgamermd.caszualadditions.utils.cosmetics;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SkullBlock;

public final class SpecialCosmetics {
    private SpecialCosmetics() {}
    /** Avoid a hard cross-mod dependency; aquarium item is registered by Linked Aquariums. */
    public static boolean isWearableAquarium(ItemStack stack) {
        return !stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
            .equals(net.minecraft.resources.Identifier.fromNamespaceAndPath("linked_aquariums", "wearable_aquarium"));
    }
    public static boolean isAquariumFishBucket(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.COD_BUCKET)
            || stack.is(net.minecraft.world.item.Items.SALMON_BUCKET)
            || stack.is(net.minecraft.world.item.Items.TROPICAL_FISH_BUCKET)
            || stack.is(net.minecraft.world.item.Items.PUFFERFISH_BUCKET);
    }
    public static boolean isBone(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.BONE); }
    public static boolean isRodLike(ItemStack stack) { return isEndRod(stack) || isBone(stack); }
    public static boolean isEndRod(ItemStack stack) { return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EndRodBlock; }
    public static boolean isPlayerHead(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock skull && skull.getType() == SkullBlock.Types.PLAYER;
    }
    public static boolean hasBlockHead(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
    public static boolean isCrystalCluster(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        if (item.getBlock() instanceof net.minecraft.world.level.block.AmethystClusterBlock) return true;
        if (stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("caszual_additions", "crystal_cluster_cosmetics")))) return true;
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(item.getBlock()).getPath();
        return name.contains("cluster") && (name.contains("crystal") || name.contains("amethyst"));
    }
    public static boolean isSnowGolem(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        return head.is(net.minecraft.world.item.Items.CARVED_PUMPKIN) && chest.is(net.minecraft.world.item.Items.STICK)
            && legs.is(net.minecraft.world.item.Items.SNOW_BLOCK) && feet.is(net.minecraft.world.item.Items.SNOW_BLOCK);
    }
    public static boolean rodLegs(ItemStack legs, ItemStack feet) { return isEndRod(legs) || isEndRod(feet); }
    public static boolean specialLegs(ItemStack legs, ItemStack feet) { return rodLegs(legs, feet) || isBone(legs) || isBone(feet); }
    public static boolean isMobHead(ItemStack stack) { return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock && !isPlayerHead(stack); }
}