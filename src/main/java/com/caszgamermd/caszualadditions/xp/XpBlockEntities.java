package com.caszgamermd.caszualadditions.xp;
import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class XpBlockEntities {
 public static BlockEntityType<XpTankBlockEntity> TANK; public static BlockEntityType<XpChargerBlockEntity> CHARGER;
 private XpBlockEntities(){}
 public static void initialize(){
  TANK=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,CaszualAdditions.id("xp_tank"),FabricBlockEntityTypeBuilder.create(XpTankBlockEntity::new,XpBlocks.XP_TANK).build());
  CHARGER=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,CaszualAdditions.id("xp_charger"),FabricBlockEntityTypeBuilder.create(XpChargerBlockEntity::new,XpBlocks.XP_CHARGER).build());
 }
}
