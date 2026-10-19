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

    @Override public void registerClient(IWailaClientRegistration registration){
        // Jade only calls this on the physical client. Resolve this class
        // lazily: loading client-only tooltip/layout classes on a dedicated
        // server would otherwise crash during plugin instantiation.
        try{
            var clazz=Class.forName(
                    "com.caszgamermd.caszualadditions.compat.jade.XpChargerJadeTooltip");
            registration.registerBlockComponent(
                    (IBlockComponentProvider)clazz.getDeclaredConstructor().newInstance(),
                    XpChargerBlock.class);
        }catch(ReflectiveOperationException e){
            throw new IllegalStateException("Cannot register XP Charger Jade tooltip",e);
        }
    }
}
