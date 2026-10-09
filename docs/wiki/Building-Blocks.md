# Building Blocks, Quarter Blocks & Colored End Rods

[← Home](Home.md) · [Recipes](Recipes.md)

## Generic quarter blocks

The **Quarter Block** is a small, independently placeable building piece.

**Texture customization:** craft a quarter-block item together with a block item to get **eight** quarter pieces using that block's default appearance. The matching custom recipe accepts many ordinary block items, including modded blocks.

Individual corners can be placed independently, including diagonally opposite corners. In Creative, breaking a targeted quarter removes that section rather than clearing the whole block.

## Colored end rods

The Caszual Additions colored-end-rod collection includes end rods for all **16 dye colors**, plus an **RGB end rod** with animated coloring. To recolor a compatible end rod, combine it with the appropriate dye.

Craft the RGB end rod with a **red end rod + green end rod + blue end rod** (shapeless): produces **three RGB end rods**.

Dynamic emissive colors depend on your shader and supporting shader configuration; ordinary light-level mechanics cannot natively produce true colored block lighting. See the [shader integration notes](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/docs/colorful-rods-shader-integration).

## RGB building blocks

The RGB family currently registers:

| | | |
| --- | --- | --- |
| RGB Block | RGB Slab | RGB Stairs |
| RGB Wall | RGB Fence | RGB Fence Gate |
| RGB Door | RGB Trapdoor | RGB Bars |
| RGB Carpet | RGB Wallpaper | RGB Quarter Block |
| RGB Vertical Slab | RGB Vertical Stairs | |

RGB wallpaper supports walls, floors, and ceilings. Vertical slabs and stairs are placed relative to the face clicked; they are not the same as ordinary horizontal slabs/stairs.

### Selected recipes

- **RGB Block:** fill the entire 3×3 crafting grid with RGB end rods.
- **RGB Quarter Blocks ×8:** one RGB block (shapeless).
- **RGB Vertical Slabs ×6:** three RGB blocks in a vertical column.
- **RGB Wallpaper ×8:** one RGB block + one paper (shapeless).

See [Recipes](Recipes.md) for the rest of the crafting JSON reference.

[Generic quarter block source](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/quarter) · [Colored end rod source](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/rods)
