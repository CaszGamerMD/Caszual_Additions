# Caszual Additions — Modular Pallet Warehouse (development)

Target: Minecraft Java **26.2 Fabric**. This document covers the current
implementation on the main branch. For a step-by-step end-to-end regression
of each system, see [Warehouse integrated test plan](warehouse-test-plan.md).

## Placeable Stack of Pallets and persistent Golem routes

- **Stack of Pallets is now a physical block:** place the existing item on
  the floor to create one linked warehouse pallet magazine. Each block holds
  up to **12 independent empty pallets**, preserving each stored pallet's
  material, dyes and components. It visually grows from zero through four
  stacked pallet layers as it fills. Right-click while holding an empty pallet
  to deposit it; sneak-right-click with an empty hand to retrieve one.
  Breaking the magazine normally drops a single Stack of Pallets item with
  its contents, not twelve scattered pallet items. The item can still be used
  as an older handheld/rack magazine for existing worlds.
- **Golem supply:** sorting Golems can bring empty pallets from INPUT PTPs
  to these placeable magazines. Remote exchanges prefer a placed stocked
  magazine, then a legacy magazine item in a rack, then an empty INPUT PTP.
  Item ownership rolls back on failed withdrawal or dispatch.
- **No more 20-second forgetting:** manual/sorting/order worker tasks keep
  their saved source, destination, phase and cargo when a path fails.
  Four-Golem remote carriers also keep their pickup/return assignment across
  a waypoint stall or world save/reload; the blocked waypoint is marked for
  a temporary detour, not used to cancel cargo custody. A physical blocked
  path still causes a WAIT message and automatic retries.
- **Y-level path priority:** Golems and their pallet platforms search a
  flat, cardinal warehouse aisle first. Only if a safe same-Y route cannot
  be found may an actual stair/ramp ascent or descent be chosen.
  They do not voluntarily climb over a rack/pallet shortcut.
  Long-distance carriers route in checked local 36-block legs and renew
  expiring chunk tickets ahead of their walking corridor.
- **Testing:** Verify travel around walls, chunk boundaries, different
  elevations with ramps, world reload during pickup/return, and that the
  exact cargo survives. Back up your world before first trying long trips.

## Warehouse GUI update — pallet, rack computer and Shulker Station

- **Pallet/rack/PTP storage:** The 216 real storage slots use two pages of
  18×6; the player's inventory stays below. The clipboard module has its own
  real slot beside the pallet, not over the last storage column. Only while
  a filter module is installed does a separate right-hand editor show 16
  non-consuming sample slots and whitelist/blacklist, target scope and
  apply/clear controls. No filter widget overlaps storage or the hotbar.
- **Rack Computer:** Select a rack in **RACKS & PTPs** to inspect its contents.
  The **HIGHLIGHT • 10s** button lights the loaded pallet with a bright
  rack perimeter visible to *every player* in the same dimension. Each new
  highlight replaces the previous one: there is only ONE at a time.
  Searching the network no longer highlights every search result.
  PTP controls remain on the rack page.
- **STORAGE GOLEMS:** **RECALL ALL** applies to every currently loaded
  walking Storage Golem assigned to the connected shelves, including manual
  escorts, sorting workers and order runners. Inventory travels back with
  the actual worker; loaded four-Golem pallet carriers receive a warehouse
  unloading order first rather than dropping their 216-slot load. Workers
  in unloaded chunks receive the durable recall from their assigned shelf
  when they next load (no forced chunk loading). RECALL ALL also pauses
  AUTO sorting, so newly returned workers stay shelved until resumed.
