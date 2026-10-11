# Silk Touch: special blocks

This experimental feature lives in `feature/silk-touch-special-blocks` until it is tested and merged.

- **Mob spawners:** A survival player using a **Silk Touch pickaxe** can collect a vanilla monster spawner. The collected block retains its block entity data: selected mob, spawn potentials, ranges, counts and delays. Placing the item restores the saved spawner configuration.
- **Budding geode blocks:** Silk Touch can collect budding amethyst and supported modded budding blocks. Buds and ordinary amethyst blocks retain their normal behavior.
- **Dragon egg:** Left- and right-clicking the egg no longer teleports it. It retains gravity and can be mined normally.

## Modded budding block support

Detection includes blocks implementing Minecraft's `BuddingAmethystBlock` class, blocks whose registry path includes `budding`, and entries added to the data-driven tag:

`#caszual_additions:silk_touch_budding_blocks`

Other mods or modpacks can add blocks to `data/caszual_additions/tags/block/silk_touch_budding_blocks.json` using a datapack with `"replace": false`.

Example:
```json
{
  "replace": false,
  "values": [
    "examplemod:crystal_growth_core",
    "othermod:budding_ruby"
  ]
}
```

The feature leaves existing modded Silk Touch drops alone (no second block drop) and only runs for successful survival block breaks with the appropriate tool.

## Test checklist

1. Break a zombie spawner with a normal pickaxe: no spawner block should drop.
2. Break a zombie spawner with a Silk Touch pickaxe: pick up exactly one spawner; place it and verify it still spawns zombies.
3. Repeat with a custom mob spawner and verify the full configuration survives pickup and placement.
4. Verify creative breaks and protected/canceled breaks do not award extra items.
5. Break budding amethyst with/without Silk Touch; check the drops.
6. Repeat with a modded budding block already implementing Silk Touch and a modded budding block registered only via the tag. Ensure only one block drops.
7. Left- and right-click a dragon egg; verify it does not teleport. Mine and replace it.
8. Mine/place the same spawner repeatedly; verify it does not produce an XP farm.

This branch has not yet been confirmed with a full game test.
