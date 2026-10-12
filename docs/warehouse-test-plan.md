# Caszual Additions — Warehouse Test Plan (Fabric 26.2)

**Testing status:** Fabric CI validates compilation, unit tests, JSON
blockstate/model references, and basic invariants. It does not drive a
Minecraft GUI, validate real Golem navigation, or establish zero-loss across
chunk saves. Run these tests on a backed-up world, then on a dedicated server.

## Creative staging

1. Take the **Warehouse Test Kit** from the Caszual Creative tab; right-click
   in air to unpack. It includes racks, pallets, a Rack Computer, PTP blocks,
   Wrap Units, Hopper Adapters, four Storage Golem Shelves, eight Storage
   Golems, Shulker Tanks and a **handheld Storage Scanner**. It contains
   **no Order Collector or Order Collector Hub**.
2. Build rack frames (2×2×3), one Rack Computer touching rack frame, and
   two or more linked Storage Golem Shelves touching that computer.
3. Dock at least four Golems. Form a 3×3 PTP for INPUT and another for OUTPUT;
   connect them physically to the computer or the linked shelf group.
4. Connect other rack groups using physical Rack Connector rods, including
   a bridged gap (up to four cells). Leave at least one empty rack.
5. Prepare a third PTP with a Wrap Unit and loaded pallet for the moving test.

## Rack connectors — slim rods, horizontal only

- [ ] Facing NORTH, EAST, SOUTH and WEST, right-click a rack frame with a
      Rack Connector item. Confirm it renders **one thin, top-corner frame
      rod**, not two middle rods or a giant diagonal X.
- [ ] Put another rack 1–4 clear horizontal cells away on the clicked face;
      confirm one connector click extends the single top-corner rod across the gap.
- [ ] Try **side-to-side** spans to join two parallel racks in one aisle.
      Anvil-name an Aisle Tag A; verify tag inheritance and Rack Tag A-12.
- [ ] Try **face-to-face** spans. The Rack Computer must discover both
      groups, even if they retain separate aisle tags.
- [ ] Destroy a middle section; verify connection and search results break.
      Restore the piece and confirm it reconnects without restart.
- [ ] Click a lower rack section near each opposite side corner: the rod
      must snap to the rack's top at the corner chosen by the click.
- [ ] A five-block gap, a diagonal turn and vertical connector placement
      must NOT silently join rack networks.
- [ ] Two racks placed directly back-to-back connect without rod items.
- [ ] Walk beside/over rod spans; collision should be narrow and consistent
      with the visible model.

## Rack computer, PTP and wrapping

- [ ] The computer looks like a player-facing kiosk with a screen/keyboard.
- [ ] Search racks by aisle, item and name; highlight full rack outlines
      for 10 seconds rather than particles, visible only to searching player.
- [ ] The computer lists directly attached and shelf-linked PTPs.
      Test NEXT PTP, mode switching and Wrap/Unwrap button permissions.
- [ ] Rotate Golem Shelves, Hopper Adapters, Wrap Units through all four
      directions; models and docked worker visual poses must rotate.
- [ ] Mount pallets in racks; cargo and collision stay inset by two
      pixels per horizontal edge (four pixels smaller overall).
- [ ] Wrap a loaded pallet from the Wrap Unit. Check **clear glass pane-style
      walls around all four sides, not iron bands**; glass should not
      z-fight with cargo/frames. Unwrap using shears.
- [ ] Packed item transport works via a linked shelf Shulker Tank; verify
      exact item counts, shulker names, filter rejection and loose fallback.

## Handheld Scanner — link, lift, follow, deliver, return

- [ ] Dock Golems on shelves touching a rack several blocks away from the
      Computer; verify Rack Computer and automated sorting detect them.
- [ ] Right-click the Rack Computer while HOLDING the **Storage Scanner tool**:
      it saves the warehouse link immediately, then attempts to deploy four Golems.
- [ ] With only three docked Golems, Scanner should still LINK to the
      Computer and clearly report that four are required. No worker items
      are consumed by failed dispatch.
- [ ] All four real Golems follow player before they pick up a pallet.
- [ ] Right-click a **sealed pallet on an equipped PTP** with scanner.
      The four move to the pallet first; they assemble on its sides.
      The source PTP must not empty before **four unique arrivals**.
- [ ] After all four arrive, the PTP becomes empty. Exactly one Golem
      (lead carrier) owns its actual pallet and 216 inventory slots; the
      other three are escorts. Verify four-player-following animation.
- [ ] Walk to a distant clear location. Right-click the intended block
      with scanner. Pallet appears in a clear loaded **2×2** footprint;
      compare inventory, component data and filters before/after.
- [ ] Repeat the lift, but right-click an **empty rack** at the destination.
      Verify pallet and all 216 stacks transfer to rack without duplicate.
- [ ] Click a full rack or blocked 2×2 area; failure must preserve the
      carried pallet. Retry when clear.
- [ ] Return to original Rack Computer after placement; scanner right-click
      dismisses Golems, who physically navigate to the original shelves.
      Confirm docked count restores to four.
- [ ] Sneak-right-click Rack Computer with Scanner to run **read-only
      warehouse audit**, including linked racks, docked Golems, PTPs,
      Shulker supplies and output queues.
- [ ] Right-click scanner in air to read saved warehouse link/crew status.
- [ ] Kill or reclaim the lead Golem while carrying: exactly one pallet
      and all stored item stacks drop for recovery, with no surviving
      second authoritative copy.
- [ ] Save/reload before pickup, with a carried pallet, and after placement:
      compare quantities; validate Golems resume their job or safely wait.
- [ ] Logout mid-transport. Golems must not discard stock or allow a
      different player to steal the scanner link.
- [ ] Attempt crossing dimension/chunk boundaries. Golems should follow
      only through loaded and navigable areas; note stalls. Do not assume
      chunk loading is implemented.

## Warehouse logistics and safety

- [ ] Stock INPUT PTP, enable GOLEMS: AUTO; real Golem collects and sorts
      items to allowed racks.
- [ ] Create Rack Computer order at an OUTPUT PTP; verify Golem retrieves
      matching racks and deposits *actual* requested stock.
- [ ] Attempt an order without attached OUTPUT PTP. It must fail with a
      helpful message, never fall back to a removed Hub.
- [ ] Validate pallet whitelist/blacklist (up to eight comma-separated
      IDs, tags and mod namespaces) and component-exact mode.
- [ ] Test PTP hopper input/output, wrapper on/off, blocked destination,
      interrupted Golem deliveries and item recovery.
- [ ] Destroy a PTP/rack with stock in Survival: drop actual inventory
      and pallet exactly once.
- [ ] Verify **Order Collector** and **Order Collector Hub** do not appear
      in recipes, JEI or the creative inventory and have no registered
      runtime blocks. Old worlds need migration before updating.
- [ ] Test a dedicated server with two players: scanner crews must be
      player-specific; one player must not control the other's Golems.
- [ ] Confirm no missing models, abnormal invisible collision, rendering
      overlap, Java exceptions, or persistent phantom reservations.

**Visual limit:** The four Golems walk and lift their arms, but a
fully synchronized floating 2×2 pallet mesh between all four workers
has not yet been implemented. Check carried stock integrity independently
of the unfinished animation.
