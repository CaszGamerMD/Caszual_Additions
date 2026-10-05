package com.caszgamermd.caszualadditions.xp;
import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class XpBlocks {
 public static Block XP_TANK, XP_CHARGER, XP_DRAIN;
 private XpBlocks(){}
 public static void initialize(){
  XP_TANK=register("xp_tank",new XpTankBlock(props("xp_tank").strength(2.0f)));
  XP_CHARGER=register("xp_charger",new XpChargerBlock(props("xp_charger").strength(2.5f)));
  XP_DRAIN=register("xp_drain",new XpDrainBlock(props("xp_drain").strength(2.0f)));
 }
 private static BlockBehaviour.Properties props(String name){return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,CaszualAdditions.id(name)));}
 private static Block register(String name,Block block){
  var id=CaszualAdditions.id(name);var bk=ResourceKey.create(Registries.BLOCK,id);Registry.register(BuiltInRegistries.BLOCK,bk,block);
  var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));return block;
 }
}
