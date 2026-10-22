# Caszual Additions

Caszual Additions is a Fabric 26.2 quality-of-life mod and the hub for the **Caszual** family of mods.

## Documentation / Wiki

**[Read the Caszual Additions Wiki](docs/wiki/Home.md)** — feature guides, crafting recipes, pallet storage, cosmetics, XP utilities, RGB building blocks, and the player-head vending machine.

The complete [recipe index](docs/wiki/Recipes.md) links to the current crafting JSON files. Wiki source pages live in `docs/wiki` so they can be versioned with the mod.

**Migration warning:** Older items/blocks registered under `caszutils:` or `colorful_rods:` now use `caszual_additions:` and are not automatically migrated. Back up affected worlds first. [Read the upgrade notes](docs/wiki/Upgrade-Notes.md).\n\n## Goals

- Add small quality-of-life features that fit naturally into Minecraft.
- Provide a common identity for companion Caszual mods.
- Provide one shared Creative Mode tab containing items from installed Caszual mods.
- Make Caszual-family content easy to discover in recipe viewers such as JEI/REI where supported.

## Wearable Aquarium compatibility

When **Linked Aquariums 0.8.0+** is installed, its [Wearable Aquarium](https://github.com/CaszGamerMD/Caszual_Aquariums/blob/main/docs/wiki/Wearable-Aquarium.md) can be equipped in the **chest cosmetic slot**. It visually replaces the player with a glass humanoid aquarium containing up to four saved fish. Fish are loaded and retrieved using buckets while the item is held; the cosmetic remains appearance-only. The armor's normal gameplay effects remain unchanged.

## Companion-mod integration

Companion mods should remain independently installable, but can opt into the Caszual ecosystem.

Recommended conventions:

- Add `caszual_additions` as an optional dependency.
- Add all user-facing items/blocks to the common item tag:
  `caszual_additions:caszual_content`
- Prefix searchable descriptions/metadata with `Caszual` where useful.
- When Caszual Additions is installed, companion mods can register their entries into the shared Creative Mode tab.

The shared tag is intentionally cross-mod: any installed Caszual-family mod may contribute items to it.

## Barrel of Caszual Fun — shared item tag (0.1.1)

The creative-only **Barrel of Caszual Fun** now discovers items from the shared `caszual_additions:caszual_content` item tag **as well as** the legacy Caszual mod namespaces. This allows a separately installed companion such as **Caszual MTG 0.11.2+** to contribute all of its user-facing items without making Caszual Additions a required dependency.

The same tag feeds the shared Caszual Creative tab. The barrel scans the active item registry whenever an existing barrel is opened; new tagged items are appended, while items a player deliberately removed are not recreated. The barrel remains **creative-mode only** and retains its page-based inventory.

## Planned companion mods

- Pretty Frogs
- TV Time
- Aquarium-related content
- Built-in utility features (cosmetics, firefly glass, size snacks)

## Drone Parking Bay warehouse pickup (0.1.3)

Pallets Plus 0.4.9 calls **DroneBayNoReplacementDispatch.request** to send **four independent Storage Golems** to a wrapped pallet on a linked INPUT Parking Bay. All four arrive without cargo, assemble at the pallet, and carry its original 216-slot inventory to a vacant INPUT aisle rack (regular rack fallback). No spare or replacement pallet and no online warehouse owner are required. All four shelf workers are restored after delivery. **Normal Scanner REMOTE pickup is unchanged:** four Golems total, three walking to the target and one bringing an empty replacement pallet to exchange before they return.
