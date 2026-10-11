# Caszual Additions — Modular Pallet Warehouse (development)

Target: Minecraft Java **26.2 Fabric**. This document covers the current
implementation in the modular pallet warehouse branch.

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
  of a rack. The tag becomes a **physical dark-steel sign on the front**,
  with readable pale text. Its name also appears in the Rack Computer.
- **Aisle Tag:** Rename in an anvil, then apply it to a rack in a horizontal
  aisle. A **second physical sign on the front** uses amber text.
  Side-by-side racks inherit the aisle prefix in the computer:
  `Aisle Name - Rack Name`. Each physical sign is attached to the rack
  on which you installed the corresponding tag.
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

The physical endpoint supports Storage Golem input sorting and output order
fulfillment; both require a connected shelf and Rack Computer. The Storage
Scanner, four-golem transport of wrapped pallets, and shelf-attached Shulker
Tank packing are still planned. Existing legacy hubs retain their virtual
order logic and saved state; no automatic migration is performed.

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
5. Full/filtered/sealed output pallets suspend future pickups. If capacity
   or mode changes during travel, the worker walks back to its source rack,
   returns any remaining cargo, and releases the unfulfilled reservation.
   If returning cargo is impossible, the actual items drop by the rack for
   recovery; cargo is never silently deleted. Breaking or recalling a worker
   also releases a currently loaded output reservation.

Output retrieval currently transports **loose items**. Linked shelf-attached
Shulker Tanks and golem-packed shipments, the Storage Scanner, and coordinated
four-golem sealed pallet moving are later phases. The original 2×2
Order Collector Hub continues to operate independently.

The new worker system needs client-side pathfinding and server reload QA;
compilation alone cannot verify movement, collision, and recovery.

CI verifies code compilation but not real mob navigation. Verify clearance
around your shelves, golem pathing to the outside of racks and transfer
points, safe handling of full destinations, death and drops, and reloads
on a backed-up test world.

## Suggested in-game regression checks

- Place adjacent racks at north/east/south/west orientations and verify no
  rack or pallet mesh protrudes outside its 2×2×3 footprint.
- Stack racks and put racks back-to-back, using paired connectors; verify that
  the computer scans each group once, and doesn't scan unconnected racks.
- Apply tags to an anvil and ensure aisle prefix only affects that aisle.
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
- Mix named/color empty shulkers in the magazine, then complete a packing order
  and examine contents and names.
- Break and re-place a rack and a hub, making sure all storage items, collectors,
  and the pallet drop in survival.
- Test persistence by saving/reloading while an order is waiting on stock.
