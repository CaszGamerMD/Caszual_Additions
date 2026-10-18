package com.caszgamermd.caszualadditions.curtains;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** Hang two-block-high interconnecting strip curtains, like a soft industrial
 * plastic warehouse entrance. Side by side blocks have no divider rails. */
public final class PlasticCurtainsContent {
    public static PlasticCurtainsBlock CURTAINS;
    public static Item ITEM;
    private PlasticCurtainsContent(){}
    public static void initialize(){
        var id=CaszualAdditions.id("plastic_curtains");
        var key=ResourceKey.create(Registries.BLOCK,id);
        CURTAINS=Registry.register(BuiltInRegistries.BLOCK,key,
                new PlasticCurtainsBlock(BlockSetType.OAK,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)
                                .strength(.7f).noOcclusion()
                                .setId(key)));
        var itemKey=ResourceKey.create(Registries.ITEM,id);
        ITEM=Registry.register(BuiltInRegistries.ITEM,itemKey,
                new BlockItem(CURTAINS,new Item.Properties()
                        .setId(itemKey).useBlockDescriptionPrefix()));
    }
}
