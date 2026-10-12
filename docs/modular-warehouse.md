# Caszual Additions — Modular Pallet Warehouse (development)

Target: Minecraft Java **26.2 Fabric**. This document covers the current
implementation on the main branch. For a step-by-step end-to-end regression
of each system, see [Warehouse integrated test plan](warehouse-test-plan.md).

## Blocks and dimensions

| Block | Occupied space | Function |
| --- | --- | --- |
| Pallet | 2 × 2 × 3 display; 216 storage slots | Standalone storage (existing) |
| Pallet Rack | **2 × 2 × 3** | Holds one pallet and the pallet's 216 slots |
| Rack Computer | 1 × 1 × 1 | Search and order terminal for physically connected racks |
| Order Collector Hub | **2 × 2 × 1** | Holds one output pallet and **up to 3 collectors** |
| Shulker Tank | **1 × 1 × 2** | Magazine of **64 EMPTY shulker boxes** |

All rack logic is server-authoritative. It doesn't load distant chunks. Only
loaded neighboring racks are included in a computer's graph, capped at 256
rack controllers.

### Building rack rows

1. Place a Pallet Rack with a **2 × 2 × 3** empty volume.
2. Keep one complete block of empty approach space **in front of each rack**,
   across its two-block width and three-block height. A rack, computer, hub, or
   shulker tank cannot be placed across an existing reserved corridor.
3. Place adjacent rack bays **side by side** to extend the automatic aisle.
4. For racks on another level or behind the row, apply **Rack Connectors to
   the matching physical faces on BOTH touching racks**. These add the racks to
   the same network; they do not automatically rename the destination aisle.
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
and on the output pallet of a legacy Order Collector Hub.

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
- **Rack Connector:** Right-click the rack frame face to attach a
  connector. Two rack frames with **paired opposite connectors on their
  actual shared face** now display steel **X-shaped cross-braces** bridging
  the frames. Vertical pairs display short linking brackets.
  Unpaired connectors do not show a bridge or add an unintended network edge.
  The cross-brace is rendered only once per pair to avoid z-fighting.
- **Pallet Filter:** Use the real module slot and whitelist/blacklist controls
  in the pallet UI (detailed in the previous section). Rules and module state
  persist when pallets move into or out of a rack.

**Pallet ownership:** New 2×2 pallets use exactly one block entity on the
home corner, not four independent tile entities. The other three footprint
cells still reserve space and collision, and breaking any part removes the
whole pallet. All four pieces share the same 2×2 selection outline. Existing
saved inventory data remains on the home controller.

## Rack Computer

Open the computer with an empty hand. It can:

- Search by item name, registry ID, aisle name, or rack name.
- Browse racks in all touching rack groups and connector-linked networks.
- See each rack's coordinates, occupied slots out of 216, stored-item count,
  and configured filter.
- Click a rack to inspect its aggregated contents.
- Mark matching rack locations for **10 seconds** with visible particles.
- Click an item in a selected rack, type an order quantity (1–4096), and
  choose **REQUEST ITEMS**.

The computer must touch at least one rack to search. For orders, connect a
Storage Golem Shelf group and an **OUTPUT** Pallet Transfer Point (touching
either a shelf in the linked group or the Rack Computer). The terminal queues
up to 16 pending output orders on that PTP; requests can wait safely for an
unsealed output pallet or available stock. Legacy Order Collector Hubs
remain supported if no output PTP is connected. All requests are validated
on the server.

## Order Collector Hub

1. Build and place a **2 × 2** hub adjacent to the Rack Computer.
2. Right-click the hub holding an **Order Collector** item to install it.
   One hub can hold **three collectors maximum**.
3. Right-click with a pallet item to install an output pallet. The pallet must
   have an item filter compatible with the expected output.
4. Optionally place a **Shulker Tank** touching any hub cell and stock it
   with empty boxes.
5. At the computer, inspect connected racks, select an item ID, and submit a
   quantity. Queued requests remain saved across world reloads.

Each collector is currently a **server-side virtual worker**, processing
network transfers every 10 game ticks. A future iteration will render actual
traveling collectors. The current implementation does not claim real animated
movement or simulated travel time.

