package com.casz.colorfulrods;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ColorfulRods {
    public static final String MOD_ID = "colorful_rods";
    public static final Map<String, Block> RODS = new LinkedHashMap<>();
    private ColorfulRods() {}
    public static void initialize() {
        for (String color : new String[]{"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black","rgb"}) {
            String name=color+"_end_rod"; Identifier id=Identifier.fromNamespaceAndPath(MOD_ID,name);
            ResourceKey<Block> blockKey=ResourceKey.create(Registries.BLOCK,id);
            Block rod=new ColoredEndRodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD).setId(blockKey));
            Registry.register(BuiltInRegistries.BLOCK,blockKey,rod);
            ResourceKey<Item> itemKey=ResourceKey.create(Registries.ITEM,id);
            Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(rod,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
            RODS.put(color,rod);
        }
        RgbBuildingBlocks.initialize();
    }
}
