package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Creative-only in-game regression fixture. The kit provides enough actual
 * blocks and workers for an INPUT PTP, OUTPUT PTP, four-Golem haul, Shulker
 * Tank packing, labeled aisles and filter tests. It never places blocks or
 * edits existing storage, so players retain control of the test layout.
 */
public final class WarehouseTestKitItem extends Item {
    public WarehouseTestKitItem(Properties properties){super(properties);}

    private static void add(List<ItemStack> items,Item item,int amount) {
        items.add(new ItemStack(item,amount));
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(!player.getAbilities().instabuild){
            player.sendSystemMessage(Component.literal(
                    "Warehouse Test Kit is Creative-only; switch to Creative to unpack."));
            return InteractionResult.SUCCESS;
        }
        List<ItemStack> supplies=new ArrayList<>();
        add(supplies,RackContent.RACK_ITEM,8);
        add(supplies,RackContent.COMPUTER_ITEM,2);
        add(supplies,RackContent.STORAGE_SHELF_ITEM,4);
        add(supplies,RackContent.STORAGE_GOLEM_ITEM,8);
        add(supplies,RackContent.PTP_TRANSFER_ITEM,32);
        add(supplies,RackContent.PTP_WRAP_ITEM,4);
        add(supplies,RackContent.PTP_HOPPER_ITEM,4);
        add(supplies,RackContent.STORAGE_SCANNER_ITEM,1);
        add(supplies,RackContent.TANK_ITEM,2);
        add(supplies,PalletContent.WOODEN_PALLET_ITEM,8);
        add(supplies,PalletContent.IRON_PALLET_ITEM,2);
        add(supplies,RackContent.PALLET_FILTER,8);
        add(supplies,RackContent.RACK_TAG,8);
        add(supplies,RackContent.AISLE_TAG,4);
        add(supplies,RackContent.CONNECTOR,16);
        add(supplies,RackContent.CONNECTOR,16);
        add(supplies,Items.SHULKER_BOX,8);
        add(supplies,Items.COBBLESTONE,64);
        add(supplies,Items.HOPPER,4);
        add(supplies,Items.SHEARS,1);
        add(supplies,Items.ANVIL,1);
        add(supplies,Items.PAPER,16);
        for(ItemStack stack:supplies)
            player.getInventory().placeItemBackInInventory(stack);
        player.sendSystemMessage(Component.literal(
                "Warehouse Test Kit unpacked: "+supplies.size()
                +" supply stacks. Extra stacks may be dropped if inventory is full."));
        player.sendSystemMessage(Component.literal(
                "Set up racks + terminal + shelves, then INPUT / OUTPUT PTPs. "
                +"Right-click a scanner with PAPER for a live readiness audit."));
        return InteractionResult.SUCCESS;
    }
}
