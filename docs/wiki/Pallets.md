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

Pallets do not render all 216 stored stacks.

- Up to **128 nonempty inventory stacks** are displayed in **4 × 4 × 8** columns/layers across the 2×2 footprint.
- Block cargo uses real Minecraft world-block models scaled to **8 × 8 × 8 pixels**, with **1 pixel between adjacent cargo units**, horizontally and vertically. Four across requires a slight **1.5-pixel overhang** at the edges of the 2×2 pallet, and the final tier stays below five world blocks.
- Non-block items appear as compact kraft-cardboard boxes with the item's icon on the front. Block models are anchored at the deck rather than using the offset inventory-display transform, so they stack tightly without floating.
- The first nonempty stack fills the bottom tier. If a visible stack is removed, remaining visible stacks immediately pack down into the earliest available display positions.
- Once the 128 display positions are occupied, additional stacks stay in the full 216-slot inventory but are no longer rendered on the pallet.
- The pallet base is submitted **only by the shared controller block**. A single set of long deck boards and bottom runners now crosses all four blocks, without quarter-pallet seams.

Unusually oversized modded item models can exceed standard display bounds and may require visual adjustment.

## Breaking and moving

Breaking the pallet drops the pallet item and the stored contents in Survival. Its wooden material combination and plastic tint are retained in the pallet item. A pallet needs a clear **2×2** area for placement.

[Source: pallet classes](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/pallet)
