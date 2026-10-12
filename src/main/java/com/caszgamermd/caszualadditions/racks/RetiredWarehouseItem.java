package com.caszgamermd.caszualadditions.racks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Legacy item ID retained exclusively for safe upgrades of saved inventories.
 * Neither obsolete item is craftable or discoverable from the creative tab.
 *
 * The former Order Collector is exchangeable 1:1 for a Storage Golem; the
 * former 2x2 Hub is exchangeable for the nine transfer blocks of a PTP.
 * Existing block entities remain loadable separately for inventory recovery.
 */
public final class RetiredWarehouseItem extends Item {
    private final boolean oldHub;

    public RetiredWarehouseItem(Properties properties, boolean oldHub) {
        super(properties);
        this.oldHub=oldHub;
    }

    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(level.isClientSide())return InteractionResult.SUCCESS;
        ItemStack original=player.getItemInHand(hand);
        if(original.isEmpty())return InteractionResult.PASS;
        ItemStack replacement=new ItemStack(oldHub
                ?RackContent.PTP_TRANSFER_ITEM:RackContent.STORAGE_GOLEM_ITEM,
                oldHub?9:1);
        original.shrink(1);
        player.getInventory().placeItemBackInInventory(replacement);
        player.sendSystemMessage(Component.literal(oldHub
                ?"Retired Order Collector Hub exchanged for nine Pallet Transfer Blocks."
                :"Retired Order Collector exchanged for one Storage Golem."));
        return InteractionResult.SUCCESS;
    }
}
