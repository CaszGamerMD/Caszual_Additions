# Anti-Warden Tools

[← Home](Home.md) · [Recipes](Recipes.md)

Three survival-craftable tools for navigating the Deep Dark. All three are included in the Caszual creative tab and the Barrel of Caszual Fun.

## Wool Night-Vision Earmuffs

Craft three wool across the top crafting row and one wool at each end of the second row (helmet shape). Equip in the **helmet armor slot**.

While worn, the hood **prevents both Darkness and Blindness** from being applied. The Warden's attack still deals damage; the hood only guards against the screen-darkening effects.

## Silent Wool Slippers

Craft with wool in the ordinary boot pattern. Equip in the **boots armor slot**.

Game-event vibrations produced by the wearer will be **ignored by nearby vibration listeners at most seven blocks from the player**. This targets sculk sensors, shriekers and the Warden's vibration detection; it does not silence other players. It is not general soundproofing or invisibility and does not block direct line of sight, sniffing, or a Warden that already knows your location.

## Warden Snare Trap

Craft with three iron blocks across the bottom row, a sculk sensor between two iron ingots in the middle row, and iron ingots in the two top corners.

Place the trap as a low-profile metal plate on a solid floor. Only a **living Warden** standing over the plate will spring the jaws. The trapped Warden is pinned in place and takes **two health points (one heart) every second**. Each successful hit releases **two experience points** above the trap for players to collect. The trap resets automatically once its Warden dies or disappears, and breaking the trap releases its restraint.

The trap does **not** catch other monsters, players, or animals. The Warden can still attack nearby players, so trapping it does not make it harmless.

## Trap acoustic suppression

While the trap has a living Warden captive, the Warden is silenced and sculk vibration listeners inside a nine-block radius ignore triggers. Outside that radius, ordinary Warden and sculk behavior continues. When the trap releases its Warden, the prior silent state is restored. This is distinct from the wool slippers' wearer-specific seven-block vibration protection.

The helmet is styled as cushioned earmuffs with dual green night-vision-looking goggles. Its night-vision lenses are cosmetic; the gameplay effect continues to prevent Darkness and Blindness. The boots look like padded wool slippers.

## Technical notes

- Protection is applied server-side for multiplayer. The client also sees the helmet equipment/appearance.
- The trap saves its captive's UUID and damage progress. It never spawns a Warden or awards XP just for waiting; XP is tied to actual inflicted damage.
- The boot effect checks the distance to the vibration receiver, not the player's distance to the trap or the visible particle.
- Live game testing is still needed for sculk listeners, trap capture, and placement in cramped ancient-city layouts.
