package com.caszgamermd.caszualadditions.painting;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class IndustrialPaintContent {
    public static Block PAINT_CAN,PRIMER_CAN,SURFACE;
    /** Six distinct one-quad baked model proxies. A painted face must NOT
     * be a 0.001-thick FULL CUBE: the backface overlaps and turns black,
     * and opposite faces z-fight against each other on full blocks. */
    public static final Block[] SURFACE_FACES=new Block[6];
    public static final String[] FACE_NAMES={
            "west","east","down","up","north","south"};

    public static Item PAINT_CAN_ITEM,PRIMER_CAN_ITEM,BRUSH,REMOVER;
    public static EntityType<IndustrialFinishEntity> FINISH;
    public static BlockEntityType<IndustrialPaintCanBlockEntity> PAINT_CAN_ENTITY;
    private IndustrialPaintContent(){}
    private static Block registered(String name,java.util.function.Function<
            BlockBehaviour.Properties,Block> creator){
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Block> key=ResourceKey.create(Registries.BLOCK,id);
        return Registry.register(BuiltInRegistries.BLOCK,key,
                creator.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(1.6f).noOcclusion().setId(key)));
    }
    private static Item blockItem(String name,Block block){
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new BlockItem(block,new Item.Properties().stacksTo(16)
                        .setId(key).useBlockDescriptionPrefix()));
    }
    public static void initialize(){
        PAINT_CAN=registered("industrial_paint_can",IndustrialPaintCanBlock::new);
        PRIMER_CAN=registered("industrial_primer_can",IndustrialPrimerCanBlock::new);
        // Proxy isn't obtainable: renderer samples the actual target's
        // voxel/model outline and tints this subtle textured surface.
        SURFACE=registered("industrial_paint_surface",Block::new);
        for(int i=0;i<FACE_NAMES.length;i++)
            SURFACE_FACES[i]=registered("industrial_paint_face_"+FACE_NAMES[i],Block::new);
        PAINT_CAN_ITEM=blockItem("industrial_paint_can",PAINT_CAN);
        PRIMER_CAN_ITEM=blockItem("industrial_primer_can",PRIMER_CAN);
        Identifier brushId=CaszualAdditions.id("industrial_paint_brush");
        ResourceKey<Item> brushKey=ResourceKey.create(Registries.ITEM,brushId);
        BRUSH=Registry.register(BuiltInRegistries.ITEM,brushKey,
                new IndustrialBrushItem(new Item.Properties().stacksTo(1).setId(brushKey)));
        Identifier removerId=CaszualAdditions.id("industrial_paint_remover");
        ResourceKey<Item> removerKey=ResourceKey.create(Registries.ITEM,removerId);
        REMOVER=Registry.register(BuiltInRegistries.ITEM,removerKey,
                new IndustrialRemoverItem(new Item.Properties().stacksTo(1).setId(removerKey)));
        PAINT_CAN_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                CaszualAdditions.id("industrial_paint_can"),
                FabricBlockEntityTypeBuilder.create(IndustrialPaintCanBlockEntity::new,
                        PAINT_CAN).build());
        ResourceKey<EntityType<?>> key=ResourceKey.create(Registries.ENTITY_TYPE,
                CaszualAdditions.id("industrial_finish"));
        FINISH=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<IndustrialFinishEntity>of(
                        IndustrialFinishEntity::new,MobCategory.MISC)
                        // The old 64-block tracking cap made paint vanish
                        // while its *block* was still being rendered. Track
                        // up to the maximal vanilla view horizon (32 chunks),
                        // and let normal view/frustum visibility cull it.
                        .sized(1.0f,1.0f).clientTrackingRange(512).build(key));
        IndustrialPaintNet.initialize();
        // A broken block's coat must go immediately, even before the entity's
        // periodic check. This also handles Creative block breaking.
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
                (world,player,pos,state,entity)->{
                    if(world instanceof net.minecraft.server.level.ServerLevel server)
                        IndustrialFinishLogic.remove(server,pos);
                });
    }
}