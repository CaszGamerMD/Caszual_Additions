# Warehouse Remote Pallet Exchange & Tools

## Remote pickup with the Storage Scanner
1. Right-click your warehouse Rack Computer with a Storage Scanner to link it. The scanner stores the dimension and Rack Computer position and keeps the link at any distance in that dimension.
2. Stock the warehouse with **four docked Storage Golems** and a fresh **empty pallet**. Store the fresh pallet in a Stack of Pallets inside a rack, or as an empty pallet mounted on an INPUT PTP.
3. At the remote site, **sneak-right-click a plastic-wrapped, intact 2×2 pallet** with the linked Scanner. This saves a remote pickup request on the Scanner (no GUI and no physical crew required beside the player).
4. When both the warehouse and pickup site chunks are loaded, the server dispatches one moving pallet entity with four attached golem models. The crew carries the fresh empty pallet to the remote site. Once next to the wrapped pallet, it places the fresh pallet in the same four-block footprint and takes ownership of the old pallet's entire 216-slot inventory.
5. The crew returns the old pallet to its home Rack Computer's network and unloads it at a vacant **INPUT PTP** first, then any vacant PTP (switched to INPUT), or an accessible vacant rack as a fallback. The PTP removes plastic wrap only after a successful transfer. Rack fallback preserves the existing wrap. The four workers then physically return to their shelves.

A loaded player-directed crew can also be recalled: **sneak-right-click in the air** with the linked Scanner. The crew then tries the same PTP/rack home-return sequence.

**Chunk-loading limitation:** No blocks or entities are teleported, and this mod deliberately does not force-load long routes. Requests persist on the scanner when either endpoint is unloaded. A traveling crew waits if the intervening path is unloaded or blocked. Ensure the route chunks remain loaded (e.g. nearby players or a chunk-loader) for a long-distance trip to actually progress. The status label describes the wait reason.

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
