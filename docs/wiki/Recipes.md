# Recipes — Caszual Additions

[← Home](Home.md) · [Pallets](Pallets.md) · [XP System](XP-System.md)

The crafting reference below is drawn from the `main` branch's data-pack recipe files. It currently indexes **83 recipes**, including shaped, shapeless, and custom recipes.

## Illustrated by grids

### Pallet (wood / iron / copper / plastic)

```text
M C M
C C C
M C M
```

**C** = chest, **M** = any plank for wooden pallets, iron block for iron, copper block for copper, or purpur block for plastic. The wooden version can mix four plank types.

### XP Tank

```text
I G I
G B G
I G I
```

I = iron ingot; G = glass; B = experience bottle.

### XP Charger

```text
I L I
E T E
I R I
```

I = iron ingot; L = lapis lazuli; E = experience bottle; T = enchanting table; R = redstone.

### XP Drain

```text
I H I
G B G
I R I
```

I = iron ingot; H = hopper; G = glass; B = experience bottle; R = redstone.

### XP Shower

```text
I I I
  E
  I
```

I = iron ingot; E = experience bottle.

### Player Head Vending Machine

```text
I H I
I C I
I E I
```

I = iron ingot; H = player head; C = chest; E = emerald.

### Unbreakable Anvil / Book

- Unbreakable anvil: nine ordinary anvils in a full 3×3 grid.
- Unbreakable book: one book surrounded by eight unbreakable anvils.

### Custom crafting recipes

- **Wooden pallet:** custom 3×3 recipe with five chests and four planks (supports mixed plank types).
- **Plastic pallet dye:** plastic pallet + dye.
- **Player plushie:** eight white wool + player head (preserves head profile).
- **Quarter block:** one generic quarter piece + one block item → eight pieces with that texture.

### Caszual Additions food & guide

- **Shortcakes ×4:** sweet berries + wheat + sugar.
- **Stack o' Jacks ×4:** wheat + sugar + egg.
- **Vanilla Wafers ×4:** wheat + sugar + milk bucket.
- **Cosmetic Guide:** book + brush.
- **Clear Firefly Glass Box:** three glass across top row, two glass at outer ends of second row; colored boxes may be crafted or dyed.

### Caszual Additions colored rods

- Colored end rod: a compatible end rod + a dye of the desired color.
- RGB end rods ×3: red end rod + green end rod + blue end rod.
- RGB Block: nine RGB end rods.
- RGB Quarter Blocks ×8: RGB Block.
- RGB Vertical Slabs ×6: three RGB blocks vertically.
- RGB Wallpaper ×8: RGB Block + paper.

See individual feature pages for usage, not just crafting.

## Complete source recipe index

All **83** recipe files in the repository are listed below. Clicking an entry opens its actual JSON, including ingredient keys, output counts and patterns. This ensures recipes not printed as ASCII grids still have an exact, inspectable definition.

### Core Additions (14)

- [copper pallet](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/copper_pallet.json)
- [iron pallet](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/iron_pallet.json)
- [plastic pallet](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/plastic_pallet.json)
- [plastic pallet dye](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/plastic_pallet_dye.json)
- [player head vending machine](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/player_head_vending_machine.json)
- [player plushie](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/player_plushie.json)
- [quarter block](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/quarter_block.json)
- [unbreakable anvil](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/unbreakable_anvil.json)
- [unbreakable book](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/unbreakable_book.json)
- [wooden pallet](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/wooden_pallet.json)
- [xp charger](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/xp_charger.json)
- [xp drain](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/xp_drain.json)
- [xp shower](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/xp_shower.json)
- [xp tank](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/xp_tank.json)

### Caszual Additions — Cosmetics / Firefly Glass (37)

