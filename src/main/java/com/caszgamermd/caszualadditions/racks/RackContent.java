package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public final class RackContent {
    public static RackBlock RACK;
    public static RackStairsBlock RACK_STAIRS;
    public static RackLadderBlock RACK_LADDER;
    public static RackScaffoldingBlock RACK_SCAFFOLDING;
    public static Item RACK_STAIRS_ITEM,RACK_LADDER_ITEM,RACK_SCAFFOLDING_ITEM;
    public static BlockEntityType<RackStairsBlockEntity> RACK_STAIRS_ENTITY;
    public static final java.util.Map<String,Item> INDUSTRIAL_CONCRETE_ITEMS=new java.util.LinkedHashMap<>();
    public static RackConnectorBlock CONNECTOR_BLOCK;
    public static StorageGolemShelfBlock STORAGE_SHELF;
    public static BlockEntityType<StorageGolemShelfEntity> STORAGE_SHELF_ENTITY;
    public static Item STORAGE_SHELF_ITEM,STORAGE_GOLEM_ITEM;
    public static EntityType<StorageGolem> STORAGE_GOLEM_TYPE;
    public static EntityType<MovingPallet> MOVING_PALLET_TYPE;
    public static EntityType<WarehouseForklift> FORKLIFT_TYPE;
    public static Item FORKLIFT_ITEM,FORKLIFT_CONTROLLER;
    public static Block RACK_COMPUTER;
    public static Item STORAGE_SCANNER_ITEM,PORTABLE_WRAPPER,STACK_OF_PALLETS,PALLET_JACK;
    public static StackOfPalletsBlock PALLET_STACK_BLOCK;
    public static BlockEntityType<StackOfPalletsBlockEntity> PALLET_STACK_ENTITY;
    public static ShulkerTankBlock SHULKER_TANK;
    public static PtpBlock PTP_TRANSFER,PTP_HOPPER,PTP_WRAP;
    public static Item PTP_TRANSFER_ITEM,PTP_HOPPER_ITEM,PTP_WRAP_ITEM;
    public static BlockEntityType<PtpBlockEntity> PTP_ENTITY;
    public static BlockEntityType<PtpHopperPort> PTP_PORT_ENTITY;
    public static Item RACK_ITEM, COMPUTER_ITEM, TANK_ITEM;
    public static Item RACK_TAG, AISLE_TAG, CONNECTOR, PALLET_FILTER, WAREHOUSE_TEST_KIT;
    public static BlockEntityType<RackBlockEntity> RACK_ENTITY;
    public static BlockEntityType<ShulkerTankEntity> SHULKER_TANK_ENTITY;
    public static MenuType<ShulkerStationMenu> SHULKER_STATION_MENU;
    public static MenuType<StackOfPalletsMenu> PALLET_STACK_MENU;

    private RackContent() {}
    private static Item item(String name) {
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new Item(new Item.Properties().stacksTo(64).setId(key)));
    }
    private static Item blockItem(String name, Block block) {
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new BlockItem(block,new Item.Properties().stacksTo(16).setId(key).useBlockDescriptionPrefix()));
    }
    public static void initialize() {
        WarehouseRemoteTickets.initialize();
        WarehouseRackAccess.initialize();
        for(String color:java.util.List.of("white","orange","magenta","light_blue",
                "yellow","lime","pink","gray","light_gray","cyan","purple",
                "blue","brown","green","red","black")){
            Identifier industrialId=CaszualAdditions.id(color+"_industrial_grade_concrete");
            ResourceKey<Block> industrialKey=ResourceKey.create(Registries.BLOCK,industrialId);
            Block concrete=Registry.register(BuiltInRegistries.BLOCK,industrialKey,
                    new IndustrialGradeConcreteBlock(BlockBehaviour.Properties
                            .ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f,8.0f)
                            .setId(industrialKey)));
            INDUSTRIAL_CONCRETE_ITEMS.put(color,
                    blockItem(color+"_industrial_grade_concrete",concrete));
        }
        Identifier id=CaszualAdditions.id("pallet_rack");
        ResourceKey<Block> key=ResourceKey.create(Registries.BLOCK,id);
        RACK=Registry.register(BuiltInRegistries.BLOCK,key,
                new RackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(4.0f).noOcclusion().setId(key)));
        RACK_ITEM=blockItem("pallet_rack",RACK);
        ResourceKey<Block> stairsKey=ResourceKey.create(Registries.BLOCK,
                CaszualAdditions.id("rack_stairs"));
        RACK_STAIRS=Registry.register(BuiltInRegistries.BLOCK,stairsKey,
                new RackStairsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(2.6f).noOcclusion().setId(stairsKey)));
        RACK_STAIRS_ITEM=blockItem("rack_stairs",RACK_STAIRS);
        RACK_STAIRS_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("rack_stairs"),
                FabricBlockEntityTypeBuilder.create(RackStairsBlockEntity::new,
                        RACK_STAIRS).build());
        ResourceKey<Block> ladderKey=ResourceKey.create(Registries.BLOCK,
                CaszualAdditions.id("rack_ladder"));
        RACK_LADDER=Registry.register(BuiltInRegistries.BLOCK,ladderKey,
                new RackLadderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER)
                        .strength(1.7f).noOcclusion().setId(ladderKey)));
        RACK_LADDER_ITEM=blockItem("rack_ladder",RACK_LADDER);
        ResourceKey<Block> scaffoldKey=ResourceKey.create(Registries.BLOCK,
                CaszualAdditions.id("rack_scaffolding"));
        RACK_SCAFFOLDING=Registry.register(BuiltInRegistries.BLOCK,scaffoldKey,
                new RackScaffoldingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(2.5f).noOcclusion().setId(scaffoldKey)));
        RACK_SCAFFOLDING_ITEM=blockItem("rack_scaffolding",RACK_SCAFFOLDING);

        Identifier computerId=CaszualAdditions.id("rack_computer");
        ResourceKey<Block> computerKey=ResourceKey.create(Registries.BLOCK,computerId);
        RACK_COMPUTER=Registry.register(BuiltInRegistries.BLOCK,computerKey,
                new RackComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5f).requiresCorrectToolForDrops()
                        .noOcclusion().setId(computerKey)));
        COMPUTER_ITEM=blockItem("rack_computer",RACK_COMPUTER);
        ResourceKey<Item> scannerKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("storage_scanner"));
        STORAGE_SCANNER_ITEM=Registry.register(BuiltInRegistries.ITEM,scannerKey,
                new StorageScannerItem(new Item.Properties()
                        .stacksTo(1).setId(scannerKey)));
        StorageScannerItem.initialize();
        ResourceKey<Item> jackKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("pallet_jack"));
        PALLET_JACK=Registry.register(BuiltInRegistries.ITEM,jackKey,
                new PalletJackItem(new Item.Properties().stacksTo(1).setId(jackKey)));
        PalletJackItem.initialize();
        ResourceKey<Item> portableWrapKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("portable_pallet_wrapper"));
        PORTABLE_WRAPPER=Registry.register(BuiltInRegistries.ITEM,portableWrapKey,
                new PortablePalletWrapperItem(new Item.Properties()
                        .stacksTo(1).setId(portableWrapKey)));
        ResourceKey<Block> stackBlockKey=ResourceKey.create(Registries.BLOCK,
                CaszualAdditions.id("stack_of_pallets"));
        PALLET_STACK_BLOCK=Registry.register(BuiltInRegistries.BLOCK,stackBlockKey,
                new StackOfPalletsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
                        .strength(3.0f).noOcclusion().setId(stackBlockKey)));
        ResourceKey<Item> stackedPalletKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("stack_of_pallets"));
        STACK_OF_PALLETS=Registry.register(BuiltInRegistries.ITEM,stackedPalletKey,
                new StackOfPalletsItem(PALLET_STACK_BLOCK,new Item.Properties()
                        .stacksTo(1).useBlockDescriptionPrefix().setId(stackedPalletKey)));
        PALLET_STACK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("stack_of_pallets"),
                FabricBlockEntityTypeBuilder.create(StackOfPalletsBlockEntity::new,
                        PALLET_STACK_BLOCK).build());
        PALLET_STACK_MENU=Registry.register(BuiltInRegistries.MENU,
                CaszualAdditions.id("pallet_stack_menu"),
                new ExtendedMenuType<>(
                        (menuId,inventory,pos)->new StackOfPalletsMenu(menuId,inventory,pos),
                        BlockPos.STREAM_CODEC));
        RACK_TAG=item("rack_tag");
        AISLE_TAG=item("aisle_tag");
        Identifier connectorId=CaszualAdditions.id("rack_connector");
        ResourceKey<Block> connectorKey=ResourceKey.create(Registries.BLOCK,connectorId);
        CONNECTOR_BLOCK=Registry.register(BuiltInRegistries.BLOCK,connectorKey,
                new RackConnectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5f).noOcclusion().setId(connectorKey)));
        ResourceKey<Item> connectorItemKey=ResourceKey.create(Registries.ITEM,connectorId);
        CONNECTOR=Registry.register(BuiltInRegistries.ITEM,connectorItemKey,
                new RackConnectorItem(CONNECTOR_BLOCK,new Item.Properties()
                        .stacksTo(16).setId(connectorItemKey).useBlockDescriptionPrefix()));
        ResourceKey<Item> kitKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("warehouse_test_kit"));
        WAREHOUSE_TEST_KIT=Registry.register(BuiltInRegistries.ITEM,kitKey,
                new WarehouseTestKitItem(new Item.Properties().stacksTo(1).setId(kitKey)));
        PALLET_FILTER=Registry.register(BuiltInRegistries.ITEM,
                ResourceKey.create(Registries.ITEM,CaszualAdditions.id("pallet_filter")),
                new PalletFilterItem(new Item.Properties().stacksTo(16)
                        .setId(ResourceKey.create(Registries.ITEM,CaszualAdditions.id("pallet_filter")))));
        RACK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("rack"),
                FabricBlockEntityTypeBuilder.create(RackBlockEntity::new,RACK).build());

        Identifier tankId=CaszualAdditions.id("shulker_tank");
        ResourceKey<Block> tankKey=ResourceKey.create(Registries.BLOCK,tankId);
        SHULKER_TANK=Registry.register(BuiltInRegistries.BLOCK,tankKey,
                new ShulkerTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.0f).setId(tankKey)));
        TANK_ITEM=blockItem("shulker_tank",SHULKER_TANK);
        SHULKER_TANK_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("shulker_tank"),
                FabricBlockEntityTypeBuilder.create(ShulkerTankEntity::new,SHULKER_TANK).build());
        SHULKER_STATION_MENU=Registry.register(BuiltInRegistries.MENU,
                CaszualAdditions.id("shulker_station"),
                new ExtendedMenuType<>(
                        (menuId,inventory,pos)->new ShulkerStationMenu(menuId,inventory,pos),
                        BlockPos.STREAM_CODEC));

        // All warehouse storage and orders use physical shelves and PTPs.
        Identifier ptpId=CaszualAdditions.id("pallet_transfer_block");
        ResourceKey<Block> ptpKey=ResourceKey.create(Registries.BLOCK,ptpId);
        PTP_TRANSFER=Registry.register(BuiltInRegistries.BLOCK,ptpKey,
                new PtpBlock(PtpBlock.Kind.TRANSFER,BlockBehaviour.Properties
                        .ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f)
                        .noOcclusion().setId(ptpKey)));
        PTP_TRANSFER_ITEM=blockItem("pallet_transfer_block",PTP_TRANSFER);

        Identifier adapterId=CaszualAdditions.id("ptp_hopper_adapter");
        ResourceKey<Block> adapterKey=ResourceKey.create(Registries.BLOCK,adapterId);
        PTP_HOPPER=Registry.register(BuiltInRegistries.BLOCK,adapterKey,
                new PtpBlock(PtpBlock.Kind.HOPPER,BlockBehaviour.Properties
                        .ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f)
                        .noOcclusion().setId(adapterKey)));
        PTP_HOPPER_ITEM=blockItem("ptp_hopper_adapter",PTP_HOPPER);

        Identifier wrapId=CaszualAdditions.id("ptp_wrap_unit");
        ResourceKey<Block> wrapKey=ResourceKey.create(Registries.BLOCK,wrapId);
        PTP_WRAP=Registry.register(BuiltInRegistries.BLOCK,wrapKey,
                new PtpBlock(PtpBlock.Kind.WRAP,BlockBehaviour.Properties
                        .ofFullCopy(Blocks.IRON_BLOCK).strength(3.5f)
                        .noOcclusion().setId(wrapKey)));
        PTP_WRAP_ITEM=blockItem("ptp_wrap_unit",PTP_WRAP);

        PTP_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("pallet_transfer_point"),
                FabricBlockEntityTypeBuilder.create(PtpBlockEntity::new,
                        PTP_TRANSFER,PTP_HOPPER,PTP_WRAP).build());
        PTP_PORT_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("ptp_hopper_port"),
                FabricBlockEntityTypeBuilder.create(PtpHopperPort::new,PTP_HOPPER).build());

        Identifier shelfId=CaszualAdditions.id("storage_golem_shelf");
        ResourceKey<Block> shelfKey=ResourceKey.create(Registries.BLOCK,shelfId);
        STORAGE_SHELF=Registry.register(BuiltInRegistries.BLOCK,shelfKey,
                new StorageGolemShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5f).noOcclusion().setId(shelfKey)));
        STORAGE_SHELF_ITEM=blockItem("storage_golem_shelf",STORAGE_SHELF);
        STORAGE_SHELF_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("storage_golem_shelf"),
                FabricBlockEntityTypeBuilder.create(StorageGolemShelfEntity::new,STORAGE_SHELF).build());
        STORAGE_GOLEM_ITEM=item("storage_golem");
        ResourceKey<EntityType<?>> golemKey=ResourceKey.create(
                Registries.ENTITY_TYPE,CaszualAdditions.id("storage_golem"));
        STORAGE_GOLEM_TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,golemKey,
                EntityType.Builder.<StorageGolem>of(StorageGolem::new,MobCategory.MISC)
                        .sized(0.70f,1.45f).build(golemKey));
        FabricDefaultAttributeRegistry.register(STORAGE_GOLEM_TYPE,StorageGolem.attributes());
        ResourceKey<EntityType<?>> movingKey=ResourceKey.create(Registries.ENTITY_TYPE,
                CaszualAdditions.id("moving_pallet"));
        MOVING_PALLET_TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,movingKey,
                EntityType.Builder.<MovingPallet>of(MovingPallet::new,MobCategory.MISC)
                        .sized(1.85f,2.50f).build(movingKey));
        FabricDefaultAttributeRegistry.register(MOVING_PALLET_TYPE,MovingPallet.attributes());

        ResourceKey<Item> forkliftItemKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("warehouse_forklift"));
        FORKLIFT_ITEM=Registry.register(BuiltInRegistries.ITEM,forkliftItemKey,
                new WarehouseForkliftItem(new Item.Properties().stacksTo(1)
                        .setId(forkliftItemKey)));
        ResourceKey<Item> forkliftControllerKey=ResourceKey.create(Registries.ITEM,
                CaszualAdditions.id("forklift_controller"));
        FORKLIFT_CONTROLLER=Registry.register(BuiltInRegistries.ITEM,forkliftControllerKey,
                new WarehouseForkliftItem(new Item.Properties().stacksTo(1)
                        .setId(forkliftControllerKey),false));
        ResourceKey<EntityType<?>> forkliftKey=ResourceKey.create(Registries.ENTITY_TYPE,
                CaszualAdditions.id("warehouse_forklift"));
        FORKLIFT_TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,forkliftKey,
                EntityType.Builder.<WarehouseForklift>of(WarehouseForklift::new,MobCategory.MISC)
                        .sized(1.20f,2.6f).build(forkliftKey));
        FabricDefaultAttributeRegistry.register(FORKLIFT_TYPE,WarehouseForklift.attributes());
        RackComputerNet.initialize();
    }
}
