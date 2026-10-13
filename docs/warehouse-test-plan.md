## Storage Golem flat-surface return navigation

- [ ] Dispatch a Golem and place one full-height solid block in its
      direct route home, with open level ground on both sides. After its
      job it MUST detour around the block; it must not hop over it.
- [ ] Arrange a zig-zag corridor made of ordinary full blocks or crates
      between the worker and its shelf. It should follow clear corners
      with no diagonal clipping through solid block corners.
- [ ] Verify it approaches the **side of its shelf on the same floor**,
      rather than trying to climb onto the shelf top.
- [ ] Repeat for all return flows: sorting, output orders, packed
      Shulker Tank orders, player-led Golems after the pallet is
      delivered, and an older automatic four-Golem haul.
- [ ] Place a solid block halfway through a Golem's return corridor.
      It should re-plan after detecting it cannot continue, not
      endlessly jump against the newly blocked route.
- [ ] Completely seal every side of its shelf: the Golem must wait
      instead of jumping onto or through the solid obstruction.
      Reopen the side and confirm it returns automatically.
- [ ] Test real elevation changes separately: golems may use legal
      stairs/sloped floors to return to a shelf on another Y level,
      but they must not target the TOP of their shelf.
- [ ] Interrupt a worker carrying undelivered stock or an empty Shulker.
      Once it eventually redocks, all real items must drop for
      recovery, never vanish as the Golem becomes a shelf token.
- [ ] Repeat on a dedicated server after saving and reloading;
      flat path caches should be rebuilt and not force-load chunks.

# Caszual Additions — Warehouse Test Plan (Fabric 26.2)

**Testing status:** Fabric CI validates compilation, unit tests, JSON
blockstate/model references, and basic invariants. It does not drive a
Minecraft GUI, validate real Golem navigation, or establish zero-loss across
chunk saves. Run these tests on a backed-up world, then on a dedicated server.

## Storage Golems workstation page

- [ ] Open the Rack Computer and select **STORAGE GOLEMS** in the header.
      Confirm the page reports linked shelves, docked and active Golems.
- [ ] With sorting paused, press **SORT: PAUSED** to enable AUTO. Place items
      at INPUT PTP and orders at OUTPUT PTP; select **RUN QUEUE** to request
      immediate dispatch from available docked workers.
- [ ] Observe deployed worker entries changing from PICKING to DELIVERING,
      then RETURNING TO SHELF; verify current cargo and position update.
- [ ] Select **ENLIST 4**; four docked workers follow the player.
      Verify workers show FOLLOWING PLAYER / READY.
- [ ] Use handheld Scanner on a sealed glass-wrapped pallet: inspect
      LIFTING WRAPPED PALLET then FOLLOWING PLAYER / CARRYING PALLET,
      and confirm the other three show ESCORT.
- [ ] Deliver pallet to ground or an empty rack, walk to original Computer
      and press **DISMISS CREW**; verify tasks change to RETURNING TO SHELF
      and docked count restores after arrival.
- [ ] Switch between **RACKS & PTPs** and **STORAGE GOLEMS**; existing
      search, order and PTP buttons must not overlap or intercept Golem UI.
- [ ] Inspect two-player server behavior: no cross-control of another
      player's crew, and only loaded Golems appear in task snapshots.
- [ ] Reload a world mid-job and confirm task labels and inventories persist.

## Industrial Grade Concrete connectivity

- [ ] Craft each of the 16 colored Industrial Grade Concrete variants
      using the matching 4 concrete blocks in corners and 5 iron ingots
      in the remaining crafting squares. Each recipe outputs **4 IGC**.
- [ ] Place a Rack Computer touching an IGC block (or directly on IGC).
      Run a continuous IGC floor to a remote rack, Storage Golem Shelf,
      Shulker Tank and 3×3 PTP. Each connected component must appear in
      Rack Computer searches and controls, and Golems must dispatch.
- [ ] Repeat with mixed IGC colors across the same flooring run.
- [ ] Connect a second warehouse branch using only contiguous warehouse
      component blocks touching face-to-face (no IGC).
- [ ] Break a middle IGC block in the only connection path and confirm
      disconnected components vanish from the network immediately.
- [ ] Confirm a loose IGC floor with no adjacent connected Computer does
      NOT spontaneously appear on another Computer's network.
- [ ] Build a long run across unloaded chunks: unavailable chunks must
      not be force-loaded or counted as connected until they load.
- [ ] Confirm IGC floor does not replace the four real Golems' walking
      requirements, create remote inventory access, or duplicate pallets.

## Storage Golem robot texture and head model

- [ ] Examine an ordinary Golem in first and third person: its head must
      **touch the torso**, without a one-pixel air gap or obvious z-fighting.
- [ ] Inspect all four Golem models locked to the moving pallet while idle,
      lifting, turning, and walking. Heads should stay seated on their torsos.
- [ ] Inspect the slate robot skin on all four sides: dark mechanical visor
      with twin cyan optical sensors, front chest access panel, seams,
      metallic rivets, and visibly separate shoulder/knee joints.
- [ ] Verify hands look oily and work-worn, and feet have brown-black
      grime concentrated on boots/soles; no skin transparency or UV bleed.
- [ ] Compare a shelf worker and attached pallet worker side by side:
      both should use exactly the same 64x64 worn slate robot texture.
- [ ] Load another resource pack and reload resources; the Golem must
      still render cleanly with no missing-purple/pink textures.

