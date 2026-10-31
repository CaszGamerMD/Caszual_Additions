# Caszual Additions

Caszual Additions is a Fabric 26.2 quality-of-life mod and the hub for the **Caszual** family of mods.

## Enchantment Printer (v0.1.16)

The **Enchantment Printer** is a stand-alone Caszual Additions workstation, not a Mining Rig machine. Craft it from an Enchanting Table, Anvil, Redstone, Iron Ingots, Lapis and Emeralds, then right-click it to open the printer's catalog interface.

**Four slots:** FEED (an enchanted book or enchanted equipment to learn/refill enchantments), TARGET (one compatible item to enchant), LAPIS and EMERALDS. Click **LEARN / REFILL** to consume one donor item; every enchantment on that item is learned or refreshed at the highest supplied level, independently of the other stored enchantments. Consuming a donor is a deliberate action; merely putting it into the slot does not destroy it.

Choose one or more learned enchantments from the paginated, searchable-in-future list, then click **PRINT SELECTED**. The printer validates every selected enchantment for target support, level and mutual compatibility, plus all resources **before** committing any change; incompatible batches cannot waste ingredients. A successful batch applies them all to the single target item. The machine remembers its selected set for easy repeat prints on replacement items.

**Balance:** Each enchantment consumes `3 + 2 × level` player XP **levels**, `level` lapis (min 1) and `ceil(level/2)` emeralds (min 1). Costs add for simultaneous enchantments, and the screen shows the total. XP is charged to the operating player; lapis and emeralds come from the printer's ingredient slots. Enchantments print at their learned level, but they gradually strain the stored imprint: the chance to **run out after a successful print** starts at 4%, increases by 7 percentage points per successful use and tops out at 85%. Each imprint tests independently. Running out disables that enchantment for **2 minutes** (2,400 ticks); simply waiting restores it, or feeding an enchanted item/book carrying that enchantment immediately refills and resets its strain. The printer does not destroy an already enchanted target when it runs out after printing.

The GUI displays the library, selected preset, each enchantment's current run-out percentage or cooldown, and total XP/lapis/emerald cost. The database, selected preset, levels, strain and blocked time survive world reloads. **Breaking the block currently returns its contents but not its learned library; relocate cautiously.** The printer does not duplicate incompatible enchantments or bypass normal supported-item rules.

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

## Pallet Transfer Node two-Golem pushers (0.1.4)

Pallets Plus 0.5.0 uses exactly two independently walking and persistent warehouse Golems to push a staged Pallet Transfer Node pallet onto an adjacent eligible train flatbed or conveyor carrier, or to unload a parked vehicle onto an INPUT node. Workers move to different service locations and transfer the real pallet without an item duplicate, then return to their original shelves. A node without adjacent transport never dispatches this crew. The existing four-Golem INPUT pickup has no replacement pallet; normal four-Golem REMOTE pickup still sends one replacement-pallet courier. This branch includes the latest mainline forklift fixes.

The two-Golem OUTPUT push mission now begins at a loaded glass-wrapped pallet in a reachable ground-level **Output aisle rack**. Both workers walk to its service aisle; after the existing 40-tick rack-pull animation, the lead Golem owns all 216 inventory slots. Together they push the original pallet to the transfer node and transfer sole ownership to the adjacent empty train flatbed or conveyor carrier. If the transport leaves, the loaded team waits with its original cargo. No spare pallet or four-Golem crew is used for this job.
