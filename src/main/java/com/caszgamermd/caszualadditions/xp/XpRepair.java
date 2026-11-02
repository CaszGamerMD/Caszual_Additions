package com.caszgamermd.caszualadditions.xp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Repairs three Mending slots independently from one real XP network. */
public final class XpRepair {
    private XpRepair(){}
    public static boolean hasMending(ItemStack stack){
        var ench=stack.get(DataComponents.ENCHANTMENTS);
        if(ench==null)return false;
        for(var entry:ench.entrySet())
            if(entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.MENDING))
                return true;
        return false;
    }
    public static void tick(ServerLevel level,XpChargerBlockEntity charger){
        for(int i=0;i<charger.getContainerSize();i++){
            ItemStack stack=charger.getItem(i);
            if(stack.isEmpty()||!stack.isDamaged()||!hasMending(stack))continue;
            int xp=Math.min(4,(stack.getDamageValue()+1)/2);
            int used=XpNetwork.extract(level,charger.getBlockPos(),xp);
            if(used>0){
                stack.setDamageValue(Math.max(0,stack.getDamageValue()-used*2));
                charger.setChanged();
            }
        }
    }
}
