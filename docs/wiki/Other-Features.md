# Other Features

[← Home](Home.md) · [Recipes](Recipes.md)

## Player Head Vending Machine

The head vending machine is a **1 block wide × 1 block deep × 2 blocks tall** appliance. Its two halves form one unit: place it with a clear block above, right-click either half to open the UI, and breaking one half removes the whole machine.

The custom 3D model includes a large player-head screen, three category indicators, a chest-styled head-dispenser drawer, green emerald payment slot, reinforced steel casing, side vents, and cyan accent lighting.

The vending machine now has five tabs:

1. **Custom Heads** — search a remote decorative-head catalog by name, category and keywords, view previews, and browse pages.
2. **Player Heads** — enter a Minecraft username for that player's head.
3. **Mob Heads** — browse vanilla mobs with search and pages. Existing vanilla skulls are dispensed directly; mobs without vanilla skulls are resolved by a matching HeadDB decorative texture, which may not be available for every name.
4. **Favorites** — star a custom, player, or mob head to save it for later. Favorites are stored per player, not per machine.
5. **History** — shows up to 30 recently purchased heads per player; purchase a previously generated head again from this tab.

In **Survival**, each purchase costs **one emerald**. **Creative-mode players receive heads for free**. The server validates the machine, range, catalog entries, and any required emerald payment. The custom-head catalog is cached, and external data availability depends on the third-party source.

**Recipe**

```text
I H I
I C I
I E I
```

I = iron ingot, H = player head, C = chest, E = emerald.

[Head database details](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/docs/head-vending-custom-heads.md) · [3D model and Blockbench editing guide](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/docs/blockbench/player-head-vending-machine.md)

## Player Plushie

A decorative plushie that retains the identity/texture data of the player head used to craft it.

**Right-click with an empty hand to cycle 12 poses:** standing, sitting, reading an enchanted book, sword, axe, pickaxe, hoe, searching with a spyglass, running, sleeping, waving, and crying. The pose is stored in the block entity, synchronized to clients, and retained when the plushie is picked up.

**Recipe:** eight **white wool** blocks + one **player head**, anywhere in a 3×3 grid (shapeless custom recipe; all nine positions are used). The placed block retains the profile and drops a matching plushie.

## Size Snacks

Food items control the player scale:

| Item | Effect | Crafting (shapeless; yields 4) |
| --- | --- | --- |
| **Shortcakes** | Decrease to the next smaller size | Sweet berries + wheat + sugar |
| **Stack o' Jacks** | Increase to the next larger size | Wheat + sugar + egg |
| **Vanilla Wafers** | Reset to standard size (1.0×) | Wheat + sugar + milk bucket |

Scale stages: **0.125×, 0.25×, 0.5×, 1×, 1.25×, 1.5×, 2×, 2.5×**.

## Firefly Glass Boxes

Decorative glass enclosures can surround firefly bushes and change the visual tint of emitted particles. There is a basic clear version and dye-color variants.

**Clear Firefly Box:**

```text
G G G
G   G
```

G = glass (two-row shaped recipe).

**Color variations:** the clear firefly box + the matching dye produces a dyed box. The resource recipe set also includes direct color-specific crafting recipes.

## Unbreakable Anvil and Book

**Unbreakable Anvil:** nine regular anvils arranged in a 3×3 grid → one unbreakable anvil.

**Unbreakable Book:** surround a book with **eight unbreakable anvils** → one book.

The unbreakable item and anvil behavior is defined in the mod's custom logic.

[Recipe list](Recipes.md) · [Source tree](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java)
