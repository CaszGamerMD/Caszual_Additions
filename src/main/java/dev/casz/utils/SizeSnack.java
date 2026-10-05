package dev.casz.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SizeSnack extends Item {
    public enum Direction { SHRINK, GROW, RESET }
    private final Direction direction;
    public SizeSnack(Properties properties, Direction direction) { super(properties); this.direction = direction; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide() && eater instanceof Player player) {
            var scale = player.getAttribute(Attributes.SCALE);
            if (scale != null) {
                double target = direction == Direction.RESET ? 1.0 : SizeStages.next(scale.getBaseValue(), direction == Direction.GROW);
                scale.setBaseValue(target);
                player.sendOverlayMessage(Component.translatable("message.caszutils.scale", target));
            }
        }
        return super.finishUsingItem(stack, level, eater);
    }
}