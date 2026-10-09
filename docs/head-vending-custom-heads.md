# Custom head vending

The Player Head Vending Machine has three tabs:

- **Custom Heads:** Search decorative heads by name, category, or tags. Cycle
  categories with the category button; use Previous/Next to browse results.
  Each result shows the custom head texture.
- **Player Heads:** Enter a Minecraft username to resolve a normal player head.
- **Mob Heads:** Choose from vanilla skull/head items.

Each purchase costs one emerald, including custom heads. The server validates
that the machine still exists and is nearby, verifies the selected custom head
against the loaded catalog, and performs the emerald transaction.

## Database

The server downloads the public compatibility catalog from
[HeadDB](https://headdb.net/). The endpoint is part of HeadDB's public v1 API
and does not require an API key. Only names, categories, tags for searching,
and texture hashes are used. The mod does **not** ship a copy of the catalog.

- Internet access to `headdb.net` is required for the first custom-head
  search after server startup.
- The downloaded catalog is shared across players in server memory for six
  hours. It is not fetched on every machine opening.
- After a failed refresh, previously loaded data remains available; initial
  failures show an error and are retried after two minutes.
- Player and vanilla mob-head vending continue to work if the external catalog
  is unavailable.
- The server never accepts an arbitrary custom texture from a client. Custom
  purchases supply a hash that must match a loaded catalog entry.

HeadDB is an independent service. Catalog availability and updates depend on
its maintainers; it is not part of Minecraft or Mojang.