## One moving pallet with four locked, animated Golems

- [ ] Enlist exactly four real Storage Golems and right-click a glass-wrapped
      ground pallet or a sealed PTP. All four must reach distinct outer
      loading positions **before** the pallet rises.
- [ ] As it lifts, verify the four original independent entities disappear
      and ONE wide moving pallet entity takes over, with four Golem models
      firmly attached to the pallet's corners.
- [ ] Rotate, walk, sprint and change elevation. All four attached golems
      turn and translate with the pallet; their legs animate as a team.
      **No drifting or golems left behind** during movement.
- [ ] Observe the raised glass pallet from all four sides. Its cargo models
      should remain attached to the same root frame without z-fighting.
- [ ] Walk around corners, doors, narrow aisles and uneven terrain. One
      collision/pathfinding body should respect obstacles and avoid clipping.
- [ ] Query the Storage Golems tab while carrying: it should show one
      four-Golem team task, not four independent moving workers.
- [ ] Deliver to a completely empty rack; ensure the exact pallet and all
      216 slots transfer. Four independent EMPTY workers must reappear,
      resume following, and eventually return to their assigned shelves.
- [ ] Repeat on clear 2x2 ground; save/reload the world once mid-carry
      and once after delivery. The pallet inventory must have one owner
      at every stage.
- [ ] Destroy the moving pallet in a BACKED-UP Creative test world:
      exactly one pallet + its real cargo + four Golem recovery tokens
      should drop, not duplicate models with live inventories.
- [ ] Test PTP pickup, remote ground pickup and failed destinations. A
      rejected location must leave cargo on the same moving entity.
- [ ] Logout during transit and rejoin without forcing chunk loads.
      Verify the moving pallet pauses safely and resumes when reloaded.

## Portable wrapping and off-site Golem pallet pickup

- [ ] Craft a **Portable Pallet Wrapper** from 4 glass panes, 4 iron ingots
      and 1 copper ingot (glass at four corners, iron on sides, copper center).
      The same reusable tool should remain after wrapping.
- [ ] Place a free-standing wooden, iron, copper or plastic pallet anywhere
      (including outside the connected warehouse network). Fill several
      inventory slots. Right-click with the portable wrapper: four clear
      glass sides should enclose the cargo. Right-click again to unwrap.
- [ ] Wrap a pallet in a rack or an assembled PTP with the same tool;
      existing PTP Wrap Units and shears should remain functional.
- [ ] Link the Scanner at a Rack Computer and enlist 4 docked Golems.
      Lead them to a remote, wrapped **ground pallet**.
- [ ] Right-click ANY of its four ground blocks with the Scanner. Workers
      rally at reachable outer edges first; all four must arrive before
      the pallet disappears from the floor.
- [ ] Confirm the Golems form a square around **ONE raised 2x2 pallet**
      while following the player, all four using elevated carrying-arm poses.
      The lead renderer alone draws the pallet, wrapped glass and up to 27
      visible cargo crates; no four-fold duplicate meshes.
- [ ] Test after 1, 9, 18, 27 and 216 filled slots. Visual representation
      caps at 27 while the server saves all 216 item stacks exactly.
- [ ] Lead crew to a distant empty rack, then a clear 2x2 ground destination
      on a second trip. Check exact item counts, filters, wraps and material.
- [ ] Try clicking an unwrapped ground pallet with the Scanner; it must
      reject the job without moving the source or stealing any items.
- [ ] Break the ground pallet while Golems are walking to it. The crew must
      stop the job without stealing/duplicating stock.
- [ ] Repeated right-clicks while a pickup is active must not create two
      pallet owners or let a second crew lift the same source.
- [ ] Save/reload during pickup, while following, and after delivery; verify
      the 216-slot owner stays singular and visible cargo is restored.
- [ ] Test Golem death or shift-recall while carrying; items/pallet recover
      exactly once and the floating pallet visually disappears.
- [ ] Return empty crew to warehouse and dismiss from original Computer.

## Standalone pallet access and rack loading clearance

- [ ] Place wooden, iron, plastic, and each copper-age pallet directly on
      the ground, outside a rack and outside any PTP.
- [ ] Right-click every part of the 2×2 pallet with an **empty hand**.
      The same 216-slot inventory must open from all four parts.
- [ ] Repeat while holding an ordinary item, including a full item stack.
      The pallet inventory must open without placing/consuming the held item.
- [ ] Test the farthest corner of a 2×2 pallet. The menu should stay open
      when standing by that corner, not close because the controller is
      diagonally distant.
- [ ] Add and remove items on both pages, including shift-click transfers;
      close/reopen and restart the world to confirm stored contents persist.
- [ ] Shear a glass-wrapped ground pallet; verify the glass comes off while
      inventory remains. Crouch-right-click while holding building blocks
      should still allow normal deliberate placement.
- [ ] Place a pallet inside a rack and confirm rack inventory interaction
      remains unchanged; move a pallet with four Golems from PTP to ground,
      then open all four ground cells.
- [ ] Check the rack's front opening: there are **no horizontal crossbars
      between levels 1–2 and 2–3** on its front face (all four facings).
      Outer posts and back/side bracing remain.
- [ ] Verify the lowest level has narrow support-grill strips under the
      pallet across the complete 2×2 footprint, and that visual collision
      matches the open front rather than producing invisible crossbars.

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