The order queue supports at most 16 pending jobs per hub. When no pallet,
collectors, or sufficient stock/output room is present, work **pauses rather
than dropping items**.

### Shulker Tank and packing

- The Shulker Tank is **one block across and two tall**.
- Right-click with an **empty** shulker box (any color, name) to add one.
- The tank holds at most **64 individual empty shulkers**; filled or loot-pending
  boxes are rejected.
- Sneak-right-click with an empty hand to retrieve one.
- When packing is possible, an available worker takes a real empty box from
  the tank, loads up to 27 item stacks, and deposits the filled box on the
  hub's pallet. Names and colors survive.
- Without available shulkers, orders deliver **loose items** to the hub's
  pallet where allowed. A filter can prevent either type from entering.
- The worker never starts a loose transfer without reserving a compatible
  output slot; custom-item components are preserved.

### Known limitations and planned polish

- No rendered animated roaming collectors yet; movement is implemented as
  periodic, safe server-side logistics transfers.
- Search/highlights, inventories, 2×2×3 rendering and shulker packing all
  require in-game testing on a real client/server. CI only confirms compilation,
  unit tests and static model checks.
- Shulker selection is by **box item type** under pallet filters; the first
  available box is selected from the magazine.
- Auto-sorting, cross-dimensional networks, unloaded-chunk operation, and
  hopper/direct pipe integration are not part of this first implementation.

## New 3×3 Pallet Transfer Point — first implementation (Issue #34)

The original 2×2 **Order Collector Hub** remains as a legacy block. Its stored
orders, collectors and pallet inventory are not automatically converted or
deleted. **Do not replace an occupied old hub without emptying it.**

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
  to seal a loaded pallet. Wrapped pallets have external retaining bands,
  block access to the inventory and prevent hopper transfers.
  **Shears** unwrap the pallet to restore its normal interactions.
- Neither module can be installed in the center cell. These modules form the
  initial material/capability framework; additional platform materials and
  upgrades will be added in later phases.

### Current limits

The physical endpoint supports Storage Golem input sorting, output ordering
and linked Shulker Tank packing; all need a connected shelf and Rack Computer.
The Storage Scanner and initial coordinated four-golem transport of wrapped
pallets are now implemented and testable; floating cargo visuals still need
polish. Legacy hubs retain their own virtual order logic and saved state;
no automatic migration is performed.

Test module orientation, shulker contents, module cell break/reassembly,
read/write pipe direction, filter enforcement, sealing, and world persistence
on a backed-up test world. Automated CI build success is not in-game proof.

## Storage Golems and linked shelves — input-sorting prototype

**Storage Golems are their own small slate-steel and cyan warehouse mobs.**
They are not Copper Golems. They use normal server-side pathfinding rather
than teleporting items between containers.

### Shelf and input sorting setup

1. Craft a **Storage Golem Shelf** and place it next to a **Rack Computer**
   that physically connects to your loaded rack network.
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
Storage Scanner and first four-Golem sealed-pallet transport are implemented. The original 2×2 Order
Collector Hub remains independent.

The new worker system needs client-side pathfinding and server reload QA;
compilation alone cannot verify movement, collision, and recovery.

CI verifies code compilation but not real mob navigation. Verify clearance
around your shelves, golem pathing to the outside of racks and transfer
points, safe handling of full destinations, death and drops, and reloads
on a backed-up test world.

## Storage Scanner — read-only pallet inspection

The **Storage Scanner** is a new craftable steel/cyan block. Place it beside
**any block of a fully assembled 3×3 Pallet Transfer Point** (including hopper
or wrap modules), then right-click the scanner to inspect the point's pallet.
It works on **sealed** pallets without unwrapping, withdrawing or altering
their real inventory.

The scanner reports the pallet's total item count, distinct item IDs and
occupied slots. When the point is connected to a Rack Computer (directly or
through adjacent linked Storage Golem Shelves), it also estimates how many
items from that pallet would fit across the computer's **loaded, connected**
rack network, respecting each rack's currently installed pallet, filters,
existing matching partial stacks, and remaining empty slots.

