CASZUAL ADDITIONS - COSMETIC OPTIONS
====================================

This file lists the unique special cosmetic behaviors currently implemented.
Ordinary block items can also be placed in cosmetic slots; those generic block-head/outfit uses are not individually listed here.

HEAD COSMETICS
--------------
End Rod / Colored End Rods
- End rod mounted on the head at a backward angle.
- Colorful End Rod variants use their own block appearance.

Lightning Rod
- Lightning rod mounted on the head at a backward angle.

Flowers
- Flower crown generated from items/blocks tagged as flowers.
- Supports vanilla flowers and compatible modded flowers using the flower tag.

Crystal / Amethyst Clusters
- Crystal-spike mohawk/spine cosmetic.
- Vanilla amethyst clusters plus compatible tagged/modded crystal clusters are recognized.

Mob Heads
- Mob-head cosmetics hide the player's normal head so custom head models can display correctly.

Bone
- Skeleton-style head/body cosmetic set using a custom 3D bone-block-textured model.
- Bone is accepted in all four cosmetic slots; equipped sections control which skeleton sections appear.

Blaze Powder
- Persistent flame halo/wisps around the head.

Nautilus Shell
- Asymmetric ocean-floor shell arrangement around the head.

CHEST / BODY COSMETICS
----------------------
End Rod / Colored End Rods
- Rod arms plus an end-rod rib-cage effect.

Bone
- 3D skeleton torso, ribs, spine, and arms using bone-block texture.

Nautilus Shell
- Shells across the torso with kelp accents on the arms.

LEGS / FEET COSMETICS
---------------------
End Rod / Colored End Rods
- End-rod legs.

Bone
- 3D skeleton legs using bone-block texture.

Nautilus Shell
- Kelp on the legs and shell/debris details around the feet.

FLOWER FEET
- Moving leaves a colored flower-dust trail.
- Trail color follows known flower colors; compatible unknown/modded flowers use a green fallback.

Blaze Powder Feet
- Moving leaves small flames/embers with occasional lava sparks.

Echo Shard Feet
- Moving leaves Sculk Soul particles.

Slime Ball Feet
- Moving leaves slime particles.

Gunpowder Feet
- Moving leaves a smoke trail.

MULTI-SLOT / TRANSFORMATION COSMETICS
-------------------------------------
Snow Golem Transformation
Required cosmetic combination:
- Head: Carved Pumpkin
- Chest: Stick
- Legs: Snow Block
- Feet: Snow Block
Effect:
- Replaces the visible player body with a controllable Snow Golem-style model.
- Includes special arm/held-item handling.

Bone Skeleton Set
- Bone can be equipped across Head, Chest, Legs, and Feet.
- The equipped sections combine into the custom animated 3D skeleton appearance.

Crystal Spine
- Crystal cluster head cosmetic creates head spikes plus a Godzilla-like crystal spine down the upper body.

OTHER COSMETIC SYSTEM FEATURES
------------------------------
Hide Armor
- Cosmetic menu option can hide normal equipped armor visually for all synced players while retaining its gameplay stats.

Armor Stand Cosmetics
- Armor Stands support rendering of the special cosmetic system.
- Sneak-right-clicking an Armor Stand with supported head cosmetics transfers one cosmetic item to its head slot.

Generic Block Cosmetics
- Block items are accepted by the cosmetic slot system.
- Solid block head cosmetics reduce the underlying player head scale slightly so the block can cover it while retaining compatibility with non-solid/custom models.

Last updated from current main cosmetic implementation.
