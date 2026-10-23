# Caszual Mail — Blue Mailbox and PO Box

Two craftable blocks in **Caszual Additions** (Fabric 26.2):

- **Blue Mailbox**: A blue USPS-inspired street-side collection box. Right-click to choose a recipient from the server's recorded player history. Click a player head, place up to nine outgoing item stacks into the private staging slots, then click **SEND MAIL**. Recipient pages can be changed with the arrow controls.
- **PO Box**: A compact metal mail locker. Right-click to open **your own private 27-slot inventory**, regardless of which PO Box block you visit, in any dimension. Only your UUID can access these slots; opening somebody else's block still opens *your own* inventory rather than theirs.

## Delivery rules

Recipients may be offline. Sending changes the recipient's saved PO Box immediately on the server. One click attempts the **entire** contents of the nine staging slots: items merge with identical existing stacks before using empty slots. If the recipient's 27 slots cannot hold everything, the operation is rejected with no partially sent items and no sender inventory loss. The sender can remove any unsent items or close the screen; closing puts remaining stacks back into the sender's inventory. No inventory contents live in the physical mailbox or PO Box block, and breaking a block never destroys private mail.

All PO Boxes on the server share the same world save, keyed to player UUID, not player name, and are separate from vanilla Ender Chest contents. Data persists in the Overworld's saved-data directory. Concurrent sends use the server thread. The list of recipients records players when they join the server while this feature is installed, including subsequent offline recipients. Earlier visitors who have **not** logged in since mail was installed need to rejoin once to appear in the list. The system does not guess offline UUIDs from player names.

The design uses nine outgoing slots and 27 private PO Box slots by default, no postage cost, and no physical distance limit. P.O. Boxes have no owner item or password: the *inventory access* itself is private on a per-player basis like an Ender Chest.

## Recipes
- Blue Mailbox: blue concrete, chest, iron ingots
- PO Box: iron ingots, chest, gold nuggets
