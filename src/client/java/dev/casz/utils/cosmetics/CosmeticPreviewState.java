package dev.casz.utils.cosmetics;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CosmeticPreviewState {
    private static Player player;
    private static ItemStack head = ItemStack.EMPTY;
    private static ItemStack chest = ItemStack.EMPTY;
    private static ItemStack legs = ItemStack.EMPTY;
    private static ItemStack feet = ItemStack.EMPTY;

    private CosmeticPreviewState() {}

    public static void begin(Player target, ItemStack h, ItemStack c, ItemStack l, ItemStack f) {
        player = target;
        head = h.copy();
        chest = c.copy();
        legs = l.copy();
        feet = f.copy();
    }

    public static void end() {
        player = null;
        head = chest = legs = feet = ItemStack.EMPTY;
    }

    public static boolean active(Player target) {
        return player == target;
    }

    public static ItemStack get(Player target, EquipmentSlot slot) {
        if (!active(target)) return Cosmetics.get(target, slot);
        return switch (slot) {
            case HEAD -> head;
            case CHEST -> chest;
            case LEGS -> legs;
            case FEET -> feet;
            default -> Cosmetics.get(target, slot);
        };
    }
}
