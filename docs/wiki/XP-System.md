# XP Storage and Repair

[← Home](Home.md) · [Recipes](Recipes.md)

Caszual Additions has four connected XP devices.

| Device | Purpose |
| --- | --- |
| **XP Tank** | Stores experience in a linked network; each tank entity has a raw XP-point capacity of **30,970**, corresponding to the XP needed for level 100 from level 0 |
| **XP Charger** | Accepts a damaged Mending item and uses nearby network XP for repairs |
| **XP Drain** | A floor-level device that takes XP from players above it and feeds the network |
| **XP Shower** | Delivers available stored XP to a player underneath; right-click to start or stop |

Tanks have visible proportional fill states and connect with neighboring XP components.

## Recipes

Letters are explained below each grid. All grids are **3×3**.

### XP Tank

```text
I G I
G B G
I G I
```

I = iron ingot, G = glass, B = experience bottle.

### XP Charger

```text
I L I
E T E
I R I
```

I = iron ingot, L = lapis lazuli, E = experience bottle, T = enchanting table, R = redstone dust.

### XP Drain

```text
I H I
G B G
I R I
```

I = iron ingot, H = hopper, G = glass, B = experience bottle, R = redstone dust.

### XP Shower

```text
I I I
  E
  I
```

I = iron ingot, E = experience bottle (spaces represent empty slots).

## Charger operation

Use a damaged Mending item on an empty charger to place it inside. Use the charger again to retrieve the item. An on-screen status shows the item and connected XP network levels.

## Shower operation

Right-click the shower to toggle an XP session. The shower operates from connected stored XP. Stand below it to receive the XP stream; interaction toggles it off.

[XP implementation](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/xp)
