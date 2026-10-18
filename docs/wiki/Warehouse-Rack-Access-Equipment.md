# Warehouse Rack Access Equipment (Fabric 26.2)

Three rack-focused access tools with gray industrial steel models, available in
the **Caszual Additions** creative tab and the Caszual content tag.

## Rack Stairs

Modeled on wheeled warehouse mobile maintenance stairs, this is a single
**three-block-long / three-block-high**, multi-block stair unit. Each
ascending block has two half-block treads with steel side rails.

- Place on a solid floor, facing the raised rack bay. The unit consumes a
  1-wide by 3-long by 3-high area (one inventory item).
- With an empty hand, right-click a stair segment to **push it one block**
  in the direction you are looking. Pushing it saves that position as
  its home. Sneak-right-click to explicitly save its current home.
- If anyone is using the steps, or the next space is occupied, the unit
  cannot move. **Golems must not push an occupied stair.**
- When working with an elevated rack, a warehouse Golem can request an
  available stair unit facing the rack. A **different** Golem from a
  stocked shelf is assigned as the mover. The stairs advance one block
  at a time under escort; they never teleport between positions.
- When work completes, the mover escorts the stairs back to the last
  player-assigned position, then returns to its own shelf.
- If the stairs occupy the pallet service aisle, pallet handling waits
  for the access area to become clear.

## Rack Ladder

A narrow, steel-rung ladder. It attaches to rack-frame blocks even though
their skeletal faces are not solid. Use the same climbing control as a
vanilla ladder; step sideways or backwards to exit. The block is in the
Minecraft climbable tag, not a custom teleport/elevator.

## Rack Scaffolding

An elevated work platform for safe foot traffic beside high rack bays.

- A steel mesh deck, with **safety railings on the exposed edges**.
- The side facing the pallet service opening is left open.
- Adjacent walkway sections have no **collision** rail between them, so
  players and Golems can cross from section to section.
- Right-click to fold/unfold the platform and its railings.
- Before a pallet moves in or out, any scaffolding crossing its service
  aisle folds away. If an entity is on the platform, the transfer
  **waits**, rather than retracting underneath them.
- After the transfer, platforms unfold. If somebody is still in the
  way, the re-extension is deferred until the walkway is clear.

## Safety and testing

The implementation is designed to preserve loaded pallets and avoid
crushing riders. The multi-block stair escorts, scaffold rendering,
high-elevation approach routing and ladder dismounts need verification
with real players and Golems in Minecraft. Automated tests cannot
simulate full in-game pathfinding or animations.