- [black firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/black_firefly_glass_box.json)
- [blue firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/blue_firefly_glass_box.json)
- [brown firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/brown_firefly_glass_box.json)
- [cosmetic guide](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/cosmetic_guide.json)
- [cyan firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/cyan_firefly_glass_box.json)
- [dye black firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_black_firefly_glass_box.json)
- [dye blue firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_blue_firefly_glass_box.json)
- [dye brown firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_brown_firefly_glass_box.json)
- [dye cyan firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_cyan_firefly_glass_box.json)
- [dye gray firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_gray_firefly_glass_box.json)
- [dye green firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_green_firefly_glass_box.json)
- [dye light blue firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_light_blue_firefly_glass_box.json)
- [dye light gray firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_light_gray_firefly_glass_box.json)
- [dye lime firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_lime_firefly_glass_box.json)
- [dye magenta firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_magenta_firefly_glass_box.json)
- [dye orange firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_orange_firefly_glass_box.json)
- [dye pink firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_pink_firefly_glass_box.json)
- [dye purple firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_purple_firefly_glass_box.json)
- [dye red firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_red_firefly_glass_box.json)
- [dye white firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_white_firefly_glass_box.json)
- [dye yellow firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/dye_yellow_firefly_glass_box.json)
- [firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/firefly_glass_box.json)
- [gray firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/gray_firefly_glass_box.json)
- [green firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/green_firefly_glass_box.json)
- [light blue firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/light_blue_firefly_glass_box.json)
- [light gray firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/light_gray_firefly_glass_box.json)
- [lime firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/lime_firefly_glass_box.json)
- [magenta firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/magenta_firefly_glass_box.json)
- [orange firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/orange_firefly_glass_box.json)
- [pink firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/pink_firefly_glass_box.json)
- [purple firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/purple_firefly_glass_box.json)
- [red firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/red_firefly_glass_box.json)
- [shortcakes](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/shortcakes.json)
- [stack o jacks](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/stack_o_jacks.json)
- [vanilla wafers](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/vanilla_wafers.json)
- [white firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/white_firefly_glass_box.json)
- [yellow firefly glass box](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/yellow_firefly_glass_box.json)

### Caszual Additions colored end rods & RGB building (32)

- [black end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/black_end_rod.json)
- [blue end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/blue_end_rod.json)
- [brown end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/brown_end_rod.json)
- [cyan end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/cyan_end_rod.json)
- [gray end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/gray_end_rod.json)
- [green end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/green_end_rod.json)
- [light blue end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/light_blue_end_rod.json)
- [light gray end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/light_gray_end_rod.json)
- [lime end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/lime_end_rod.json)
- [magenta end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/magenta_end_rod.json)
- [orange end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/orange_end_rod.json)
- [pink end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/pink_end_rod.json)
- [purple end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/purple_end_rod.json)
- [red end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/red_end_rod.json)
- [rgb bars](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_bars.json)
- [rgb block](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_block.json)
- [rgb block from quarters](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_block_from_quarters.json)
- [rgb carpet](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_carpet.json)
- [rgb door](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_door.json)
- [rgb end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_end_rod.json)
- [rgb fence](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_fence.json)
- [rgb fence gate](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_fence_gate.json)
- [rgb quarter block](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_quarter_block.json)
- [rgb slab](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_slab.json)
- [rgb stairs](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_stairs.json)
- [rgb trapdoor](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_trapdoor.json)
- [rgb vertical slab](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_vertical_slab.json)
- [rgb vertical stairs](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_vertical_stairs.json)
- [rgb wall](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_wall.json)
- [rgb wallpaper](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/rgb_wallpaper.json)
- [white end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/white_end_rod.json)
- [yellow end rod](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/src/main/resources/data/caszual_additions/recipe/yellow_end_rod.json)

## Notes

- A custom recipe JSON often specifies only a serializer ID; the real matching logic is in the corresponding Java recipe class. Feature pages describe the confirmed behavior.
- Third-party modpacks can add or override recipes using data packs.
- GitHub JSON links are source definitions, **not** a replacement for the in-game recipe viewer.
