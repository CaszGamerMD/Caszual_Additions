# Pallet transport — feature branch

Target: Fabric 26.2. This branch adds transport without merging the feature into main.

## Equipment

- **Conveyors:** 3×3 module kits, three blocks wide, straight, left/right turn, and one-block rise/fall over three blocks. Place module centers three blocks apart. Face the direction of entry; a turn changes the exit by 90 degrees. Ramp exits connect one block higher/lower.
- **Warehouse rail:** separate three-wide rail modules, straight and left/right turns. These vehicles do not use vanilla minecart rails. Allow generous clearance for long cars around corners.
- **Train:** an engine, a flatbed holding three full pallets in a row, and a passenger carriage. Cars retain their cargo and couplings across saves.
- **Drone:** carries one pallet beneath a four-rotor frame. Deploy it on a complete 3×3 parking bay. Link a second bay within 128 blocks, in the same dimension.
- **Transport Controller:** selects pallets/carriers, transfers cargo, couples cars, and links drone destinations. Available with the equipment in the Caszual creative tab and Fun Barrel tag; each item has its own recipe.

## Loading and unloading

1. Wrap a ground pallet using the existing pallet wrapper.
2. Click the pallet with the Transport Controller, then click a conveyor module or a stopped freight car/drone.
3. Empty-hand click a carrier to pause/start. Wait a moment after stopping.
4. Select the carrier with the controller, then click supported ground with a clear 2×2 footprint and three blocks of headroom to unload the first pallet. The destination must be within 12 blocks.
5. To hand off between transport types, select the loaded carrier and then another stopped carrier with a free slot.

All 216 inventory slots, four wood selections/plastic tint, filter settings, and wrapping are retained per pallet. A destroyed carrier drops its pallet decks and contents once. Breaking any part of a guideway removes the module as one unit.

## Trains

Place an engine and cars on a continuous straight rail stretch. Leave six blocks between an engine and its first flatbed center, or nine between long flatbeds when coupling. Select the leading vehicle with the controller, then sneak-click the following car. Repeat from front to back. Empty-hand click the engine to start or pause. Empty-hand click a passenger car to board; use the normal dismount key to leave.

Pause the engine before loading or uncoupling. Sneak-click a stopped car with an empty hand to uncouple; sneak-click an empty, uncoupled vehicle again to pick it up. Redstone at a module center stops the engine or conveyor pallet.

## Drone trips

Deploy on the home bay, load a pallet, select the drone with the controller, then click the destination bay. Empty-hand click the drone to depart. It climbs seven blocks above the higher bay, travels horizontally, and lands. It stops at arrival for unloading/loading; another click sends it back. The flight corridor must be clear and loaded. Blocked or unloaded paths retain the payload and wait. This version does not load chunks, route around buildings, or automatically load/unload pallets from warehouse racks/PTPs.

## Validation

`TransportPathTest` checks rotated module joins, quarter-circle geometry, and ramp elevation continuity. `scripts/transport_assets_smoke.py` checks model coordinates, state/model references, item models, and distinct recipes. `scripts/preview_transport_models.py` renders the actual repository geometry with flat material swatches for inspection; it is not an in-game screenshot. Interactive client validation is still required for boarding, multiplayer motion, and full transport journeys.
