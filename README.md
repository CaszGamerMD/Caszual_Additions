# Caszual Additions

Caszual Additions is a Fabric 26.2 quality-of-life mod and the hub for the **Caszual** family of mods.

## Documentation / Wiki

**[Read the Caszual Additions Wiki](docs/wiki/Home.md)** — feature guides, crafting recipes, pallet storage, cosmetics, XP utilities, RGB building blocks, and the player-head vending machine.

The complete [recipe index](docs/wiki/Recipes.md) links to the current crafting JSON files. Wiki source pages live in `docs/wiki` so they can be versioned with the mod.

## Goals

- Add small quality-of-life features that fit naturally into Minecraft.
- Provide a common identity for companion Caszual mods.
- Provide one shared Creative Mode tab containing items from installed Caszual mods.
- Make Caszual-family content easy to discover in recipe viewers such as JEI/REI where supported.

## Companion-mod integration

Companion mods should remain independently installable, but can opt into the Caszual ecosystem.

Recommended conventions:

- Add `caszual_additions` as an optional dependency.
- Add all user-facing items/blocks to the common item tag:
  `caszual_additions:caszual_content`
- Prefix searchable descriptions/metadata with `Caszual` where useful.
- When Caszual Additions is installed, companion mods can register their entries into the shared Creative Mode tab.

The shared tag is intentionally cross-mod: any installed Caszual-family mod may contribute items to it.

## Planned companion mods

- Pretty Frogs
- TV Time
- Aquarium-related content
- CaszUtils and other QoL modules
