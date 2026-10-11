package com.caszgamermd.caszualadditions.headvending;

import java.lang.reflect.Method;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * Optional integration with Fuzs' All The Heads (26.2.2).
 *
 * This mod registers a single alltheheads:mob_head item, differentiated by
 * the alltheheads:head_type data component backed by a datapack registry.
 * Ordinary new ItemStack(alltheheads:mob_head) is NOT a valid specific head.
 *
 * Use its own public MobHeadItem.createItem(HolderLookup.Provider, ResourceKey)
 * factory, which builds that component correctly. Reflection allows Caszual
 * Additions to load normally when the other mod is absent: no classes from
 * All The Heads appear in method signatures or dependencies.
 */
public final class AllTheHeadsCompat {
    private static final Map<String, String> DEFAULT_VARIANTS = Map.ofEntries(
            Map.entry("axolotl", "axolotl/lucy"),
            Map.entry("cat", "cat/tabby"),
            Map.entry("chicken", "chicken/temperate"),
            Map.entry("cow", "cow/temperate"),
            Map.entry("frog", "frog/temperate"),
            Map.entry("horse", "horse/brown"),
            Map.entry("llama", "llama/creamy"),
            Map.entry("mooshroom", "mooshroom/red"),
            Map.entry("parrot", "parrot/red"),
            Map.entry("pig", "pig/temperate"),
            Map.entry("rabbit", "rabbit/brown"),
            Map.entry("sheep", "sheep/white"),
            Map.entry("trader_llama", "trader_llama/creamy"),
            Map.entry("tropical_fish", "tropical_fish/clownfish"),
            Map.entry("villager", "villager/plains"),
            Map.entry("wolf", "wolf/pale"),
            Map.entry("zombie_villager", "zombie_villager/plains")
    );
    private static final boolean INSTALLED =
            FabricLoader.getInstance().isModLoaded("alltheheads");
    private static Method factory;
    private static ResourceKey<?> headRegistryKey;
    private static boolean attempted;

    private AllTheHeadsCompat() {}

    public static boolean installed() {
        return INSTALLED;
    }

    private static synchronized boolean ready() {
        if (!INSTALLED) return false;
        if (attempted) return factory != null && headRegistryKey != null;
        attempted = true;
        try {
            Class<?> modRegistry = Class.forName("fuzs.alltheheads.common.init.ModRegistry");
            headRegistryKey = (ResourceKey<?>) modRegistry
                    .getField("HEAD_REGISTRY_KEY").get(null);
            Class<?> item = Class.forName("fuzs.alltheheads.common.world.item.MobHeadItem");
            factory = item.getMethod("createItem", HolderLookup.Provider.class,
                    ResourceKey.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            org.slf4j.LoggerFactory.getLogger("CaszualAdditions/HeadVending")
                    .warn("All The Heads is installed but its head factory is unavailable", error);
            return false;
        }
    }

    /**
     * Return a fully component-equipped real mob-head item or EMPTY if not
     * installed, unknown in the registry, or incompatible with this version.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ItemStack create(HolderLookup.Provider registries, String mobKey) {
        if (registries == null || mobKey == null || !ready()) return ItemStack.EMPTY;
        String variant = DEFAULT_VARIANTS.getOrDefault(mobKey, mobKey);
        try {
            Identifier id = Identifier.fromNamespaceAndPath("alltheheads",
                    "minecraft/" + variant);
            ResourceKey key = ResourceKey.create((ResourceKey) headRegistryKey, id);
            ItemStack stack = (ItemStack) factory.invoke(null, registries, key);
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Not every vanilla mob maps to an All The Heads variant (some
            // are native vanilla heads). Fall back without crashing the UI.
            return ItemStack.EMPTY;
        }
    }
}
