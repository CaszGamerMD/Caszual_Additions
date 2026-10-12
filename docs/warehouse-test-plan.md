# Caszual Additions Warehouse — integrated test plan (Fabric 26.2)

**Status:** all main subsystem foundations exist and are available for manual
testing. GitHub CI builds the mod and runs automated invariant tests; it does
**not** execute a graphical Minecraft client or multiplayer world. Back up
any world with valuable inventories before testing.

## 0. Rapid Creative setup

1. In Creative, take **Warehouse Test Kit** from the Caszual Additions tab.
   Right-click in the air to unpack it. This supplies **32 PTP transfer
   blocks, 8 racks, 8 Storage Golems, 4 shelves, 4 Wrap Units, 4 Hopper
   Adapters, 3 scanners, 2 Shulker Tanks, 16 empty shulker boxes**, pallets,
   filters, tags, connectors, building materials and a legacy collector hub.
   Overflowing stacks may drop by your feet if your inventory is full.
2. Build a 2-wide × 2-deep × 3-high rack, and leave its one-block-deep
   **front approach lane completely unobstructed**.
3. Place a Rack Computer touching an actual rack segment. Connect at least
   two Storage Golem Shelves adjacent to the computer and each other; insert
   all eight Golems (two per shelf).
4. Make one **3×3 INPUT PTP**, place an empty pallet, connect one platform
   cell to the linked shelves or Rack Computer, and attach a Storage Scanner
   to another platform edge. Repeat with an **OUTPUT** PTP and scanner.
5. Keep one or two racks **empty, without installed pallets** for testing a
   future whole-pallet transfer. Install a pallet in other racks to test
   item-level sorting. Use a third PTP with Wrap Unit for the hauling tests.
6. **Right-click a scanner with PAPER** at any time for a non-mutating,
   server-side **warehouse audit**. For normal inspection, right-click the
   scanner empty-handed. Sneak-right-click empty-handed starts/cancels a haul.

## 1. Rack and pallet storage

- [ ] Install wood, iron and plastic pallets; open storage from each rack part.
- [ ] Check full 216-slot capacity, 3×3×3 visible cargo and rack collision.
- [ ] Try placing racks facing N/E/S/W, side-by-side, back-to-back and stacked.
- [ ] Verify no entity can walk through the outside frame or through cargo.
- [ ] Try filtering by item, mod namespace, tag and exact components.
- [ ] Check whitelist and blacklist behavior, removing/installing filter module.
- [ ] Attempt shift-click transfer of a disallowed item; it must not enter.
- [ ] Save and reload: items and filter settings persist.

## 2. Labels, connectors and Rack Computer

- [ ] Anvil-name an Aisle Tag **A**; attach to a rack. Look for its sign on
      the *end side* of the aisle, not on every rack front.
- [ ] Anvil-name Rack Tag **12**; front sign and terminal label show **A-12**.
- [ ] Add an adjacent rack and verify end signs move outward.
- [ ] Bridge disjoint rack groups with two facing Rack Connectors; verify
      the visible crossbeam and connected computer search.
- [ ] Remove one connector: disconnected rack content must vanish from search.
- [ ] Search by name, ID, aisle and item; click contents and highlight racks.
- [ ] Test the UI at small/large Minecraft GUI scales, with JEI open/closed.
- [ ] Test the Computer auto/pause button and docked-Golem count.

## 3. PTPs and modules

- [ ] Place nine transfer blocks and verify a single controller forms at
      the center, with one shared 216-slot inventory.
- [ ] Fit Hopper Adapters into the four corners and Wrap Units into the
      four edge-middle cells; modules cannot occupy the center.
- [ ] Verify INPUT hoppers/pipes insert items, OUTPUT hoppers extract items.
- [ ] Seal a filled pallet; normal inventory opening and pipes are blocked.
- [ ] Scan it with a scanner; all original stacks remain visible in the scan.
- [ ] Unwrap with shears. Inspect visual retaining bands and edge alignment.
- [ ] Break any one cell in Survival with cargo: all items and pallet drop
      once; reassemble and verify the remaining eight blocks are reusable.

## 4. Storage Golem input sorting

- [ ] Set INPUT PTP, install pallet, fill with allowed/unallowed item types.
- [ ] Enable GOLEMS: AUTO. Confirm a real Golem walks PTP → rack → shelf.
- [ ] Verify correct item counts and filter rejection.
- [ ] Fill a target rack mid-trip: alternate rack chosen or actual cargo
      dropped for recovery, never silently deleted.
- [ ] Kill a carrying Golem; its inventory item(s) and worker token drop.
- [ ] Sneak-click a deployed Golem; worker item and real cargo return.
- [ ] Save/reload during delivery; verify exactly one real cargo copy.

