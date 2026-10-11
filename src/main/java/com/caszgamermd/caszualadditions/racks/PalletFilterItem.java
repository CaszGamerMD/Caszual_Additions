package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class PalletFilterItem extends Item {
    public PalletFilterItem(Properties properties){super(properties);}

    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();
        var player=context.getPlayer();
        if(player==null) return InteractionResult.PASS;
        var state=level.getBlockState(context.getClickedPos());
        if(!(state.getBlock() instanceof PalletBlock))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        var pos=PalletBlock.rootPos(context.getClickedPos(),state);
        if(!(level.getBlockEntity(pos) instanceof PalletBlockEntity pallet))
            return InteractionResult.FAIL;
        ItemStack sample=player.getOffhandItem();
        if(sample.isEmpty() || sample.is(this)){
            pallet.clearFilter();
            player.displayClientMessage(Component.literal("Pallet filter removed."),true);
        } else {
            pallet.setFilter(sample);
            player.displayClientMessage(Component.literal(
                    "Pallet accepts only " + sample.getHoverName().getString()),true);
        }
        if(!player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
