# Upgrading to the Unified Caszual Additions IDs

[← Wiki Home](Home.md) · [Getting Started](Getting-Started.md) · [Recipes](Recipes.md)

Caszual Additions now owns the entire built-in feature set under **one namespace**: `caszual_additions`.

## What changed?

| Before | Now | Examples |
| --- | --- | --- |
| `caszutils:` | `caszual_additions:` | `shortcakes`, `stack_o_jacks`, `vanilla_wafers`, `cosmetic_guide`, `firefly_glass_box`, colored firefly boxes, colored particles |
| `colorful_rods:` | `caszual_additions:` | All 16 colored end rods, `rgb_end_rod`, `rgb_block`, RGB slab/stairs/wall/quarter, doors, wallpaper, fences and more |
| `caszual_additions:` | **Unchanged** | Pallets, generic quarter blocks, XP devices, vending machine, plushie, unbreakable anvil and book |

This also updates block/item models, recipes, loot tables, item/block tags, texture paths, particle definitions, mixin/resource registration and the creative tab. The language keys are consolidated in `assets/caszual_additions/lang/en_us.json`.

## Important: existing-world compatibility

**Existing items and placed blocks using `caszutils:` or `colorful_rods:` IDs are not automatically converted to the new registry IDs.** They may fail to load, be removed by Minecraft, or become unavailable when loading an existing world. The same applies to item stacks in inventories, containers or other mods, and to references embedded in saved commands or datapacks.

1. **Back up your world, player data and server** before replacing an older JAR.
2. Confirm your client and server run the *same* Caszual Additions build and compatible Fabric API.
3. On the old build, move/remove stored items using the old namespaces before upgrading if you want to preserve their contents; consider screenshots or a creative inventory list for recreation.
4. Replace only the old Caszual Additions JAR with the new JAR. Do **not** load another mod also registering those same legacy or new item IDs.
5. If your modpack includes recipes, functions, KubeJS scripts, tags, command blocks or external shader configuration referencing old IDs, manually replace them with `caszual_additions:`.

For example, `colorful_rods:rgb_end_rod` is now `caszual_additions:rgb_end_rod`, and `caszutils:shortcakes` is now `caszual_additions:shortcakes`.

**This is a breaking ID rebrand, not an automatic migration utility.** Avoid opening your only copy of an important world with the new build.

## What stays the same?

- The distributed mod ID is still **`caszual_additions`**.
- The existing Caszual Additions machines and their registry IDs remain unchanged.
- Recipe ingredients and mechanics are intended to stay the same, except that any formerly separate module ingredients use their new IDs.
- External companion mods such as Pretty Frogs, Caszual Aquariums and Caszual TV Time remain separate projects.

[GitHub releases](https://github.com/CaszGamerMD/Caszual_Additions/releases) · [Report an issue](https://github.com/CaszGamerMD/Caszual_Additions/issues)
