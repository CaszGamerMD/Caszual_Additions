package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
public final class XpRepair {
 private XpRepair(){}
 public static boolean hasMending(ItemStack stack){
  var ench=stack.get(DataComponents.ENCHANTMENTS); if(ench==null)return false;
  for(var e:ench.entrySet())if(e.getKey().is(net.minecraft.world.item.enchantment.Enchantments.MENDING))return true; return false;
 }
 public static void tick(ServerLevel level,XpChargerBlockEntity charger){
  var s=charger.item();if(s.isEmpty()||!s.isDamaged()||!hasMending(s))return;
  int xp=Math.min(4,(s.getDamageValue()+1)/2);int used=XpNetwork.extract(level,charger.getBlockPos(),xp);
  if(used>0){s.setDamageValue(Math.max(0,s.getDamageValue()-used*2));charger.setChanged();}
 }
}
