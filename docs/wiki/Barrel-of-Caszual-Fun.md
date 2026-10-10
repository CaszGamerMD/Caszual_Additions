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

It also includes other namespaces starting with `caszual_` and legacy `caszutils` / `colorful_rods`.

**One of each registered item** means one of each item ID, including modded block items, tools, cards/items, and special devices. Different textures, mob variants, custom cards, and other variants represented by *data on the same item ID* are not automatically separate entries.

Uninstalled companion mods contribute no items; the barrel still works with just Caszual Additions.

## Adaptive capacity

- The inventory automatically grows by pages of **54 slots**, accommodating as many item IDs as are present without a fixed chest-sized cap.
- The GUI shows **54 items per page** with Previous/Next controls, a page counter, and a total item counter.
- Slots are filled in a predictable order, sorted by item registry ID.
- Removing items does **not** restock them on every opening. It's a sampler, not an infinite duplicator.
- If more Caszual mods or items are installed later, the next opening adds *newly discovered* registered items to the existing barrel without replacing or replenishing previously removed items.
- Inventory content, the capacity, and already-cataloged items are preserved when the world is saved.
- To create a new full sampler at any time, place another Barrel of Caszual Fun from the Creative inventory.

It does not have a survival recipe. Breaking it drops neither the free starter items nor a usable barrel.
