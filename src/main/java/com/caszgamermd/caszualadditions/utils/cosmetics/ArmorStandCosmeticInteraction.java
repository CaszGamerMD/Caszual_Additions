package com.caszgamermd.caszualadditions.utils.cosmetics;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
public final class ArmorStandCosmeticInteraction {
 private ArmorStandCosmeticInteraction(){}
 public static void initialize(){
  UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
   if(!(entity instanceof ArmorStand stand)||!player.isCrouching()) return InteractionResult.PASS;
   ItemStack held=player.getItemInHand(hand);
   EquipmentSlot slot=pickSlot(held);
   if(slot==null) return InteractionResult.PASS;
   ItemStack old=stand.getItemBySlot(slot);
   if(level.isClientSide()) return InteractionResult.SUCCESS;
   if(!old.isEmpty()){
    if(!player.getInventory().add(old.copy())) player.drop(old.copy(),false);
    stand.setItemSlot(slot,ItemStack.EMPTY);
   }
   if(!held.isEmpty()){
    stand.setItemSlot(slot,held.copyWithCount(1));
    if(!player.getAbilities().instabuild) held.shrink(1);
   }
   return InteractionResult.SUCCESS;
  });
 }
 private static EquipmentSlot pickSlot(ItemStack stack){
  if(stack.isEmpty()) return null;
  if(FlowerCosmetics.isFlower(stack)||SpecialCosmetics.isCrystalCluster(stack)||SpecialCosmetics.isMobHead(stack)||
     SpecialCosmetics.isEndRod(stack)||SpecialCosmetics.isBone(stack)||
     net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("lightning_rod"))
    return EquipmentSlot.HEAD;
  if(stack.is(net.minecraft.world.item.Items.BLAZE_POWDER)) return EquipmentSlot.HEAD;
  if(stack.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)) return EquipmentSlot.HEAD;
  return null;
 }
}
