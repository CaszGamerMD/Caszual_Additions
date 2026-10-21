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
   boards the forklift, and remains mounted between multiple pallet jobs.
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
