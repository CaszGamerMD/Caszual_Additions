# Getting Started

[← Home](Home.md) · [Recipes](Recipes.md)

## Installation

1. Install **Minecraft Java Edition 26.2** and a matching **Fabric Loader**.
2. Install the compatible **Fabric API**.
3. Download the current Caszual Additions mod JAR from the [GitHub releases](https://github.com/CaszGamerMD/Caszual_Additions/releases).
4. Place the JAR in your Minecraft `mods` folder, then start the game.
5. For multiplayer, install the matching mod build on the server and on clients.

Do not put a `-sources.jar` in your `mods` folder. Do not leave different versions of the same mod installed.

## Finding the content

Use the **Caszual** creative inventory tab. Compatible standalone Caszual-family mods can contribute their items to this tab using the `caszual_additions:caszual_content` item tag.

Recipes can be browsed in JEI when a compatible installation is present; the exact recipes are also listed in the [Recipes page](Recipes.md).

## Modules and namespaces

The distributed mod includes several modules. Registry IDs are grouped under `caszual_additions:`, `caszutils:`, and `colorful_rods:`; these do **not** necessarily mean three separate JARs are required.

## Multiplayer and networking

Pallet inventories, vending purchases, cosmetics and XP devices have server-side state. Use the mod on both the client and server. The online decorative head search requires outbound network access by the server and caches downloaded data; other vending tabs continue to work without it.

## Important links

- [GitHub releases](https://github.com/CaszGamerMD/Caszual_Additions/releases)
- [Source repository](https://github.com/CaszGamerMD/Caszual_Additions)
- [Issue tracker](https://github.com/CaszGamerMD/Caszual_Additions/issues)
- [Developer integration notes](https://github.com/CaszGamerMD/Caszual_Additions/blob/main/COMPANION_MOD_INTEGRATION.md)
