# Warehouse Scanner and Computer (Fabric 26.2)

The **Warehouse Computer** remains the same `caszual_additions:rack_computer`
block for compatibility with saved worlds. Its coordinates identify a
warehouse, while a configurable name identifies it to players.

The Warehouse Computer GUI has a persistent **Warehouse Name** setting in
DEBUG. Rename it without replacing the block. Physical warehouse network
membership spreads through any face-connected compatible Rack, Warehouse
Shelf, PTP, IGC concrete, connector, Rack Stairs, Rack Ladder, Rack
Scaffolding and Forklift Parking tiles.

Right-click the Warehouse Computer with a scanner to **link or refresh** the
Scanner. The action bar confirms: `Scanner is now linked to <name>`.

Shift-right-click in air to rotate the modes:
- **REMOTE** – click a wrapped ground pallet to request an independent
  replacement crew from the linked warehouse. A returning crew prefers
  open ground-level Input aisle racks, then ordinary ground racks. If
  no rack is available it stays loaded and waits for a player to help. A
  returning remote crew does not silently put its pallet into a PTP.
- **DIRECT** – right-click a linked Golem Shelf for four actual workers,
  then pick a wrapped pallet on ground, PTP or accessible rack. Click
  a ground pad, PTP or rack to direct the delivery. For a stacked rack,
  a Golem-driven forklift from the linked warehouse is required: the
  four Golems lower the pallet to a clear nearby floor pad and return
  while the forklift collects the pallet and installs it at height.
  A stalled returning remote carrier can be claimed through a Direct
  Scanner if it is WAITING for warehouse placement. Click an assigned
  carrying rig to pause it safely with its pallet; clicking an unloaded
  direct Golem dismisses the independent team.
- **FORKLIFT** – click a linked Golem Shelf to call an available operator
  to follow. Click an empty forklift with the scanner to assign the
  operator; the Golem walks to the driver's seat. Choose a pallet on
  the ground or in a rack, then a ground pad or rack destination.
  Extended rack targeting remains 20 blocks through unobstructed view.
  One operator remains in the machine between orders. Right-click
  the seated operator with a Forklift-mode Scanner to request dismissal;
  if busy, they finish delivery, return to parking, and walk home.
  The old separate Forklift Clipboard remains registered for existing
  saved items but is hidden from the Caszual creative tab.
- **RECALL** – double-right-click the same carrying rig within 12 ticks.
  It chooses the nearest safe supported 2×2 ground pad, physically
  delivers its pallet (all 216 slots), then sends its four members home.
  When there is no safe floor, it stays holding the load and reports why.
  Double-clicking an ordinary empty Golem recalls it to its shelf.

The **Orders** page has a toggle for Builder Golem pending requests.
The server sends connected PTP reservations, the current outstanding
material type, next outstanding material type, and a manifest of all
remaining requested item quantities. These are snapshots of the
existing reservation system, not fictitious orders or a second queue.

## Forklift parking

Placing a forklift marks a real 2×2 floor bay. These shallow tiles have
no collision, but deny block placement while the forklift is away.
The forklift returns here after a job. Its four wheels are independent
animated parts, with wheel rotation centered on their own axles.

## Testing

Always test in a world with an empty ground rack, one stacked rack,
a linked Warehouse Computer, a Golem Shelf with docked workers,
a wrapped 2×2 pallet, and unobstructed 3-wide service aisles.
Verify drivers mount rather than remaining at their shelf doors,
pallet contents persist through chunk unload/restart, and the
Builder reservation details match the actual output PTP.
