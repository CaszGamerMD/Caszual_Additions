# XP Storage and Repair

[← Home](Home.md) · [Recipes](Recipes.md)

Caszual Additions has four connected XP devices plus one independent, portable **ground-placed XP Collector**.

| Device | Purpose |
| --- | --- |
| **XP Tank** | Stores experience in a linked network; each tank entity has a raw XP-point capacity of **30,970**, corresponding to the XP needed for level 100 from level 0 |
| **XP Charger** | Holds and repairs up to **three** damaged Mending items at once using connected tank XP |
| **XP Drain** | A floor-level device that takes XP from players above it and feeds the network |
| **XP Shower** | Delivers available stored XP to a player underneath; right-click to start or stop |
| **XP Collector** | Place it on the ground; vacuums nearby dropped XP orbs (6-block radius) and stores up to 30,970 points. Right-click with an empty hand to transfer its XP to yourself; sneak-right-click to read its fill. When broken, the collector keeps its stored XP for re-placement. |

Tanks have visible proportional fill states and connect with neighboring XP components. **Breaking an XP Tank preserves that tank's own stored XP in the dropped tank item.** Replacing it restores that exact amount (up to 30,970 points). Other connected tanks keep their own storage; an individual tank drop does not duplicate XP from the linked network.

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

### XP Collector

```text
  G
G B G
  I
```

G = glass, B = experience bottle, I = iron ingot. Place the collector on a solid floor in the work area. It automatically absorbs loose XP orbs within six blocks (and three blocks vertically). It does not draw XP directly from players or connected XP tanks. XP is preserved on the block and on its dropped item.

## Charger operation

Right-click with a damaged Mending item to occupy one of the three charger docks. All three repair independently from the same connected XP network. Empty-hand right-click retrieves a finished item first (or an unfinished one if none are ready); **sneak-right-click with an empty hand retrieves all three**. Hopper insertion can fill empty repair slots and extraction can take finished tools. The action bar and Jade show the occupied slots and tank XP.

## Shower operation

Right-click the shower to toggle an XP session. The shower operates from connected stored XP. Stand below it to receive the XP stream; interaction toggles it off.

[XP implementation](https://github.com/CaszGamerMD/Caszual_Additions/tree/main/src/main/java/com/caszgamermd/caszualadditions/xp)
