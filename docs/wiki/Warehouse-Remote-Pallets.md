# Warehouse Remote Pallet Exchange & Tools

## Remote pickup with the Storage Scanner
1. **Shift-right-click** your Rack Computer with a Scanner to bind it. The scanner remembers the dimension and Rack Computer.
2. Keep four **docked** Storage Golems and one fresh empty pallet available **per remote job**. For example, 12 docked workers and three fresh pallets can dispatch three independent crews together.
3. With the Scanner in **REMOTE** mode, right-click each intact, glass-wrapped ground pallet you want to swap. Each target is added to the scanner's persistent FIFO queue (up to 64 targets). Shift-right-click **in the air** to toggle Remote/Direct; switching to Direct cancels queued *undispatched* remote requests.
4. The dispatcher tries all queued targets, creating independent missions whenever it can reserve a **complete four-Golem crew**, an empty replacement pallet, and usable aisles. Jobs that cannot launch stay queued and are retried. A crew already servicing one target does **not** prevent the same player from dispatching another crew to a different pallet.
5. Three real Golems walk empty-handed to the pickup area. The fourth pushes the fresh pallet. The trio joins only after reaching the old pallet, then swaps the full 216-slot pallet for the empty one and returns its cargo to an accessible warehouse INPUT PTP or rack. Workers ultimately return to shelves.
6. The same wrapped pallet cannot be assigned to two active remote missions; both the three-Golem pickup party and the lifted pallet rig hold an exclusive claim while active. An already claimed or removed target is reported as invalid and removed from the pending scanner queue.

**Crew isolation:** REMOTE crews are independent of each other and of the player's DIRECT crew. Direct-mode manual controls still select only one player-led crew, so a remote worker cannot accidentally be counted as a direct squad member.

**No half-crews:** Having one to three docked workers does not launch a partial team. Dispatch withdraws four shelf tokens for each mission; spawn failure returns all four and restores the empty pallet.

**Chunk-loading limitation:** No blocks or entities teleport along long-distance routes. Pending requests retry when relevant chunks are loaded; dispatched workers may pause where routes are unloaded or blocked. Keep corridors loaded if you expect long-haul travel to progress.

## Stack of Pallets
- Holds **12 individually saved empty pallet ItemStacks**, including wood combinations, plastic colors, copper aging, wrap state and filter settings.
- Use the Stack of Pallets on an empty physical pallet to collect it. Ordinary use in the air returns the top pallet to your inventory. Sneak-use on an empty Rack or PTP to install the top pallet there.
- A warehouse Golem can now collect an empty pallet from an **INPUT PTP**, walk to a Rack containing a not-full Stack of Pallets and add the pallet to it. The worker itself transports the pallet; there is no free remote transfer.

## Pallet Clipboard Filter
Put the **clipboard filter** in the right-side upgrade slot. Click the installed clipboard to open its nested panel with **16 ghost item positions** plus whitelist/blacklist, matching scope and target fields. Ghost samples don't take items from the player. These rules still live on the pallet's saved data.

## Shulker Station
Right-click the two-high Shulker Station to open its **64-slot empty-shulker inventory** and view the three active packing lanes, completed shulkers, buffered item count, and attached Hopper Adapter/OUTPUT PTP status. Completed boxes still transfer only through a physically adjacent PTP Hopper Adapter.

## Deployment and sorting
Workers exit and check back in through the **front-facing shelf opening**, never the top. Inbound items choose pallets already containing the exact stack first, explicit whitelist matches second, and any pallet that doesn't reject the item third.

### In-game test checklist
- Queue a remote pickup while home is unloaded; load both endpoints later and verify exactly one dispatch.
- Swap a wrapped, filled pallet; ensure the new pallet is empty and the old cargo remains complete.
- Return to INPUT PTP, empty OUTPUT PTP, and a rack with all PTPs busy. Confirm no item duplication or loss.
- Close/reopen the Shulker Station and the clipboard panel; inventory and rules must remain consistent.
- Verify a loaded magazine stays at most 12 pallets, and a Golem never removes a filled input pallet.
- Test an unloaded corridor, an obstructed aisle, and a missing target; the job must wait, never teleport or delete cargo.


## Input and Output aisle tags

Aisle Tags work as warehouse routing labels when renamed **Input** or **Output**
(case-insensitive exact names; ordinary aisle names are unaffected).
**Input** appears in green and takes priority when a remote swap crew returns
its old pallet to storage. **Output** appears in red and reserves its rack
bays for outgoing Pallets Plus shipments, instead of incoming remote returns.
A rack inherits the label of its connected physical aisle row.

To stage an outbound pallet: link the Warehouse Scanner to the Rack Computer,
set **DIRECT** mode, summon four Storage Golems at a linked shelf and have them
pick up a pallet. Then **sneak-right-click the linked Rack Computer** with
the scanner while the team is carrying. They will navigate to a vacant,
ground-accessible Output rack and install the same pallet with all of its
contents. If every Output bay is full or obstructed, they hold the load and
retry rather than sending it to Input, a normal rack, or a PTP. You can also
use Direct mode to manually deposit onto a selected empty Output rack.

Caszual Additions exposes `WarehouseAislePriority.stagedOutputRacks(level,computer)`
to identify loaded, labeled shipping bays for Pallets Plus. **Actual train,
drone, and conveyor collection must be implemented in Pallets Plus** and
should reserve/pick up a pallet atomically before leaving; aisle tags do not
dispatch vehicles by themselves. Existing Output PTP item-order functionality
is separate from these rack-based pallet shipping lanes.
