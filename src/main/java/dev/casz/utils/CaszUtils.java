package dev.casz.utils;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class CaszUtils {
    public static final String MOD_ID = "caszutils";
    public static final Item SHORTCAKES = snack("shortcakes", SizeSnack.Direction.SHRINK);
    public static final Item STACK_O_JACKS = snack("stack_o_jacks", SizeSnack.Direction.GROW);
    public static final Item VANILLA_WAFERS = snack("vanilla_wafers", SizeSnack.Direction.RESET);

    private static Item snack(String name, SizeSnack.Direction direction) {
        var id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        var key = ResourceKey.create(Registries.ITEM, id);
        var food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build();
        return Registry.register(BuiltInRegistries.ITEM, key,
            new SizeSnack(new Item.Properties().setId(key).food(food), direction));
    }

    public static void initialize() {
        FireflyGlass.initialize();
        dev.casz.utils.cosmetics.Cosmetics.initialize();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(SHORTCAKES);
            entries.accept(STACK_O_JACKS);
            entries.accept(VANILLA_WAFERS);
        });
        org.slf4j.LoggerFactory.getLogger(MOD_ID).info("CaszUtils systems initialized inside Caszual Additions");
    }
}