package dev.casz.utils;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class FireflyGlass {
    public static final Map<DyeColor, FireflyGlassBlock> COLORED = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, SimpleParticleType> PARTICLES = new EnumMap<>(DyeColor.class);
    public static final FireflyGlassBlock CLEAR = register("firefly_glass_box", null);
    private FireflyGlass() {}
    private static FireflyGlassBlock register(String name, DyeColor color) {
        var id = Identifier.fromNamespaceAndPath(CaszUtils.MOD_ID, name);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = new FireflyGlassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().setId(key), color);
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey, new FireflyGlassItem(block,
            new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    public static void initialize() {
        for (var color : DyeColor.values()) {
            COLORED.put(color, register(color.getName() + "_firefly_glass_box", color));
            PARTICLES.put(color, Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(CaszUtils.MOD_ID, color.getName() + "_firefly"), FabricParticleTypes.simple()));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
            output.accept(CLEAR);
            for (var color : DyeColor.values()) output.accept(COLORED.get(color));
        });
        org.slf4j.LoggerFactory.getLogger(CaszUtils.MOD_ID).info("Registered 17 firefly glass boxes and 16 tinted firefly particles");
    }
}