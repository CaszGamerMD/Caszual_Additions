# Building Blocks, Quarter Blocks & Colored End Rods

[← Home](Home.md) · [Recipes](Recipes.md)

## Generic quarter blocks

The **Quarter Block** is a small, independently placeable building piece.

**Texture customization:** craft a quarter-block item together with a block item to get **eight** quarter pieces using that block's default appearance. The matching custom recipe accepts many ordinary block items, including modded blocks.

Individual corners can be placed independently, including diagonally opposite corners. In both **Survival and Creative**, breaking a baby block removes just the targeted quarter without disturbing the other seven. In Survival, the removed quarter drops its matching textured quarter-block item.

### Boink'r hammer

The **Boink'r** is a two-ended red hammer with a yellow handle. Craft it using two **red wool** and three **sticks**:

```text
W S W
  S
  S
```

Hold the Boink'r and **sneak + right-click** (in the air or on a block) to cycle its modes; the active mode is shown in the action bar.

| Mode | Action | Effect |
| --- | --- | --- |
| Single | Mine a baby block | Remove exactly one aimed quarter (normal mining works the same way) |
| Group | Mine a baby block | Remove all occupied quarters in that same 1×1×1 block space; no adjacent blocks are touched |
| Boink! | Right-click an ordinary solid full block | Convert it into eight matching quarter blocks in place |
| Boink! | Right-click eight matching quarters filling one block space | Recombine them into their original full block |

Only solid full-cube blocks without block-entity data can be split. Inventories, fluids, partially shaped blocks and unbreakable blocks cannot be split. Recombining requires eight occupied corners of the same material. Neither conversion consumes or drops extra items.



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
