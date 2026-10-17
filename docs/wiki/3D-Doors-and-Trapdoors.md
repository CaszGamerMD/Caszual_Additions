# Real 3D doors and trapdoors (experimental)

Implemented client-side in branch `feature/voxel-depth-doors-trapdoors`.

## What it does

Normal door and trapdoor models are thin cuboids with transparent pixels on the broad faces. This can make their grilles, holes and windows look painted onto a flat sheet.

This feature reads the **current resource-pack textures**, identifies opaque pixels, merges them into rectangular areas and extrudes those areas through the original 3-pixel-thick panel. The newly exposed inner edges are textured as well, so you can see the thickness when looking through holes.

The block **still occupies the normal Minecraft geometry**. It does not change collision, harvesting, hinge side, facing, redstone behavior, opening, sound or world save data.

## Coverage

- Vanilla doors (including iron and copper variants)
- Vanilla trapdoors, open, top and bottom
- Caszual RGB door and trapdoor
- Third-party blocks extending `DoorBlock` / `TrapDoorBlock` with the standard vanilla door model templates
- Texture packs: geometry is regenerated from the active texture on resource reload (`F3+T`)

Nonstandard modded geometries (sliding doors, multiblocks, custom renderers, different templates) are deliberately not overwritten; those need a separate compatibility handler.

### Technical notes

- Visual-only (client). Servers do not require custom data or model files.
- Opacity threshold: alpha >= 32/255.
- The first frame of an animated texture determines the stable silhouette; color animation still comes from the original texture.
- Same solid areas are merged into fewer cuboids rather than 256 individual cubes.
- Each model is generated once per resource reload, not per block or render frame.
- Existing item icons are unchanged.
- The renderer never stacks new geometry on top of the original model; it replaces the vanilla element definitions to avoid coplanar faces.

## Test plan

1. Load world and check the latest log line `Generated depth-aware models for ...`.
2. Place oak, spruce, bamboo, iron, copper, cherry and mangrove doors. Check both sides, both hinge directions and open/closed states.
3. Place oak, iron and copper trapdoors facing each compass direction; check top/bottom and open/closed.
4. Look at the cutouts from a shallow angle to confirm the holes reveal 3D edges instead of flat alpha cutouts.
5. Check the RGB door/trapdoor texture is still animated and aligned; test both inventory icons and placed blocks.
6. Enable a texture pack with different door opacity; reload resources, confirm geometry updates.
7. With third-party door mods installed, check conventional and unconventional models. Unusual models should remain unchanged.
8. Compare screenshot and FPS with feature on/off (use branch vs `main`), especially in buildings with many doors.

Status: requires in-game visual confirmation before merging.