## 5. Output orders and Shulker packing

- [ ] Change a connected PTP to OUTPUT. Select an item in the Computer
      inventory, request 1 / 64 / 128 / 4096 items.
- [ ] Confirm queued requests persist and credit *delivered* counts only.
- [ ] Test blocked output (sealed, full, filtered): source must not disappear.
- [ ] Stock a Shulker Tank touching either half of a linked shelf, with
      ordinary and renamed/colored empty boxes.
- [ ] Deliver a packed shipment containing up to 27 same-item stacks from
      one rack; preserve shulker color, name and contents.
- [ ] Take away a shulker tank / fill output during walking: original item
      stacks return to the rack or drop recoverably, and empty box returns
      to magazine or drops if unavailable.
- [ ] Remove empty shulkers: compatible orders fall back to loose transport.
- [ ] Mix component variants in a rack; returned contents must preserve them.

## 6. Scanner and rack-fit estimate

- [ ] Scan PTP with both empty and loaded pallet, sealed and unsealed.
- [ ] Scan next to each of nine PTP parts (including Hopper/Wrap modules).
- [ ] Scan without a Rack Computer, then with a connected computer.
- [ ] Verify room estimate only uses loaded connected racks, filters,
      compatible partial stacks and remaining space.
- [ ] Compare scanner contents to the Rack Computer status and inventory.
- [ ] Right-click with PAPER: audit identifies rack/shelf/worker/shulker
      prerequisites and gives clear blockers. Audit must move no items.

## 7. Four-Golem wrapped pallet transportation

- [ ] Keep a destination rack empty with no installed pallet. Seal a filled
      pallet at a PTP with an attached Wrap Unit and Storage Scanner.
- [ ] With only three docked Golems, sneak-click scanner; dispatch refuses.
- [ ] Insert fourth worker; dispatch four physically separate Golems.
- [ ] Verify all four rally at the source, then walk to the same rack,
      and source cargo changes owner **only when all four arrive**.
- [ ] Open scanner with PAPER mid-trip; check distinct rally and arrival
      counts, destination and haul state.
- [ ] Retry with a player-installed pallet in the destination mid-trip:
      source remains sealed until the blocker is removed or haul expires.
- [ ] Recall/kill a Golem: assignment cancels; other workers redock.
- [ ] Cancel using sneak-right-click scanner: source pallet is untouched.
- [ ] Break source PTP in Survival mid-haul: one pallet + contents recover.
- [ ] Save/reload during rally/delivery: no phantom duplicated pallets.
- [ ] Test blocked approaches on all four sides of the point and rack; note
      any pathfinding failures for a navigation follow-up.

## 8. Legacy Order Collector Hub compatibility

- [ ] Build 2×2 hub next to computer, install pallet and 1–3 collectors.
- [ ] Request item while no OUTPUT PTP is connected: legacy order routes
      correctly and only withdraws stocked items.
- [ ] Connect Shulker Tank to hub, pack output, preserve components/names.
- [ ] Break/re-place hub in Survival; verify stored output, workers and
      pallet drop exactly once; saved worlds with legacy hubs still open.

## 9. Dedicated-server and reload verification

- [ ] Repeat loading, saving, breaking and reconnecting on a Fabric
      dedicated server with two simultaneous players.
- [ ] Confirm client-side labels, PTP item models, scanner icon, Golem
      renderer, Shulker Tank and movement remain synchronized.
- [ ] Verify no forced chunk loading and no permanent reservation after a
      worker / point chunk unload.
- [ ] Repeat tests with JEI to find GUI layout or packet issues.
- [ ] Re-open a backed-up old warehouse world and verify saved blocks.

## Known foundations versus unfinished work

**Implemented/testable now:** racks, 216-slot pallets, physical labels,
connectors, filter modes, search, PTP assembly, hopper and wrap modules,
read-only scanner estimates, golem shelves, real input and output walking,
shulker-box packing, and four-worker wrapped-pallet handoff. The Creative
Test Kit and PAPER audit let a tester build and inspect all of these.

**Not yet complete:** visual suspended-pallet animation/corner grip,
packed multi-stop pick-up from multiple racks, advanced filter multi-rule
autocomplete, dedicated-server real-world regression automation,
cross-dimensional/unloaded-chunk operation. The legacy collector hub still
uses virtual workers. **Do not treat successful CI as proof of any item or
model rendering, animation, navigation or persistence behavior in-game.**

Report each failure with dimensions, facing, 3×3 part location, pallet filter,
worker count, steps taken, and latest.log/crash-report.
