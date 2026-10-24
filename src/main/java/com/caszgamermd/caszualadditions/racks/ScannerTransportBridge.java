package com.caszgamermd.caszualadditions.racks;

import java.lang.reflect.InvocationTargetException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Optional Pallets Plus bridge. Additions never embeds transport classes. */
public final class ScannerTransportBridge {
    private ScannerTransportBridge(){}
    public static String onBlock(ServerLevel level,ServerPlayer player,
            ItemStack scanner,BlockPos clicked){
        try{
            Class<?> bridge=Class.forName(
                "com.caszgamermd.palletsplus.transport.ScannerAddonBridge");
            var method=bridge.getMethod("onBlock",ServerLevel.class,
                ServerPlayer.class,ItemStack.class,BlockPos.class,String.class);
            return (String)method.invoke(null,level,player,scanner,clicked,
                StorageScannerItem.mode(scanner).name());
        }catch(ClassNotFoundException|NoSuchMethodException ex){
            return "Pallets Plus update is required for this Scanner mode.";
        }catch(InvocationTargetException ex){
            return "Transport request failed: "+ex.getTargetException().getClass().getSimpleName();
        }catch(ReflectiveOperationException ex){
            return "Transport bridge unavailable: "+ex.getClass().getSimpleName();
        }
    }
}
