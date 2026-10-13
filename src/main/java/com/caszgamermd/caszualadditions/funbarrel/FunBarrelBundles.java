package com.caszgamermd.caszualadditions.funbarrel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

/**
 * Organizes a creative-only sample collection into real, vanilla bundles.
 *
 * This is a VIEW/packaging operation on items ALREADY in the barrel. It must
 * never ask the registry for extra copies: removing a bundle or an item from
 * its contents is permanent for this particular barrel, just like before.
 *
 * Vanilla bundles can only carry 64 total units. Non-stackable items (which
 * weigh a full bundle by themselves) and container-incompatible items remain
 * loose, while large categories are split into numbered bundles.
 */
public final class FunBarrelBundles {
    private static final int MAX_WEIGHT = 64;
    private FunBarrelBundles() {}

    private record Category(int order, String name, String bundleColor) {}

    private static Item dyedBundle(String color) {
        // Minecraft 26.2 removed the individual colored-bundle constants
        // from Items; use the built-in item registry instead.
        return BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace(color + "_bundle"));
    }

    private static Category category(Identifier id) {
        String ns=id.getNamespace(), path=id.getPath();
        if (ns.equals("ore_laundering"))
            return new Category(70, "Ore Laundering", "lime");
        if (ns.equals("prettyfrogs") || ns.equals("pretty_frogs"))
            return new Category(80, "Pretty Frogs", "green");
        if (ns.equals("linked_aquariums") || ns.equals("caszual_aquariums"))
            return new Category(90, "Caszual Aquariums", "light_blue");
        if (ns.contains("mtg"))
            return new Category(100, "Caszual MTG", "purple");
        if (ns.contains("tvtime") || ns.contains("tv_time"))
            return new Category(110, "Caszual TV Time", "blue");
        if (ns.contains("miningrig") || ns.contains("mining_rig"))
            return new Category(120, "Mining Rig", "gray");

        if (path.endsWith("_end_rod") || path.equals("end_rod"))
            return new Category(10, "End Rods", "orange");
        if (path.startsWith("rgb_") || path.equals("quarter_block"))
            return new Category(20, "RGB Blocks", "cyan");
        if (path.contains("firefly_glass_box"))
            return new Category(30, "Firefly Glass", "yellow");
        if (path.startsWith("xp_"))
            return new Category(40, "XP Devices", "lime");
        if (path.contains("pallet") || path.contains("rack") ||
                path.contains("warehouse") || path.contains("scanner") ||
                path.contains("storage_golem") || path.startsWith("ptp_") ||
                path.contains("industrial_grade_concrete") || path.contains("aisle_tag"))
            return new Category(50, "Warehouse Equipment", "gray");
        if (path.contains("plushie") || path.contains("cosmetic") ||
                path.contains("firefly") || path.contains("nautilus"))
            return new Category(60, "Cosmetics and Fun", "pink");
        return new Category(130, "Other Caszual Items", "white");
    }

    /**
     * Sorted, defensively copied display items. These are the only stacks
     * physically moved; no registry lookup or implicit replenishment occurs.
     */
    public static List<ItemStack> organize(List<ItemStack> available) {
        Map<Integer, List<ItemStack>> groups = new LinkedHashMap<>();
        Map<Integer, Category> labels = new LinkedHashMap<>();
        List<ItemStack> loose = new ArrayList<>();

        for (ItemStack old : available) {
            if (old == null || old.isEmpty()) continue;
            ItemStack stack = old.copy();
            // Do not eat full stacks, other bundles, shulkers, single-stack
            // devices/cards or anything vanilla refuses to store in bundles.
            if (stack.getCount() != 1 || stack.getMaxStackSize() <= 1
                    || !stack.getItem().canFitInsideContainerItems()) {
                loose.add(stack);
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) {
                loose.add(stack);
                continue;
            }
            Category section = category(id);
            labels.putIfAbsent(section.order(), section);
            groups.computeIfAbsent(section.order(), ignored -> new ArrayList<>()).add(stack);
        }

        List<ItemStack> arranged = new ArrayList<>();
        groups.keySet().stream().sorted().forEach(order -> {
            Category section = labels.get(order);
            List<ItemStack> entries = groups.get(order);
            entries.sort(Comparator.comparing(s ->
                    BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            List<ItemStack> contents = new ArrayList<>();
            int weight = 0;
            int bundleNumber = 1;
            for (ItemStack entry : entries) {
                // Vanilla's occupancy for a count-one item is 1/max stack
                // size of a bundle: calculate it in units of 1/64.
                int itemWeight = Math.max(1, (MAX_WEIGHT + entry.getMaxStackSize() - 1)
                        / entry.getMaxStackSize());
                if (weight + itemWeight > MAX_WEIGHT && !contents.isEmpty()) {
                    arranged.add(bundle(section, contents, bundleNumber++));
                    contents = new ArrayList<>();
                    weight = 0;
                }
                contents.add(entry);
                weight += itemWeight;
            }
            if (!contents.isEmpty())arranged.add(bundle(section, contents, bundleNumber));
        });

        // Preserve items vanilla bundles cannot hold as individually usable
        // entries. They were never deleted by organizing.
        loose.sort(Comparator.comparing(s ->
                BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        arranged.addAll(loose);
        return arranged;
    }

    private static ItemStack bundle(Category section, List<ItemStack> contents, int sequence) {
        ItemStack result = new ItemStack(dyedBundle(section.bundleColor()));
        result.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(contents.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
        result.set(DataComponents.CUSTOM_NAME, Component.literal(section.name()
                + (sequence == 1 ? "" : " (" + sequence + ")")));
        return result;
    }
}
