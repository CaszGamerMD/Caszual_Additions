# Player Head Vending Machine: 1×1×2 model

The 26.2 player-head vending machine is a two-block-high structure using a one-block footprint. The default facing direction is north; the front face of the model is north.

The machine uses two authored vanilla-style block models:

- `player_head_vending_machine_bottom.json` — chassis, item drawer, emerald payment slot, vents and foot lighting
- `player_head_vending_machine_top.json` — screen, three category indicators, side emblem, top lighting and lid

The two models have **107 individual cuboids** combined, with steel corner pillars, inset faceplates, cyan indicators, a pixelated head display, a chest-style output drawer and green payment lights.

### Editing the model in Blockbench

1. Open Blockbench and choose the **Java Block/Item** workspace.
2. Import one model from `src/main/resources/assets/caszual_additions/models/block/` using *File → Import → Java Block/Item Model* or open its JSON directly.
3. Change or regroup elements. The `from` and `to` coordinates are each relative to their own 16×16×16 block.
4. Import or regenerate the textures from the build-generated resources folder `build/generated/vending-assets/assets/caszual_additions/textures/block/`.
5. Maintain front orientation (north), or update all `facing` rotations in the blockstate when changing it.
6. To preserve the functional two-block structure, keep both model filenames referenced by the blockstate.

Textures are generated from a small, self-contained Gradle task defined in `gradle/vending-textures.gradle` and copied into the mod JAR during `processResources`. No external image assets or Python packages are needed to build.

### Functional notes

- Place the item on the ground: the upper block is placed automatically when the area above is open.
- Right-click either half to open the same head-selection UI.
- Breaking either half removes the other half; the machine drops at most one item in Survival.
- Purchases remain **one emerald per head** and are validated by the server.
- Existing single-block installations from previous builds will require replacement with a newly placed two-block model. An orphaned half will remove itself when updated.

The textures are custom pixel artwork, not a screenshot or a shader-based glowing emissive map. The built-in light level makes the accent colors visible without relying on external shaders.
