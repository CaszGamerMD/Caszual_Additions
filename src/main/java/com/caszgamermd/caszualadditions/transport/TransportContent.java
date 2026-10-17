package com.caszgamermd.caszualadditions.transport;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;

public final class TransportContent {
    public static final Map<String,GuidewayBlock> WAYS=new LinkedHashMap<>();
    public static final Map<String,Block> MODELS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static EntityType<TransportVehicle> VEHICLE;
    public static Block BAY;
    public static Item CONTROLLER;
    public static final String[] VEHICLES={"conveyor_pallet","pallet_engine","pallet_car","passenger_car","pallet_drone"};
    public static void initialize(){
        for(String name:List.of("conveyor_straight","conveyor_left","conveyor_right","conveyor_up","conveyor_down","pallet_rail","pallet_rail_left","pallet_rail_right")){
            var key=ResourceKey.create(Registries.BLOCK,CaszualAdditions.id(name));
            var block=new GuidewayBlock(name,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).noOcclusion().setId(key));
            Registry.register(BuiltInRegistries.BLOCK,key,block); WAYS.put(name,block);
            var ik=ResourceKey.create(Registries.ITEM,CaszualAdditions.id(name));
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
        }
        var bayKey=ResourceKey.create(Registries.BLOCK,CaszualAdditions.id("drone_parking_bay"));
        BAY=Registry.register(BuiltInRegistries.BLOCK,bayKey,new GuidewayBlock("drone_parking_bay",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).noOcclusion().setId(bayKey)));
        var bik=ResourceKey.create(Registries.ITEM,CaszualAdditions.id("drone_parking_bay"));
        ITEMS.put("drone_parking_bay",Registry.register(BuiltInRegistries.ITEM,bik,new BlockItem(BAY,new Item.Properties().setId(bik).useBlockDescriptionPrefix())));
        for(int k=1;k<VEHICLES.length;k++){
            String name=VEHICLES[k];
            var key=ResourceKey.create(Registries.BLOCK,CaszualAdditions.id(name+"_model"));
            MODELS.put(name,Registry.register(BuiltInRegistries.BLOCK,key,new Block(BlockBehaviour.Properties.of().noCollision().noOcclusion().setId(key))));
            var ik=ResourceKey.create(Registries.ITEM,CaszualAdditions.id(name));
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,ik,new VehicleItem(k,new Item.Properties().stacksTo(1).setId(ik))));
        }
        var ck=ResourceKey.create(Registries.ITEM,CaszualAdditions.id("transport_controller"));
        CONTROLLER=Registry.register(BuiltInRegistries.ITEM,ck,new TransportController(new Item.Properties().stacksTo(1).setId(ck)));
        ITEMS.put("transport_controller",CONTROLLER);
        var ek=ResourceKey.create(Registries.ENTITY_TYPE,CaszualAdditions.id("pallet_transport"));
        VEHICLE=Registry.register(BuiltInRegistries.ENTITY_TYPE,ek,EntityType.Builder.<TransportVehicle>of(TransportVehicle::new,MobCategory.MISC).sized(2.4f,2.5f).build(ek));
        FabricDefaultAttributeRegistry.register(VEHICLE,TransportVehicle.attributes());
    }
    private TransportContent(){}
}
