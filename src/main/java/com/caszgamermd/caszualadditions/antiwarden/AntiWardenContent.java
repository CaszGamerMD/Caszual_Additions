package com.caszgamermd.caszualadditions.antiwarden;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Warden-countermeasures, survival craftable, registered before creative tabs. */
public final class AntiWardenContent {
    public static Item WOOL_HELMET, WOOL_BOOTS;
    public static WardenTrapBlock WARDEN_TRAP;
    public static BlockEntityType<WardenTrapBlockEntity> TRAP_ENTITY;

    private static final ResourceKey<EquipmentAsset> WOOL_EQUIPMENT =
            ResourceKey.create(EquipmentAssets.ROOT_ID,CaszualAdditions.id("wool_armor"));
    private static final ArmorMaterial WOOL_MATERIAL = new ArmorMaterial(
            11,Map.of(ArmorType.HELMET,1,ArmorType.BOOTS,1),
            15,SoundEvents.ARMOR_EQUIP_LEATHER,0.0F,0.0F,
            ItemTags.WOOL,WOOL_EQUIPMENT);

    private AntiWardenContent(){}

    public static void initialize(){
        WOOL_HELMET=armor("wool_helmet",ArmorType.HELMET);
        WOOL_BOOTS=armor("wool_boots",ArmorType.BOOTS);
        var id=CaszualAdditions.id("warden_trap");
        var blockKey=ResourceKey.create(Registries.BLOCK,id);
        WARDEN_TRAP=Registry.register(BuiltInRegistries.BLOCK,blockKey,
                new WardenTrapBlock(BlockBehaviour.Properties.of()
                        .strength(3.5F).noOcclusion().setId(blockKey)));
        var itemKey=ResourceKey.create(Registries.ITEM,id);
        Registry.register(BuiltInRegistries.ITEM,itemKey,
                new BlockItem(WARDEN_TRAP,new Item.Properties()
                        .setId(itemKey).useBlockDescriptionPrefix()));
        TRAP_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("warden_trap"),
                FabricBlockEntityTypeBuilder.create(
                        WardenTrapBlockEntity::new,WARDEN_TRAP).build());
        // If the player equips the hood after already receiving Darkness,
        // clear the existing effect as well as blocking all future attempts.
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%5!=0)return;
            for(var player:server.getPlayerList().getPlayers()){
                if(player.getItemBySlot(EquipmentSlot.HEAD).is(WOOL_HELMET)){
                    player.removeEffect(MobEffects.DARKNESS);
                    player.removeEffect(MobEffects.BLINDNESS);
                }
            }
        });
    }
    private static Item armor(String name,ArmorType slot){
        var id=CaszualAdditions.id(name);
        var key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new Item(new Item.Properties().setId(key)
                        .humanoidArmor(WOOL_MATERIAL,slot)
                        .durability(slot.getDurability(11))));
    }
}
