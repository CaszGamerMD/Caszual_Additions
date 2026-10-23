# Pallets

[← Home](Home.md) · [Recipes](Recipes.md)

Pallets are **2×2 storage platforms**. Each has **216 inventory slots**, equivalent to **eight single chests**. Clicking any section opens the shared inventory.

## Types

| Pallet | Special behavior |
| --- | --- |
| Wooden | Four recipe-corner planks select long top boards and different under-deck runners; mixed colors extend across the entire 2×2 model |
| Plastic | Craft from purpur blocks and dye the completed pallet in a shapeless recipe |
| Iron | Iron-block appearance |
| Copper | Ages through regular, exposed, weathered, and oxidized appearances |

## Crafting

Every base pallet uses **five chests** and **four material blocks**, arranged with material in the four corners and chests in the other five cells.

```text
M C M
C C C
M C M
```

- **Wood:** M = any plank block; the four corners may differ.
- **Plastic:** M = purpur block.
- **Iron:** M = iron block.
- **Copper:** M = copper block.

**Recolor plastic:** plastic pallet + a dye (shapeless) → recolored plastic pallet.

## Placing a pallet: home corner and auto-rotation

The block you click becomes the pallet's **home corner** (the closest left corner from the player's perspective). From that square, the 2×2 pallet extends:

- **One block to your right**
- **One block forward**
- **One block diagonally forward-right**

For example, while looking **north**, the home block is the **southwest** corner and the other three squares are east, north, and northeast.

If that footprint is blocked, placement automatically tries rotating **90° clockwise**, then **180°**, then **270°**. The clicked home block **never moves**, even when the pallet rotates. All four squares must be replaceable and within the world bounds. If none of the four orientations fits, **no pallet is placed**.

All pallet materials, the 216-slot shared inventory, long mixed-wood deck boards, and copper weathering follow the chosen orientation. Pallets placed before this update retain their old east/south layout and existing inventory.

## Visible items versus inventory

Pallets retain **216 inventory slots** but display up to **27 nonempty
stacks** in a compact **3 × 3 × 3** cargo grid. Each visible cell is
2/3 of a block across, with a top height of about 2.3 blocks (plus the
pallet deck). The first nonempty stack occupies the lowest available
display position, and extra inventory stacks stay securely stored but hidden.

To prevent overlapping or floating models, pallet cargo has two visual
styles:

- **Full-cube blocks** (for example stone, logs, full glass blocks) appear
  as recognizable Minecraft block models stacked on the pallet.
- **Non-full blocks and other items** (such as signs, doors, trapdoors,
  carpets, slabs, stairs, fences, walls, buttons, torches, and modded
  decorative shapes) appear inside **kraft cardboard cartons** with
  the item's icon on the front. Their world block models are never drawn
  loose among the crates.

The same rule applies to a **placed pallet** and to a **pallet displayed
inside a warehouse rack**. The forklift and four-Golem carriers already use
cartons for their carried cargo.

The entire display is cosmetic: neither boxing nor unboxing an item changes
the actual stored ItemStack, its components, filtering, storage capacity, or
the way a pallet is moved. Up to 27 visible occupied cells have corresponding
cargo selection/collision spaces, and a ceiling can reduce the visible tiers.

## Breaking and moving

Breaking the pallet drops the pallet item and the stored contents in Survival. Its wooden material combination and plastic tint are retained in the pallet item. A pallet needs a clear **2×2** area for placement.

[Source: pallet classes](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/pallet)
