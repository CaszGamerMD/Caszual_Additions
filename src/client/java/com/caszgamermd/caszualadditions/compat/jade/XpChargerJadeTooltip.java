package com.caszgamermd.caszualadditions.compat.jade;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade overlay code lives only in Fabric's CLIENT source set.
 * Minecraft client-only UI interfaces cannot be loaded from common sources.
 */
public final class XpChargerJadeTooltip implements IBlockComponentProvider {
    private static final Identifier UID=CaszualAdditions.id("xp_charger_repair");

        INSTANCE;
        private static final String PREFIX="caszual_xp_charger_";

        @Override public Identifier getUid(){return UID;}

        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,
                IPluginConfig config){
            CompoundTag data=accessor.getServerData();
            if(!data.getBooleanOr(PREFIX+"present",false))return;

            int stored=data.getIntOr(PREFIX+"stored",0);
            int capacity=data.getIntOr(PREFIX+"capacity",0);
            if(!data.getBooleanOr(PREFIX+"item",false)){
                tooltip.add(Component.translatable("jade.caszual_additions.xp_charger.empty")
                        .withStyle(ChatFormatting.GRAY));
            }else{
                String name=data.getStringOr(PREFIX+"name","");
                int max=Math.max(0,data.getIntOr(PREFIX+"max",0));
                int damage=Math.clamp(data.getIntOr(PREFIX+"damage",0),0,max);
                int remaining=max-damage;
                int percent=max==0?100:Math.round(100f*remaining/max);

                tooltip.add(Component.translatable(
                        "jade.caszual_additions.xp_charger.item",name));
                tooltip.add(Component.translatable(
                        "jade.caszual_additions.xp_charger.durability",
                        remaining,max,percent).withStyle(ChatFormatting.GRAY));

                if(damage==0){
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.complete")
                            .withStyle(ChatFormatting.GREEN));
                }else if(!data.getBooleanOr(PREFIX+"mending",false)){
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.no_mending")
                            .withStyle(ChatFormatting.RED));
                }else if(capacity==0){
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.no_tank")
                            .withStyle(ChatFormatting.YELLOW));
                }else if(stored==0){
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.no_xp")
                            .withStyle(ChatFormatting.YELLOW));
                }else{
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.repairing")
                            .withStyle(ChatFormatting.GREEN));
                }

                if(damage>0){
                    int needed=(damage+1)/2; // 1 XP repairs 2 durability.
                    tooltip.add(Component.translatable(
                            "jade.caszual_additions.xp_charger.xp_needed",needed)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            tooltip.add(Component.translatable(
                    "jade.caszual_additions.xp_charger.xp",stored,capacity)
                    .withStyle(ChatFormatting.AQUA));
        }
}