- The estimate uses a temporary **virtual inventory**: it simulates merging
  into matching stacks first, then empty slots, across the racks in the
  computer's network. It never moves items or changes filters.
- Racks without pallets, racks in unloaded chunks and disconnected racks
  are excluded. The reported capacity is a **snapshot**, not a reservation:
  other Golems or players may fill storage before a real pickup.
- With no connected computer, the scanner still reports the PTP inventory
  but explicitly says that capacity could not be verified.
- A connected Rack Computer shows the first discovered scanner's summary
  underneath its existing PTP status. Its search, order and Golem buttons
  continue to work as before.
- The scanner also starts/cancels a manually assigned four-Golem wrapped-
  pallet haul with a sneak-right-click. Regular right-click inspects contents;
  right-click **holding PAPER** runs the detailed readiness audit.

Scanner craft: iron ingots around redstone, one glass pane, and a comparator.
It is also available from the Caszual Additions creative tab.

## Coordinated four-Golem wrapped-pallet hauling (first iteration)

**Four physical Storage Golems** now cooperate on an entire pallet move, not
just individual item stacks. This is a manually dispatched, bounded workflow.

1. Build a **3×3 Pallet Transfer Point** with a **Wrap Unit**; install a
   pallet, load its inventory, and seal it by using the wrap module.
2. Attach a **Storage Scanner** to any cell of the point.
3. Connect the point to a Rack Computer directly or through a linked
   Storage Golem Shelf group. Dock **at least four** Storage Golems across
   those linked shelves.
4. Leave a **rack completely empty (no installed pallet)** within
   48 blocks horizontally/3D squared-distance and 12 blocks vertically
   of the source point. The rack must be connected to that same terminal.
5. **Sneak-right-click the scanner** to deploy four Golems. It picks
   the nearest eligible empty rack. All four physically rally at the
   wrapped pallet and then walk to the chosen rack. Only when all four
   reach the rack does the original pallet and its **216 original
   item slots** transfer into the destination rack.
6. **Sneak-right-click again** during travel to cancel the haul. The
   source remains wrapped with its original inventory and the walking
   workers redock. Ordinary scanner right-click still shows item counts
   and virtual rack capacity.

### Safety and scope

- The **source transfer point retains the one authoritative inventory**
  for the duration of a haul. The Golems carry a *transport assignment*,
  not a second copy of 216 stacks. Scanner access is read-only.
- The transfer destination must still be empty at delivery. If another
  player installs a pallet there, the workers wait; the source remains
  intact. Returning workers consume no stock.
- The sealed source cannot be unwrapped, have its mode changed, or have its
  pallet removed while a haul reservation exists. Breaking the source
  cancels the assignment and uses the normal Survival item drop path
  after unsealing, so its cargo remains recoverable.
- Killing or recalling any one worker cancels the entire shipment; each
  remaining worker returns to its shelf. Worker death and retrieval
  preserve the existing Storage Golem item.
- Each Golem chooses a loaded, collision-free approach to the point
  instead of assuming the south face is always open. The four workers
  prefer different approach sides so a scanner or module on one edge
  does not block everyone. The scanner and Rack Computer display an
  active haul's destination.
- The assignment, target rack, four-person rally/arrival masks and
  individual worker routes are persisted for world reloads. A five-minute
  lease prevents an abandoned reservation from blocking a PTP indefinitely,
  and active workers renew it while the source chunk is loaded.
- The rack accepts the pallet and inventory together on the server
  thread, after all four Golems have arrived and all item components
  are validated against the destination pallet filter.

**Presentation limitation:** The four workers show synchronized numbered
RALLY/PALLET labels and lift their arms while carrying, and the source PTP
shows purple status lamps during the job. The pallet itself still remains
visually at the source while escrowed, then appears in the rack on handoff.
A fully synchronized floating pallet mesh and physically coordinated
corner-grip placement remain future polish. This is also
*not* an unloaded-chunk hauling system and does not reserve the target
rack against other uses.

## Integrated testing tools

Use the **Warehouse Test Kit** item from the Caszual Additions Creative tab,
then right-click it in the air while in Creative to receive enough actual
warehouse components for input sorting, output orders, Shulker Tank packing,
rack labeling, physical connector networks and a four-Golem pallet haul.
It does not automatically edit the world or destroy inventories.

