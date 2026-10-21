# Builder Golem warehouse fulfillment

This is the Caszual Additions half of the Golems Plus Builder supply chain
for Minecraft Fabric 26.2.

## Warehouse setup

1. Build a connected warehouse with a Rack Computer, racks containing real
   supplies, at least one Storage Golem Shelf with docked Golems, a
   Shulker Station stocked with empty shulker boxes and an OUTPUT PTP
   with an installed pallet.
2. The Shulker Station must have a physical PTP Hopper Adapter connection to
   that OUTPUT PTP, just like the normal player output-order system.
3. Link the Builder's Planning Table with an already-linked Warehouse Scanner
   (right-click table), and start the Builder's schematic.
4. When local building stock is insufficient, Golems Plus submits a small,
   persistent manifest to the linked Rack Computer. The warehouse selects an
   EMPTY OUTPUT PTP not already reserved for another order, and dedicates
   that endpoint to the Builder's request.
5. The existing Storage Golems fulfill each item from actual rack inventories
   and deliver it to the Shulker Station. Packed shulkers go through the
   station's adapter into the PTP, where the Builder physically picks them up.
6. Golems Plus reads the boxed supplies at the Planning Table and consumes
   actual items while placing blocks.

## Controls and guarantees

- A Builder PTP is reserved by its order UUID, owning player UUID, table
  position and Rack Computer position. This metadata and normal output
  queues are saved across chunk/world reloads.
- Normal player output orders are rejected for reserved Builder PTPs. PTP
  input/output changes, wrapping controls, and removal of the empty pallet
  are disabled while it is reserved.
- A Builder request is idempotent. Retrying the same UUID will not enqueue a
  second copy of its manifest.
- The selected PTP must begin with an installed pallet, empty cargo,
  no active output ticket, no current orders and a connected, empty
  Shulker Station. Existing user operations elsewhere in the warehouse
  are unaffected.
- No instant storage transfers, magic supply duplication, teleportation or
  chunk forcing. Storage Golems still take normal routes from shelves/racks
  to the Shulker Station; the Builder walks to the PTP.
- Warehouse status can report every currently unavailable requested item,
  the absence of available empty shulkers, current order queue, and packed
  boxes ready for collection. Golems Plus relays shortages to the Planning
  Table and online owner.
- Completed reservations are released only when output orders, packed
  cargo and Shulker Station buffers are empty.

**Limitations:** The first Builder integration ships up to 16 distinct item
types and 64 of any one item in a batch. More batches are requested as
construction advances. The Builder needs its supply path and relevant
chunks loaded. Empty returned packing shulkers must currently be returned
manually from the Builder Table to the warehouse. Unusual modded
container components and concurrent edits require in-game tests.

The optional public Java API is `BuilderWarehouseFulfillment`. Caszual
Additions does not depend on Golems Plus at compile time.
