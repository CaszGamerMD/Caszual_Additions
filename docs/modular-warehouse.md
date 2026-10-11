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

### Rack names, aisle names, and connectivity

- **Rack Tag:** Rename the item on an anvil and right-click a rack. Its name
  appears in the Rack Computer.
- **Aisle Tag:** Rename on an anvil and apply to a rack in a horizontal
  side-by-side aisle. Its text prefixes that aisle's rack names:
  `Aisle Name - Rack Name`.
- **Rack Connector:** Right-click a side of a rack to toggle a connector
  on that face. A paired connector on the opposite physical face of the
  neighboring rack joins their two networks. Side-by-side same-facing racks
  already automatically form a common aisle.
- **Pallet Filter:** Hold a sample item in the **offhand**, use the filter
  item in the main hand, then click a placed pallet or a rack. Only that item
  type can then be inserted. On a placed pallet, an empty offhand removes the
  filter. The filter is saved in the pallet item when picked up and follows
  that pallet into racks and the output hub.

The initial filter applies to item **type**, not tags, durability ranges, or
arbitrary data components.

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

The computer must be next to at least one rack for search, and next to an
Order Collector Hub to queue collection orders. The terminal validates requests
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

## Suggested in-game regression checks

- Place adjacent racks at north/east/south/west orientations and verify no
  rack or pallet mesh protrudes outside its 2×2×3 footprint.
- Stack racks and put racks back-to-back, using paired connectors; verify that
  the computer scans each group once, and doesn't scan unconnected racks.
- Apply tags to an anvil and ensure aisle prefix only affects that aisle.
- Fill a filtered rack, try to insert disallowed items by hand and quick-move.
- Search for the same item in two racks; verify both highlight for ten seconds.
- Queue an order without an output pallet; add one later and verify completion.
- With a full or filtered output pallet, verify stock is not removed/lost.
- Mix named/color empty shulkers in the magazine, then complete a packing order
  and examine contents and names.
- Break and re-place a rack and a hub, making sure all storage items, collectors,
  and the pallet drop in survival.
- Test persistence by saving/reloading while an order is waiting on stock.
