# Pallets

[← Home](Home.md) · [Recipes](Recipes.md)

Pallets are **2×2 storage platforms**. Each has **216 inventory slots**, equivalent to **eight single chests**. Clicking any section opens the shared inventory.

## Types

| Pallet | Special behavior |
| --- | --- |
| Wooden | Appearance inherits each of four corner planks; mix wood types for a patchwork pallet |
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

## Visible items versus inventory

Pallets do not render all 216 stored stacks.

- Up to **63 nonempty inventory stacks** are displayed, in a **3 × 3 × 7** arrangement over the 2×2 footprint.
- Models are reduced and spaced out, intended to remain under **five blocks high** for ordinary-sized item models.
- Once all display positions are occupied, newly stored items remain in the inventory but are **not** rendered on top.
- When a visible stack is removed, the next stored nonempty stack takes the freed display position.
- All 216 slots remain usable regardless of visible item count.

Unusually oversized modded item models can exceed standard display bounds and may require visual adjustment.

## Breaking and moving

Breaking the pallet drops the pallet item and the stored contents in Survival. Its wooden material combination and plastic tint are retained in the pallet item. A pallet needs a clear **2×2** area for placement.

[Source: pallet classes](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/pallet)
