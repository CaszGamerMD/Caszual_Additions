package com.caszgamermd.caszualadditions.painting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class IndustrialBrushItem extends Item {
    public IndustrialBrushItem(Properties props){super(props);}
    public static ItemStack dipped(boolean primer,int rgb){
        ItemStack stack=new ItemStack(IndustrialPaintContent.BRUSH);
        CompoundTag tag=new CompoundTag();
        tag.putBoolean("industrial_primer",primer);
        tag.putInt("industrial_color",rgb&0xffffff);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        // CUSTOM_NAME is carried by the ItemStack, so inventory, tooltip,
        // hotbar and dropped-brush labels show the actual chemical/color.
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(primer
                ?"Industrial Primer Brush"
                :"Industrial Paint Brush #"+String.format("%06X",rgb&0xffffff)));
        return stack;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel world))
            return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player))
            return InteractionResult.FAIL;
        ItemStack stack=context.getItemInHand();
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);
        if(data==null){
            player.sendSystemMessage(Component.literal(
                    "Dip this brush into an Industrial Paint or Primer Can first."));
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag=data.copyTag();
        boolean primer=tag.getBooleanOr("industrial_primer",false);
        int color=tag.getIntOr("industrial_color",0xff5caade);
        String result=IndustrialFinishLogic.apply(world,context.getClickedPos(),primer,color);
        player.sendSystemMessage(Component.literal(result));
        return InteractionResult.SUCCESS;
    }
}