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

## Custom head search

The vending machine searches the **official public HeadDB API** using short,
paginated requests. It no longer downloads the entire legacy catalog before
the first search. Each request fetches only the pages needed to fill the
vending screen's seven result rows.

- Search by name or keywords; switch categories using the separate HeadDB
  category listing. Categories are cached for an hour.
- **Refresh** retries the current custom-head search if a previous request
  failed or timed out.
- The server sends clear status/errors back to the GUI rather than leaving
  the screen indefinitely in a loading state.
- Purchases are authorized using head texture hashes that were returned
  from the real HeadDB catalog to this server. Arbitrary client-supplied
  texture hashes are never accepted.
- The catalog service runs at [headdb.net](https://headdb.net/). The **server
  host**, not just the client PC, must be able to reach
  `https://headdb.net/api/v1/heads` and `/api/v1/categories`.
- Custom-head search, non-native vanilla mob heads and texture previews
  require the HeadDB service to be accessible. Native vanilla skulls and
  normal player-name heads do not require the HeadDB catalog.

**Connectivity check:** If the GUI says that HeadDB could not be reached,
check the server's outbound HTTPS/DNS access and use Refresh. HeadDB is an
independent third-party service; downtime and service limits are outside
Caszual Additions' control.

[HeadDB API documentation](https://headdb.net/docs/api)
