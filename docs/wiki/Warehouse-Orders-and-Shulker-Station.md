# Warehouse Orders and Shulker Station

[← Home](Home.md) · [Pallets](Pallets.md)

## Build the network

1. Connect a Rack Computer, stocked Pallet Racks, and Storage Golem Shelves through your warehouse network (warehouse blocks or Industrial Grade Concrete).
2. Build a **3×3 Pallet Transfer Point (PTP)**, set it to **OUTPUT**, and install an **unsealed pallet**. An output order needs a pallet where completed shulker boxes can be stored.
3. Replace one corner of the assembled PTP with a **PTP Hopper Adapter**.
4. Place the **Shulker Station** immediately beside that corner adapter. Its 2-block-high station should touch the adapter at the lower block level. Network attachment alone does not count: the station must touch the adapter.
5. Right-click the station with empty shulker boxes to stock its 64-box magazine. The old Shulker Tank block and saved inventories are reused as the Shulker Station.

## Submit a multi-item order

At the Rack Computer, choose **RACKS & PTPs**. Click a rack to inspect, select a stocked item on the right, enter an amount from 1 to 4,096, then choose **ADD TO LIST**. Continue adding other item types. Open the new **ORDER LIST** tab to review the requested quantities. Click any row to remove it, or **CLEAR LIST** to start again. When ready, choose **SUBMIT ORDER LIST**.

The server validates all items against connected racks and queues **all entries together**; if any entry is invalid or the output queue is full, none of the list is submitted. The queue accepts up to 16 distinct item entries.

## How the Golems fulfill orders

Each deployed Storage Golem has **nine cargo slots** for collected item stacks. The Golem walks to a source rack, retrieves up to nine stacks, then walks back to the Shulker Station. It does not spawn or fill shulkers in its own inventory.

The station packs into **three simultaneous shulker boxes**, each with the normal 27-stack capacity. A full shulker is marked ready; remaining partially filled boxes are marked ready when the output order queue finishes. Finished boxes transfer directly into the installed output pallet **only while an adjacent PTP Hopper Adapter is present** and the output pallet can accept them. The station buffers boxes if that pallet is unavailable or full, instead of discarding them. Breaking the station drops empty magazine boxes plus all working and finished boxes.

The ORDER LIST screen shows how many stations are connected, how many empty shulkers remain, how many are being packed, and how many finished boxes are ready.

## Navigation notes

Golems approach rack and station workstations from a standable aisle tile, not by routing through a rack's open metal frame. Home/work routes try alternate level paths around temporarily occupied aisle waypoints.

This is an initial warehouse-order implementation. Verify a small order and full/partial shulker handoff in-game before routing valuable inventory through an unattended station.
