# Custom head vending

The Player Head Vending Machine has five tabs:

- **Custom Heads:** Search decorative heads by name, category, or tags. Cycle
  categories with the category button; use Previous/Next to browse results.
  Each result shows the custom head texture.
- **Player Heads:** Enter a Minecraft username to resolve a normal player head.
- **Mob Heads:** Browse vanilla living mobs (paginated). Mobs with native skulls use their vanilla items; other mobs use a matching online HeadDB skin where a matching name is available.
- **Favorites:** Star a head to keep it in a per-player favorites list (up to 64 entries).
- **History:** Rebuy recently purchased heads from your personal history (up to 30 distinct entries).

Survival purchases cost one emerald each, including custom heads. Players in Creative mode receive heads for free. The server validates
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
- Player heads and the six native vanilla skull types continue to work without the external catalog. Other vanilla mob head textures require a matching entry from the online catalog.
- The server never accepts an arbitrary custom texture from a client. Custom
  purchases supply a hash that must match a loaded catalog entry.

HeadDB is an independent service. Catalog availability and updates depend on
its maintainers; it is not part of Minecraft or Mojang.
