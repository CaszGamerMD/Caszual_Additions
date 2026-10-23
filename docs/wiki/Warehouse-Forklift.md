# Warehouse Forklift and Two-Block Sorting Reach (Fabric 26.2)

Storage Golems sort inventory at most **two blocks above/below their feet**.
A high rack must have an actual reachable landing at its working height,
reached by installed Rack Stairs, Rack Ladder, or Rack Scaffolding.
The Golem never extracts inventory through twelve blocks of air.

Four-Golem pallet crews load only ground racks. For stacked racks, use the
**Warehouse Forklift** (up to **12 blocks of mast range**, or 4 rack heights).

## Using the forklift

1. Craft the Warehouse Forklift and right-click a free solid floor to place it.
2. Keep at least one docked Storage Golem in a shelf within 20 blocks. A
   free Golem walks to and mounts the forklift as its actual driver.
3. Craft a reusable **Forklift Clipboard** (compass, redstone and iron).
   To designate an exact ground pallet for storage, right-click it with the
   Clipboard first. Then click the desired rack. Without a selection the
   nearest wrapped floor pallet is used.
4. With the Clipboard, aim and right-click an elevated rack. **Empty destination racks can be selected from up to 20 blocks away** using line of sight, even outside vanilla right-click range. Walls and other obstructing blocks prevent selection. The forklift's 12-block vertical lifting limit does not change:
   - **Occupied wrapped rack:** forklift drives to the aisle, raises forks,
     removes exactly one wrapped pallet with all 216 slots, and stages it on
     a clear 2×2 pallet-sized floor area nearby.
   - **Empty rack:** forklift finds the nearest wrapped pallet on the ground,
     collects it, drives to the rack, lifts it, and inserts all saved cargo.
5. Forklift waits instead of consuming/dropping a load if the target rack,
   mast clearance, service lane, staging floor, or driver is unavailable.
6. A docked Storage Golem **walks out when the first job is requested**,
   boards the forklift in the central operator seat (below the protective roof),
   with legs bent and arms on the steering wheel. The forklift itself moves
   to pickup and delivery locations; the driver stays seated throughout.
   The Golem remains mounted between multiple pallet jobs.
   Right-click the parked forklift **with the Forklift Clipboard** to dismiss
   its driver; the Golem returns to its original shelf. You cannot dismiss
   the Golem during an active cargo transfer. Use an empty hand on the
   forklift to inspect its status.
7. If destroyed, the forklift drops its intact cargo in a loaded Pallet Jack
   item and a new forklift item.

For delivery, Golem crews may stage a wrapped pallet in a ground aisle using
existing Scanner ground-placement controls. The forklift only touches pallets
when ordered; it never autonomously rearranges warehouse inventories.

**Physical requirements:** wrapped pallets only; clear 3-deep service aisle;
2×2 solid staging pad; no blocked aisle, rails or occupied mobile stairs.

**Testing needed in a live world:** Golem climbing ladder edges, scaffolding
occupancy, moving vehicle and rider collision, tall-mast rendering, and
interrupted/dead driver recovery. CI tests are useful but not a substitute
for physics and animation checks in Minecraft.

**Movement:** The forklift uses physical pathfinding and a collision-checked
engine fallback on clear level flooring. It never teleports through obstacles.
If the driving lane is blocked or a source/target chunk is unloaded, its status
says so and the loaded pallet stays safely in the forklift until the route
is available. Use an empty hand to inspect the forklift's status.


## Forklift operator and cargo behavior (0.1.3)

The Golem sits in the forklift cab about **0.7 blocks lower** than before,
facing the steering column. The vehicle travels at approximately **4 blocks
per second** on clear level ground (20 game ticks per second).

A loaded forklift visibly carries the **full 2×2 pallet**, its original
materials, and up to 27 representative cargo cartons enclosed in glass,
matching the four-Golem carried pallet display. The vehicle continues to
own the exact saved **216 cargo slots** in its Pallet Jack while in transit.
No dummy pallet is placed into the world while the forks are moving.

The forks **lower the pallet close to the ground for travel**. They rise to
the appropriate level only when lifting from a high source rack or after
arriving at the destination rack. The forklift must lower the forks again
before driving off with the pallet.

**Dismissing the driver:** use the Forklift Clipboard or Scanner to request
dismissal. If the forklift is away from its painted parking bay, it returns
there **with the Golem still seated**, parks, and *then* releases the worker
to walk back to its original Golem Shelf. A dismissal during a delivery is
queued until the job finishes. A missing saved parking bay will not cause
the Golem to abandon the forklift in the aisle.


## Recovery from blocked driving (0.1.4)

A forklift now moves along short **collision-checked** segments instead of
requiring a fully clear long ray all the way into its destination pallet.
The 4-block-per-second motor still cannot drive through racks or walls.

- **Normal recall:** With a linked Warehouse Scanner in **FORKLIFT** mode,
  right-click the forklift or its seated operator. This cancels an unfinished
  pickup/drop-off and sends the forklift back to its painted parking bay.
  The driver only gets out once the truck is parked, as normal.
- **Emergency dismount:** If the route to the parking bay is actually
  obstructed, **sneak-right-click the forklift with an empty hand**, or
  sneak-right-click the seated operator with the FORKLIFT-mode scanner.
  This stops the task where it stands and releases the driver to their shelf.
  It does **not** discard the carried pallet or force the vehicle through walls.
- **Recover onboard cargo:** After an emergency dismount, while no Golem is
  aboard, sneak-right-click the forklift **again with an empty hand**.
  The packed Pallet Jack, holding the complete original pallet and its
  contents, is returned to the player's inventory (or dropped beside them
  if the inventory is full). The forklift's copy is cleared exactly once.
  You can assign the forklift to a new job afterward.

An abandoned loaded Pallet Jack is never overwritten by a new pickup order.
If the forklift reports that its parking bay is missing, use emergency
dismount instead of repeatedly requesting normal parking.


## Three-block-wide aisles

Forklifts can now load from and deliver to racks on **both sides of a
three-block-clear aisle**. Their physical chassis is 1.2 blocks wide and stops
in the middle approach row (row 2), not against the opposite rack at row 3.
Only two clear approach rows are needed for a forklift to engage the mast.
The existing **three-row four-Golem** loading route is unchanged.

A forklift may set down a wrapped 2x2 pallet temporarily in an **actually
empty** portion of the three-block aisle, even when those floor tiles are
marked as rack-access reservations. The pallet still requires four replaceable
cells above full solid floor, and must never overwrite racks, cargo, or other
blocks. When servicing a rack, the forklift uses the rack's facing direction
for ground pallet placement and finds an approach along the aisle rather than
trying to drive through the opposite rack.

Aisles with actual obstructions, missing floors, or occupied 2x2 landing
footprints will still be refused; this change does not bypass collisions.