- **Shelf placement:** A Storage Golem Shelf's opening/spawn/check-in side is
  the face TOWARD the placing player (opposite the player's look direction).
  No worker should appear through its roof or rear.
- **Shulker Station:** Right-click the station to open its 64 individual
  empty-Shulker slots. It also displays three **ACTIVE** boxes and their
  three corresponding **READY** output buffers. Select **LANE 1/2/3** to
  inspect that working shulker's complete 27 inventory cells. Use
  **LOAD EMPTY** to move one physical box from the magazine to an empty lane.
  Drag items into/out of those cells as with a real container. Remove a
  finished box directly from its READY slot if desired. **SEND TO PALLET**
  sends the chosen working/finished box to a touching Hopper Adapter on
  an available OUTPUT PTP, retaining it in the READY buffer if placement
  is blocked. The selected box does not get duplicated into temporary UI
  storage.
- **PTP Hopper Adapter:** Hoppers whose output spout points into an adapter
  insert items into the actual pallet (including filtered pallets). A hopper
  **below** the adapter extracts items from that same pallet. This behavior
  is determined by the hopper's face, independently of the PTP's warehouse
  INPUT / OUTPUT sorting mode. Both directions reject sealed pallets; item
  filter restrictions still apply on insertion.

## Blocks and dimensions

| Block | Occupied space | Function |
| --- | --- | --- |
| Pallet | 2 × 2 × 3 display; 216 storage slots | Standalone storage (existing) |
| Pallet Rack | **2 × 2 × 3** | Holds one pallet and the pallet's 216 slots |
| Rack Computer | 1 × 1 × 1 | Search and order terminal for physically connected racks |
| Shulker Tank | **1 × 1 × 2** | Magazine of **64 EMPTY shulker boxes** |

All rack logic is server-authoritative. It doesn't load distant chunks. Only
loaded neighboring racks are included in a computer's graph, capped at 256
rack controllers.

### Building rack rows

1. Place a Pallet Rack with a **2 × 2 × 3** empty volume.
2. Keep one complete block of empty approach space **in front of each rack**,
   across its two-block width and three-block height. A rack, computer or
   shulker tank cannot be placed across an existing reserved corridor;
   physical horizontal connector rods may cross the lane intentionally.
3. Place adjacent rack bays **side by side** to extend the automatic aisle.
4. For separate rack groups, **click a rack's horizontal face with a
   Rack Connector** near a side corner. It places one slim rod at that top corner and automatically spans a clear
   horizontal gap of up to four blocks to the next rack. Side-to-side spans
   extend the same aisle tag; face-to-face spans join warehouse networks.
   Two directly touching back-to-back racks connect automatically.
5. Place the Rack Computer touching a physical rack cell. If multiple
   independent rack networks contact the computer, the computer can inspect
   both.

The front access lane is still available for right-click upgrades. If the
placement preview refuses to place a rack, check the three-block-high front
approach and any neighboring rack's access lane.

### Installing and removing a pallet

- Hold a pallet item and right-click any rack segment to install it. The
  rack uses the existing 216-slot pallet inventory UI.
- Right-click the rack empty-handed to open storage.
- Sneak-right-click with an empty hand to remove the pallet **only if empty**.
- Breaking a loaded rack drops its contents and the empty pallet item separately.
  **Always back up a test world before evaluating new storage mechanics.**
- The first 27 stored item stacks display as contiguous **3 × 3 × 3** cargo
  above the pallet deck, bounded by the rack frame. Visible items and exterior
  posts have matching physical collision.

### Pallet Filter upgrade (redesign Issue #34 — first pass)

The **Pallet Filter** is now a *real upgrade module*, installed in the
**FILTER** section of the Pallet Storage interface. Use the same storage
screen on a pallet placed on the ground, on a pallet mounted in a rack,
and on a connected OUTPUT Pallet Transfer Point.

1. Put a Pallet Filter in its small dedicated slot. Only one fits.
   Removing it **disables** filtering without deleting the rule.
2. Choose **Whitelist** (accept matches) or **Blacklist** (reject matches).
3. Cycle the target type: **Item**, **Whole Mod**, **Item Tag**, or
   **Exact Data**.
4. For an item, mod, or exact item with components, put a sample in your
   *offhand* and select **Use Offhand Sample**.
5. For item or block tags, type the registry ID (e.g.,
   `minecraft:logs`) and select **Apply Target**. For custom mods,
   type the mod namespace (e.g., `create`).

For **Item**, **Whole Mod**, and **Item Tag** scopes, the target field now
accepts up to **eight comma-separated entries**, e.g.
`minecraft:stone,minecraft:cobblestone`. Whitelist accepts items matching
**any** entry; blacklist rejects items matching **any** entry. The field is
limited to 96 characters. **Exact Data** remains one sampled item with
components rather than typed rules. Visual autocomplete and nested rule
groups are still future enhancements.

Exact Data compares a sample item's item ID and its data components. This
first pass uses a component signature and should be regression-tested across
save/reload and across servers with different mod configurations. Advanced
multi-rule expressions and visual autocomplete remain planned.

Existing simple filter IDs are automatically read as installed whitelist
filters; your existing contents are not discarded by this migration. Rack
storage can now also be opened with an ordinary item held in hand, while
holding an upgrade item continues to use that upgrade.

### Rack names, aisle names, and connectivity

- **Rack Tag:** Rename the item on an anvil, then right-click any part
  of a rack. It makes a **physical dark-steel sign on the rack front**.
  If the rack also belongs to a named aisle, its visible rack tag and the
  Rack Computer entry inherit the prefix **with no spaces**: aisle
  `A` + rack `12` displays **`A-12`**. Racks without Rack Tags remain
  unnumbered rather than acquiring an automatic `A-` prefix.
- **Aisle Tag:** Rename in an anvil and apply it to any rack in a connected
  aisle. Its amber **aisle signage appears on the outer SIDE ends of the
  aisle row**, not above individual racks' front signs. When the row grows,
  inner aisle signs disappear and the signs follow the exposed ends.
  The label is inherited along connected aisle neighbors, including racks
  tagged earlier; rack front labels update without reapplying their tags.
- **Existing labels:** Older rack names are automatically displayed as
  physical signs after loading the updated mod; they need no re-tagging.
- **Rack Connector:** This is now a **physical placeable steel top-corner
  rod block**. Place one to four consecutive sections horizontally between
  rack frame cells to join their networks across an actual gap. Each placed
  section has collision; breaking any section immediately disconnects the
  bridge. No vertical bridging is required. One connector placed against
  a rack automatically spans a clear gap to another rack up to four blocks away.
  The beam uses **one thin horizontal rod at the clicked top corner**, never a large X. Clicking a lower rack segment still anchors it at rack-top height. Breaking the
  beam disconnects the rack network.
- **Back-to-back racks:** Two touching rack frames with opposite front
  facings now connect to the same computer network automatically, without
  connector items. Adjacent side-by-side racks still share aisle tags.
- **Pallet Filter:** Use the real module slot and whitelist/blacklist controls
  in the pallet UI (detailed in the previous section). Rules and module state
  persist when pallets move into or out of a rack.

**Pallet ownership:** New 2×2 pallets use exactly one block entity on the
home corner, not four independent tile entities. The other three footprint
cells still reserve space and collision, and breaking any part removes the
whole pallet. All four pieces share the same 2×2 selection outline. Existing
saved inventory data remains on the home controller.


### Fitted rack pallets, workstation and orientation

A pallet **mounted inside a rack** is drawn four pixels smaller across
each horizontal dimension (two-pixel inset on all four edges), and its
cargo uses the same transform and matching server-side collision shapes.
Freestanding pallets and PTP pallets keep their original size.

The Rack Computer is a **player-facing kiosk**, with a pedestal, angled
monitor, keyboard and status lamp. It lists **all loaded attached PTPs**
touching the computer or its linked shelves. Choose **NEXT PTP** to cycle
locations; **SET INPUT / SET OUTPUT** changes mode, and **WRAP / UNWRAP**
controls installed wrap units. These operations are server-authorized and
refuse to interrupt pending output orders, active output reservations or
four-Golem pallet hauls. Only PTPs with a pallet and Wrap Unit can wrap.

Storage Golem Shelves, their visible docked worker models, and PTP
Hopper/Wrap modules now face the direction of the placing player. The
horizontal direction is saved with each block for reloads.

Matching racks use a private, bright twelve-edge outline for 10 seconds
after a Rack Computer search rather than server-broadcast END_ROD particles.
The outline is a thin illuminated block-model frame, not the vanilla
entity glowing effect.

## Unified warehouse wiring and Industrial Grade Concrete

**All warehouse blocks are network conductors**, including complete rack
frames, physical Rack Connector rods, Storage Golem Shelves, Shulker Tanks,
Pallet Transfer Points and Rack Computers. Touching block faces connect
these parts into one loaded-only warehouse network; existing rack bridging
still works.

**Industrial Grade Concrete (IGC)** is available in all sixteen Minecraft
concrete colors. Each matching-color shaped recipe yields **four IGC**:

```
C I C
I I I     C = concrete of the selected color
C I C     I = iron ingot
```

Build a continuous IGC surface physically touching a Rack Computer (including
directly underneath it). Every warehouse block touching that surface,
including one placed **on top**, joins the same network. Mixed IGC colors
connect. Removing a bridging concrete cell splits the network if no other
warehouse path connects it. Traversal is limited to loaded chunks and
16,384 inspected positions; it does not chunk-load distant equipment.

## Storage Golems workstation page

Use the new **STORAGE GOLEMS** tab at the top of the Rack Computer.
It displays shelf and docked counts, deployed Golem positions, each worker's
task and cargo, and following-player/pallet-carrying state.

Controls include **SORT: AUTO / PAUSED**, **RUN QUEUE** to request immediate
sorting/output dispatch, **ENLIST 4** to send four docked workers to follow
the player, and **DISMISS CREW** to send empty workers back to their shelves.
The page refreshes live while open. Worker commands are validated server-side,
require proximity to the workstation, and don't override carrying safety.
The **RACKS & PTPs** tab retains rack search, orders and wrapper settings.

## Rack Computer

Open the computer with an empty hand. It can:

- Search by item name, registry ID, aisle name, or rack name.
- Browse racks in all touching rack groups and connector-linked networks.
- See each rack's coordinates, occupied slots out of 216, stored-item count,
  and configured filter.
- Click a rack to inspect its aggregated contents.
- Mark matching rack locations for **10 seconds** with bright per-player
  outlines around the entire rack frame; no particle clouds.
- Click an item in a selected rack, type an order quantity (1–4096), and
  choose **REQUEST ITEMS**.

The computer must touch at least one rack to search. For orders, connect a
Storage Golem Shelf group and an **OUTPUT** Pallet Transfer Point (touching
either a shelf in the linked group or the Rack Computer). The terminal queues
up to 16 pending output orders on that PTP; requests can wait safely for an
unsealed output pallet or available stock. Requests without a connected
OUTPUT PTP are rejected; all orders are validated on the server.

## Warehouse order collection — Storage Golems only

The **Order Collector** and **Order Collector Hub** have been deleted.
They are no longer registered, craftable or available in Creative, and
saved legacy blocks have no compatibility implementation. Back up or
remove old warehouse content before installing this build.

Order requests at the Rack Computer go **only** to attached OUTPUT
Pallet Transfer Points. Real Storage Golems docked in physically linked
Storage Golem Shelves collect stock from connected racks and walk it to
an OUTPUT PTP. Shulker Tanks attached to shelves supply empty boxes for
packed orders. There is no alternate virtual-worker or legacy hub path.

## New 3×3 Pallet Transfer Point — first implementation (Issue #34)

### Building a basic point

1. Craft **9 Pallet Transfer Blocks** (one shaped recipe outputs nine).
2. Place all nine in one **3×3 square on the ground**. The ninth block
   automatically activates one shared controller in the central square.
3. Right-click any cell holding a pallet item. The pallet sits **centered**
   over the platform, with its full 2×2 deck and up to three cargo layers
   rendered above the platform.
4. Right-click normally to open the existing pallet inventory and advanced
   filter screen. **Sneak-right-click with an empty hand** to switch between
   **INPUT** and **OUTPUT**. The mode persists through saves.
5. To remove an empty pallet, sneak-right-click a platform while holding
   another pallet item. Items stored in the platform must first be removed.

All nine platform cells are separate replaceable material blocks but together
form **one logical storage controller and inventory**. Breaking any cell
unforms the platform, drops stored inventory and the installed pallet once
in Survival, and leaves the other eight building blocks to reuse.

### Optional modules

- **PTP Hopper Adapter:** Right-click a platform **corner** using a Hopper
  Adapter block item. It replaces that corner cell and extends above the floor
  as a tall adapter fin. A hopper/pipe targeting that cell accesses the
  *same* pallet inventory, not an extra cache. In **INPUT** mode it accepts
  incoming stacks (subject to pallet filters); in **OUTPUT** mode it allows
  extraction. The previous corner building block is returned when upgraded
  in Survival. Transfer behavior assumes the connected pipe/hopper respects
  the Minecraft `Container` inventory interface.
- **PTP Wrap Unit:** Right-click one of the **four side-middle** platform
  cells using a Wrap Unit block item. Click the Wrap Unit **empty-handed**
  to seal a loaded pallet. Wrapped pallets have transparent glass walls,
  block access to the inventory and prevent hopper transfers.
  **Shears** unwrap the pallet to restore its normal interactions.
- Neither module can be installed in the center cell. These modules form the
  initial material/capability framework; additional platform materials and
  upgrades will be added in later phases.

### Current limits

The physical endpoint supports Storage Golem input sorting, output ordering
and linked Shulker Tank packing; all need a connected shelf and Rack Computer.
The handheld Storage Scanner and player-led four-Golem transport are
implemented for test worlds. A fully synchronized floating cargo mesh
between four carriers remains visual work; no legacy Hub or Collector is
registered in this build.

Test module orientation, shulker contents, module cell break/reassembly,
read/write pipe direction, filter enforcement, sealing, and world persistence
on a backed-up test world. Automated CI build success is not in-game proof.

## Storage Golems and linked shelves — input-sorting prototype

**Storage Golems are their own small slate-steel and cyan warehouse mobs.**
They are not Copper Golems. They use normal server-side pathfinding rather
than teleporting items between containers.

### Shelf and input sorting setup

1. Craft a **Storage Golem Shelf** and place it adjacent to **any rack frame segment in the Rack Computer's network** (or adjacent to the Computer itself).
2. Adjacent shelves connect into one bounded shelf group. Each block holds
   **up to two docked Storage Golems**. Right-click a shelf with a crafted
   Storage Golem item to insert it. The shelf renders the number currently
   docked (zero, one, or two).
3. Place a **3×3 Pallet Transfer Point** touching a shelf from this group
   **or the Rack Computer**. Install a pallet and set the PTP to **INPUT**.
4. In the Rack Computer, use **GOLEMS: AUTO / PAUSED** to control
   automatic sorting for all shelves physically linked to that computer.
   The button shows currently docked workers. The permission check runs
   server-side and the sorting state persists.
5. Fill the input pallet. Every 40 ticks a stocked shelf checks whether
   cargo can be accepted by any loaded rack in the computer's network.
6. A Storage Golem physically leaves a shelf, walks to the input PTP,
   picks up up to one stack and carries it to a compatible rack. It then
   walks home and redocks, freeing its worker slot again.

Pallet filters are respected. If the destination fills while a worker is
traveling, it seeks another compatible rack. If none exists, it returns to
the shelf and physically drops the actual cargo for recovery. A loaded
worker and its cargo survive world saves/reloads; unloaded chunks are not
force-loaded to fulfill work.

**Sneak-right-click a deployed worker** to retrieve it as a Storage Golem
item. If it has cargo, that cargo is returned to the player's inventory.
Breaking a shelf drops its currently *docked* workers, not deployed workers:
a golem exists as either a shelved unit or an active world entity, never
both. Deployed golems can be retrieved separately.

### Output orders — physical Storage Golem retrieval

1. Set a completed PTP to **OUTPUT** (sneak-right-click empty-handed);
   install a pallet, and leave it **unsealed** for deliveries. Make sure its
   pallet filter accepts the requested items.
2. Place the output PTP touching the Rack Computer or a shelf in its linked
   Storage Golem Shelf group. The computer still needs a physically connected
   rack network.
3. From the computer, select a rack item, enter the quantity (1–4096), and
   click **REQUEST ITEMS**. Up to **16 FIFO orders per output PTP** are saved
   across reloads. The computer shows pending order/item counts. Orders can
   be submitted before the output pallet is ready.
4. When a docked worker is available and sorting is in **AUTO**, it walks
   to a rack with matching stock, withdraws at most 64 items, and physically
   delivers them to the output pallet. A persisted single active reservation
   per output point prevents multiple workers overfilling the same order.
   The order quantity only decreases for items actually deposited.
   Reservations use renewable two-minute leases so an orphaned worker cannot
   permanently prevent a future pickup after its destination reloads.
5. Full/filtered/sealed output pallets suspend future pickups. If capacity
   or mode changes during travel, the worker walks back to its source rack,
   returns any remaining cargo, and releases the unfulfilled reservation.
   If returning cargo is impossible, the actual items drop by the rack for
   recovery; cargo is never silently deleted. Breaking or recalling a worker
   also releases a currently loaded output reservation.

### Shelf-linked Shulker Tank — physical packed shipments

Attach a two-high **Shulker Tank directly to any shelf in the connected shelf
group** (either half may touch a shelf), then stock it with up to 64 truly
empty shulker boxes. Named and colored empty boxes are retained unchanged.

When a Rack Computer output order is pending, a Storage Golem prefers a
**packed delivery** if a linked magazine has an empty box, the ordered item
can legally fit inside a container, and the destination pallet filter accepts
a filled box. The golem visits the magazine, takes one physical empty box,
walks to one connected source rack, loads **up to 27 matching stacks** (at
most the remaining requested item quantity), and delivers the filled box
to the OUTPUT Pallet Transfer Point. The order is credited for the number of
**items inside** the delivered box, not for the one container item.

If there is no empty box, the PTP rejects filled boxes, or the item cannot
be nested inside a box, existing **loose-item retrieval** remains active
where permitted by the pallet's filter. A worker never remotely consumes
the shelf tank's stock at dispatch time.

If a packed delivery fails before depositing, the worker walks the filled
box back to the original rack, unpacks its actual cargo into that rack (or
drops any leftovers for recovery), then returns the now-empty box to its
original Shulker Tank. If the tank has since been destroyed or filled up,
the real empty box drops beside the worker instead. Death or player recall
drops/returns the filled box or empty box the worker physically carries;
there is no second hidden inventory copy.

**Limit of this phase:** One golem fills a box from *one source rack per
trip*, not from 27 different racks. It uses real navigation and the same
saved output-order reservations as loose deliveries. Future work includes
cross-rack multi-stop packing and return-to-tank visual animations. The
Handheld Scanner and four-Golem player-led pallet transport are implemented.

The new worker system needs client-side pathfinding and server reload QA;
compilation alone cannot verify movement, collision, and recovery.

CI verifies code compilation but not real mob navigation. Verify clearance
around your shelves, golem pathing to the outside of racks and transfer
points, safe handling of full destinations, death and drops, and reloads
on a backed-up test world.

## Portable Pallet Wrapper

The new **Portable Pallet Wrapper** is a reusable handheld tool (4 glass
panes, 4 iron ingots and 1 copper ingot in the crafting pattern
`GIG / ICI / GIG`). Right-click an intact, free-standing 2×2 pallet to
toggle four transparent glass sides **without removing or replacing any
items**. The same tool can wrap/unseal rack-mounted pallets and installed
PTP pallets (even when no Wrap Unit module is installed). Shears remain an
alternative way to remove glass. The wrapper is available in Creative and
the Warehouse Test Kit.

## Single-entity pallet carry with four locked Golem corners

At pickup, **four actual Golems must still assemble** at a sealed
2×2 pallet (on ground anywhere, or on a PTP). Only after all four
unique workers reach it does the server move the ONE real pallet stack
and all **216 item stacks** to a new `moving_pallet` entity.

During transport, **the PALLET is the only moving/colliding/navigation
entity**. Four Golem models are fixed to its corners, with raised arms
and alternating leg swings driven by that one entity's movement. There
are **no four independently walking Golem entities while carrying**,
so the workers cannot drift apart, push the pallet out of alignment,
or wander into different routes. The pallet rises once on pickup.
Glass panels and up to 27 visual cargo cartons are rendered as one load.

The Scanner still chooses the destination: an empty connected or remote
rack, or a clear 2×2 patch of ground. Failed placement leaves the
single moving entity holding the load. After a successful destination
handoff, the one pallet entity respawns **exactly four empty Golems**
at its location using their original saved shelf coordinates. The
crew then follows the player until dismissed at the original
warehouse Computer. Death of a loaded moving pallet drops its actual
one pallet, all stored items, and four recoverable Golem tokens.

Manual pallet movement still uses Minecraft ground navigation,
not teleportation. Automatic remote swaps additionally use expiring
simulation tickets near the physical carrier, rather than permanent
forceload settings. A narrow aisle that cannot physically admit the
wide pallet may stop the group. Validate uneven
terrain, obstacles, cargo integrity and saving in a backed-up test
world; passing CI does not prove perfect client animation.

## Handheld Warehouse Scanner, manual haul and remote pallet exchange

The Storage Scanner is an item linked to one Rack Computer in the same
dimension. **Linking and deploying a crew are separate operations**.

1. **Right-click a Rack Computer once with the Scanner** to link it. All
   Golems remain docked; the warehouse is ready for automatic remote pickup.
   Sneak-right-click the Computer for warehouse diagnostics.
2. **Manual carry:** right-click the same Computer a second time to deploy
   four docked Golems. Right-click a sealed PTP or wrapped ground pallet
   to have them lift the cargo, then follow the crew to a clear rack or 2×2
   ground footprint. Once unloaded, click the Computer to dismiss the crew.
   Manual carrying and automatic remote dispatch share the four physical
   workers: a warehouse with only four Golems cannot run both simultaneously.
3. **Automatic remote pickup:** with the workers **still docked**, place a
   glass-wrapped freestanding pallet at the off-site pickup location. Have
   an empty pallet ready in a **Stack of Pallets** magazine stored in a
   connected rack, or on an unwrapped, empty **INPUT PTP**. Sneak-right-click
   any of the four ground-pallet blocks with the linked Scanner. The request
   saves to the Scanner. When both locations are loaded and four docked
   workers, an accessible shelf departure aisle and a replacement pallet are
   available, a single four-Golem platform departs carrying the replacement.
4. The carrier replaces the wrapped off-site pallet with the fresh pallet,
   preserves all **216 cargo slots** on the original and returns to its linked
   warehouse. An available INPUT PTP is preferred, then any other vacant PTP
   (reconfigured as INPUT), then a free accessible rack. The recovered pallet
   is unwrapped only after successful placement on a PTP.
5. Pending automatic jobs retry from the linked Scanner every five seconds
   while it is in the requesting player's inventory. The chat reply starts
   with **WAITING** when dispatch is blocked; the original pallet is not
   consumed while queued.

**Unloaded chunks:** The Scanner now opens short-lived, renewable
warehouse chunk tickets while its request is pending. Once dispatched, the
single physical carrier keeps its own surrounding walking chunks and
current destination ticking so it can travel outside the player's simulation
distance. Unlike permanent /forceload settings, these tickets expire
automatically if the request or carrier stops renewing them. The carrier
still walks through the world; blocked terrain and nontraversable elevations
are real obstacles. Do an in-game test for long routes, world restart
and a shared server before relying on unattended cargo delivery.

**Do not use the manual haul click before a remote job.** The most common
failed dispatch on a four-Golem warehouse was the old Scanner-link behavior,
which deployed all four workers automatically. This is corrected: the first
click now **only links**.

Failures are non-destructive where possible: the request remains saved,
rejected warehouse pallet withdrawals restore the original magazine/PTP
stock, and failed carrier placement retains custody of the pallet until a
destination is available. Validate complete inventory recovery and floor
navigation in a backed-up test world.

## Integrated testing tools

The **Warehouse Test Kit** in Creative provides actual racks, shelves,
Golems, PTP modules, Shulker Tanks, connectors and the handheld Scanner.
It does not place blocks automatically.

Use **Scanner + sneak-right-click Rack Computer** for a read-only
readiness audit. See
[Warehouse integrated test plan](warehouse-test-plan.md)
for practical in-game validation.

## In-game verification

Follow the [full regression checklist](warehouse-test-plan.md), especially
connector rod orientation, side-to-side aisle inheritance, face-to-face
network joins, four-Golem pickup, player following, ground delivery,
rack insertion, worker dismissal, glass wrap transparency and
reload/interruption safety.
