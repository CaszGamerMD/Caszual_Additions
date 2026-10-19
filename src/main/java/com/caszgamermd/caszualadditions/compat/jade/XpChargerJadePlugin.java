package com.caszgamermd.caszualadditions.compat.jade;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.xp.XpChargerBlock;
import com.caszgamermd.caszualadditions.xp.XpChargerBlockEntity;
import com.caszgamermd.caszualadditions.xp.XpNetwork;
import com.caszgamermd.caszualadditions.xp.XpRepair;
import com.caszgamermd.caszualadditions.xp.XpTankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Optional Jade/WAILA integration. Fabric only instantiates the "jade"
 * entrypoint when Jade is installed; no Jade class is touched by regular
 * Caszual Additions initialization.
 *
 * Everything shown is read on the logical server, because the Charger does
 * not normally sync its inventory to the client and XP is pooled across
 * adjacent tanks. This also works on dedicated multiplayer servers.
 */
@WailaPlugin
public final class XpChargerJadePlugin implements IWailaPlugin {
    private static final Identifier UID=CaszualAdditions.id("xp_charger_repair");

    @Override public void register(IWailaCommonRegistration registration){
        registration.registerBlockDataProvider(Server.INSTANCE,XpChargerBlockEntity.class);
    }

    @Override public void registerClient(IWailaClientRegistration registration){
        registration.registerBlockComponent(Client.INSTANCE,XpChargerBlock.class);
    }

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final String PREFIX="caszual_xp_charger_";

        @Override public Identifier getUid(){return UID;}

        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor){
            if(!(accessor.getBlockEntity() instanceof XpChargerBlockEntity charger)
                    ||!(accessor.getLevel() instanceof ServerLevel level))return;

            ItemStack item=charger.item();
            data.putBoolean(PREFIX+"present",true);
            data.putBoolean(PREFIX+"item",!item.isEmpty());
            if(!item.isEmpty()){
                data.putString(PREFIX+"name",item.getHoverName().getString());
                data.putInt(PREFIX+"max",item.getMaxDamage());
                data.putInt(PREFIX+"damage",item.getDamageValue());
                data.putBoolean(PREFIX+"mending",XpRepair.hasMending(item));
            }

            // Traverse once for both stored and capacity; no stale client
            // guesses about linked or filled tanks.
            var tanks=XpNetwork.tanks(level,accessor.getPosition());
            long stored=0;
            for(XpTankBlockEntity tank:tanks)stored+=tank.stored();
            data.putInt(PREFIX+"stored",(int)Math.min(Integer.MAX_VALUE,stored));
            data.putInt(PREFIX+"capacity",(int)Math.min(Integer.MAX_VALUE,
                    (long)tanks.size()*XpTankBlockEntity.CAPACITY));
        }
    }

    public enum Client implements IBlockComponentProvider {
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
}
