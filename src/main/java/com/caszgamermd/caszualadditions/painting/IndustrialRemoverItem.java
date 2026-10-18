package com.caszgamermd.caszualadditions.painting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class IndustrialRemoverItem extends Item {
    public IndustrialRemoverItem(Properties props){super(props);}
    @Override public InteractionResult useOn(UseOnContext ctx){
        if(!(ctx.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;
        if(ctx.getPlayer() instanceof ServerPlayer player)
            player.sendSystemMessage(Component.literal(
                    IndustrialFinishLogic.remove(level,ctx.getClickedPos())
                            ?"Paint and primer removed.":"This block has no industrial finish."));
        return InteractionResult.SUCCESS;
    }
}