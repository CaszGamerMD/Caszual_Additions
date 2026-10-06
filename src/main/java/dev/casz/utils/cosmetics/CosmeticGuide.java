package dev.casz.utils.cosmetics;

import dev.casz.utils.CaszUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class CosmeticGuide {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CaszUtils.MOD_ID, "cosmetic_guide");
    public static final Item ITEM;
    static {
        var key = ResourceKey.create(Registries.ITEM, ID);
        ITEM = Registry.register(BuiltInRegistries.ITEM, key, new CosmeticGuideItem(new Item.Properties().setId(key).stacksTo(1)));
    }
    private CosmeticGuide() {}
    public static void initialize() {}
}
