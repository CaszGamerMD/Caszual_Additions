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


## Selecting a rack destination and diagnosing refused jobs

The **Warehouse Scanner (FORKLIFT mode)** and **black Forklift Controller**
both support a two-click transfer: click the source's wrapped pallet, then
click a *different empty rack* to deliver it. In the scanner, sneak-right-click
a wrapped pallet to change the selected source, or sneak-click an ordinary
block to clear a stale source. With the black controller, sneak-right-click
a loaded rack to reselect or an empty rack to clear the source.

**One rack is one 2×2×3 multiblock.** All twelve rack frame blocks share the
same pallet and 216 inventory slots. Clicking a visually empty top or lower
frame section of the same rack is *not* choosing a separate destination.
Messages now show the real **root coordinates**, so you can verify the source
and destination are distinct.

An empty-looking rack is not automatically an empty inventory. If a rack
actually contains a saved pallet, or still has stored items left behind
without a deck, delivery is refused **without deleting any cargo**. The
message will report the root coordinates and stored stack count.

A forklift can also carry a **packed Pallet Jack with no visible pallet**
if older/interrupted data has stored contents but no valid display deck.
Such a vehicle is excluded from new pickups. Complete the previous delivery,
or follow the emergency driver / Pallet Jack recovery procedure above.
The diagnostic message distinguishes source and destination access clearance,
missing wrapping, unexpected contents, absent driver, or loaded internal cargo.


## 3-block aisle routing and true-to-life fork alignment

A forklift no longer aims its motor straight toward a destination pallet
when an obstacle prevents the direct route. Its server-side motor follows an
**A* aisle path** that checks the full 1.2-block chassis every half block,
verifies support under all four corners, turns at aisle intersections and
replans if a new obstacle appears. The player/warehouse does not need to
place special path markers. An impossible or unloaded route causes a clear
"NO SAFE FORKLIFT AISLE ROUTE TO DOCK" status, rather than a forklift
forcing itself into a rack.

At pickup and drop-off the forklift steers to a precise position **2.82 blocks
back from the center of the 2x2 pallet**. Its wheels stop square to the rack
opening, and its two physical fork tines run underneath the whole pallet.
It rotates in place to face the pallet **perpendicularly** before raising or
lowering, so its mast no longer points diagonally at one rack corner. A
ground pallet can be approached from any genuinely reachable side; the
nearest usable aisle route is preferred.

The traveling forks stay low, mast lift is limited to the destination/source
transfer itself, and the safe driver-dismissal and packed-cargo recovery
features remain available.


## Detailed forklift model and locked driver seat (visual redesign)

The forklift has a revised industrial Minecraft-style model: structural steel
chassis, protective lower rails, layered yellow counterweight, shaped wheel
arches, work platform and pedals, real seat cushions and backrest, armrests,
grabbable overhead guard, canopy braces, steering wheel, operator levers,
recessed lamp clusters, safety beacon, rear reflectors, and a reinforced
telescoping mast with visible hydraulic cylinders.

The **driver's seat** is one shared coordinate: model X=0, Z=+8 pixels
(0.5 block behind the chassis origin) and its cushion is 10 pixels high
(0.625 block above the floor). The Golem's riding position is calculated
from this same anchor and is rotated with the current forklift yaw on both
server and client. The Golem's body, head and steering pose rotate along
with the forklift, including turns. It does not use the old arbitrary
0.28-block sideways offset.

Front wheels visibly steer and the steering wheel responds to the same
network-synchronized angle. All four wheels retain travel spin, the middle
and upper mast sections telescope independently, the fork carriage follows
the commanded height, and both hydraulic cylinders animate as it lifts.

Only appearance, the riding anchor and pose synchronization change. The
same 1.2-block server collision box, 3-block aisle router, 4 m/s motor,
full-size cargo on its tines, saved pallet inventory, warehouse integrations
and emergency driver recovery remain unchanged.

The 128x128 existing forklift texture is retained so current installations
do not need texture migration. Existing saved forklifts update visually when
the JAR is replaced.


## Smooth forklift driving and visual cleanup