**Right-click the Storage Scanner while holding PAPER** for a read-only
health audit. It reports the assembled PTP state, stored slot/item totals,
connected Rack Computer, connected/empty racks, linked shelves, docked
workers, enabled sorting, empty shulker boxes, queued orders, scanner capacity
and any active four-worker haul's rally/arrival progress. Audit operations
never reserve or withdraw items, and do not force chunk loading.

See [Warehouse integrated test plan](warehouse-test-plan.md) for the
test-by-test checklist, multiplayer checks and known unimplemented polish.

## Suggested in-game regression checks

- Dispatch a wrapped pallet with exactly four Golems split across two
  adjacent shelves; verify all four physically rally before departing.
- Fill the destination rack just before arrival: verify the pallet remains
  safely sealed on its source point and no copy is created.
- Kill or recall one of four workers: verify the haul cancels, other
  workers redock, and the original pallet retains every stack.
- Cancel by sneak-clicking the scanner, then send another haul. Verify
  no permanent locks or missing worker tokens.
- Break the source point in Survival mid-haul and verify its real pallet
  and all 216 slots drop exactly once, while escorts return.
- Save/reload with two or three workers at destination; verify the
  persistent arrival bitmask and same source pallet without duplicates.
- Put a Storage Scanner alongside each of the nine possible PTP cell
  positions, including a Hopper Adapter and Wrap Unit, and verify that it
  finds the same central pallet controller.
- Scan a sealed pallet and verify no items are extracted, no wrapping is
  removed, and the item totals match the actual pallet inventory.
- Test multiple rack filters, partially filled matching stacks, and a
  completely full rack. Scanner capacity should fall as slots are occupied
  and show only racks in the loaded connected network.
- Check the Rack Computer scanner status at high GUI scale, then confirm rack
  row clicking and item selection still align with the displayed rows.
- Place adjacent racks at north/east/south/west orientations and verify no
  rack or pallet mesh protrudes outside its 2×2×3 footprint.
- Stack racks and put racks back-to-back, using paired connectors; verify that
  the computer scans each group once, and doesn't scan unconnected racks.
- Apply an anvil-named Aisle Tag A and Rack Tag 12; verify side aisle signs and A-12 front/computer labeling throughout that aisle only.
- Add another rack to an aisle end: the side sign should move to the new exposed edge without leaving a duplicate between racks.
- Fill a filtered rack, try to insert disallowed items by hand and quick-move.
- Search for the same item in two racks; verify both highlight for ten seconds.
- Queue an order without an output pallet; add one later and verify completion.
- Request more than one stack through an OUTPUT PTP and confirm exact delivered count.
- Seal the PTP or fill it mid-trip; verify the golem walks stock back and the
  order resumes only after the PTP accepts more cargo.
- Kill or recall a worker mid-output trip; confirm its cargo remains recoverable
  and the outstanding order/reservation is not lost on a normal loaded-chunk run.
- Save/reload during an outbound shipment and ensure it neither duplicates stock
  nor strands the order reservation.
- With a full or filtered output pallet, verify stock is not removed/lost.
- Link a Shulker Tank to a shelf (test upper- and lower-half adjacency),
  then request more than one stack through the OUTPUT PTP. Verify the golem
  walks to the magazine first, carries a real filled shulker and the order
  counts individual *contents*, not box count.
- Mix named/color empty shulkers in the magazine; verify their appearance and
  custom names survive packing, delivery, and cancellation.
- Reject shulkers with the output pallet filter, then verify normal loose
  transport still works where allowed.
- Fill/seal the PTP after packing; verify items return to the rack and the
  empty box returns to its tank or drops recoverably if the tank is broken.
- Kill/reclaim the golem before and after it fills a box. Confirm exactly
  one box and its original cargo are recoverable, with no phantom stock.
- Save/reload with an empty box held and with a full box in transit; verify
  item counts and reservation continuity.
- Break and re-place a rack and a hub, making sure all storage items, collectors,
  and the pallet drop in survival.
- Test persistence by saving/reloading while an order is waiting on stock.
