# Barrel of Caszual Fun

[← Home](Home.md) · [Other Features](Other-Features.md)

The **Barrel of Caszual Fun** is a creative-mode-only collection barrel containing **one of every registered item** from the currently installed Caszual mod family. It's intended as a quick way to explore everything in your modpack.

## Getting it

Find **Barrel of Caszual Fun** in the **Caszual Additions** creative inventory tab. It has a barrel body with a melon-green top.

- **Creative-only:** No crafting recipe or loot drops. Only Creative players can place, open, and use it. Survival players cannot open or place the barrel.
- Place it in Creative mode. It automatically fills with one copy of each eligible registered item.
- Right-click it with an empty hand to open the inventory.

## What it contains

The barrel discovers content dynamically from the server's item registry rather than relying on a hard-coded list. It recognizes these mod namespaces:

| Mod | Registry namespace |
| --- | --- |
| Caszual Additions | `caszual_additions` |
| PrettyFrogs | `prettyfrogs` |
| Caszual Aquariums | `linked_aquariums` |
| Caszual MTG | `caszual_mtg` |
| Caszual TV Time | `caszual_tv_time` |
| Ore Laundering | `ore_laundering` (via shared Caszual tag) |

It also includes other namespaces starting with `caszual_` and legacy `caszutils` / `colorful_rods`.

**One of each registered item** means one of each item ID, including modded block items, tools, cards/items, and special devices. Different textures, mob variants, custom cards, and other variants represented by *data on the same item ID* are not automatically separate entries.

Uninstalled companion mods contribute no items; the barrel still works with just Caszual Additions.

## Adaptive capacity

- The inventory automatically grows by pages of **54 slots** as needed. Categories usually reduce the number of pages by packing stackable items into vanilla bundles.
- The GUI shows **54 items per page** with Previous/Next controls, a page counter, and a total item counter.
- Items are organized into **named, color-coded vanilla bundles** (such as RGB Blocks, End Rods, Warehouse Equipment, XP Devices, and Ore Laundering), each internally sorted by registry ID. Non-stackable items and items that cannot fit inside bundles remain loose. Very large groups are split into numbered bundles.
- Removing an item **or an entire bundle** does **not** restock it on every opening. It's a sampler, not an infinite duplicator.
- Existing unbundled barrels are organized when next opened, using only the items still inside (never replenishing items previously taken).
- New items from a newly installed Caszual companion mod are added as new grouped bundles without changing bundles you already removed or edited.
- If more Caszual mods or items are installed later, the next opening adds *newly discovered* registered items to the existing barrel without replacing or replenishing previously removed items.
- Inventory content, the capacity, and already-cataloged items are preserved when the world is saved.
- To create a new full sampler at any time, place another Barrel of Caszual Fun from the Creative inventory.

It does not have a survival recipe. Breaking it drops neither the free starter items nor a usable barrel.