Forklift aisle routing still uses the half-block A* collision grid, but its
wheel motor now follows **longer collision-tested straight segments** rather
than stopping to steer at each tiny waypoint. The chassis can steer and roll
at the same time when it is already pointed along the lane; it slows down
at moderate turns and rotates in place only at sharp corners. Speed ramps up
gently to the existing **0.20 blocks/tick (4 blocks/second)** ceiling.
Lookahead tests sample the *full vehicle collision box and all wheel-support
positions* every 0.18 block so turning never becomes an excuse to cut
through a rack or pallet.

The Golem and cushion are both centered at **X=0, Z=10 model pixels**.
This is two pixels farther toward the rear than the previous shared
seat center; the height stays 10 pixels. The golem's position and yaw
continue rotating with the truck, so turning doesn't drift the driver
out of the cushion.

The chassis underside, cab floor, seat cushion/foundation, rooftop rails,
warning beacon and rear counterweight have had overlapping external faces
separated or removed. These cosmetic corrections do not change warehouse
clearance, forklift inventory, docking geometry, parking or driver rescue.

**Regression check:** drive a long straight three-block aisle followed
by a 90-degree turn, watch from third-person side and rear views, and
verify the golem stays on the seat while the carriage and roof remain free
of visual flicker.


## Stable-segment motor update

The forklift now generates **a fixed list of safe route segments** when
a warehouse job begins. It follows each segment completely before turning
toward the next one. The route is calculated from the same A* aisle path as
before, and each omitted intermediate point is checked with the entire
vehicle's collision box, so corners and three-block aisles remain protected.

It no longer sends a conflicting zero-speed instruction to vanilla mob
MoveControl every driving tick. Motor acceleration reaches the existing
0.20 blocks/tick limit over four driving ticks on a clear straight road.
True blockages can still cause a controlled stop and route recalculation.

**Measured-speed diagnostics:** Right-click the forklift with an empty hand
(without sneaking). The returned text contains the actual measured
blocks/second over the last 20 server ticks, the current route segment, and
the current heading error. For example, a clear aisle should approach
`measured 4.0 blocks/s` at 20 TPS. A server running below 20 TPS will
appear slower to the player even if the per-tick motion cap is reached.
If movement is still poor, report this exact message and whether the aisle
is straight, near a rack corner, or near the final docking position.


## Safe raised-fork rack entry and curved 90-degree withdrawal

Previously the forklift sometimes lowered a retrieved pallet while its tines
were still inside a rack, or raised the carriage only after driving into
the target bay. Rack loading now uses a staged, physical process:

1. Find a **real supported staging position outside the rack**, using a
   clear straight withdrawal or a gradual **left/right 90-degree backing
   arc** through the working aisle. Validate wheel clearance and a full
   1.8-block-wide, three-block-tall pallet/fork envelope throughout the arc.
2. Approach that exterior position with **lowered forks**, turn to align with
   the entry tangent, and **raise the mast outside the rack** to the exact
   shelf height.
3. Enter on the collision-checked arc, square with the pallet center, and
   lift/place the original wrapped pallet with its complete 216 slots.
4. **Back out along the verified arc with the mast still raised.** Forks
   rotate gradually as the forklift reverses. Lower them only when the
   envelope has left *all* surrounding rack blocks.
5. Continue to the next destination, lower for normal aisle driving, or
   park as usual.

If the opposite rack row, a wall, a pallet or a blocked aisle leaves no
safe straight or curved exit, the vehicle waits with its mast and original
cargo intact. It must not teleport, move through racks, or release a pallet
halfway through an obstruction. The chosen exit side, arc depth and progress
are saved with the forklift so a chunk unload does not invent a different
turn midway through the maneuver.

**In-game verification:** face two rack banks across a three-block aisle,
retrieve a pallet from an elevated shelf, and watch the forks at the aisle's
end and through a 90-degree turn. Check a pickup, a placement and a
rack-to-rack transfer, including server restart halfway through withdrawal.
Some layouts cannot safely accommodate a forklift's real fork length and
may need a free turn direction or an open aisle end.

**Recall during a raised-rack operation:** A normal scanner/controller
recall is queued while the forks are inserted or withdrawing. The current
transfer finishes, its tines back out on the checked arc and the carriage
lowers only after it is safely outside. The forklift then returns to its
saved parking bay and dismisses its Golem driver. This avoids the previous
unsafe instant switch to the parking state. If the aisle is genuinely
blocked, resolve the obstruction; a normal recall cannot skip physical
withdrawal. Emergency dismount remains a separate deliberate override.
