![About](https://raw.githubusercontent.com/Fuzss/modresources/main/pages/commons/headers/about.png)

**[Discord](https://discord.gg/TJc3CxR4jK) · [Premium plugins](https://nightbeam.dev/store) · [Support NightBeam](https://nightbeam.dev/support) · [NightBeam Pro](https://nightbeam.dev/memberships) · [All projects](https://nightbeam.dev/projects)**

> **Running a server?** NightBeam's premium plugins are on [BuiltByBit](https://nightbeam.dev/store): NightWatch Pro anti-cheat, Elo ranked leaderboards and more.

<img src="https://i.imgur.com/1rBhYTm.png" width="400" alt="DonutLeaderboard Category Menu" />

**DonutLeaderboard** is a standalone leaderboard plugin for Spigot, Paper, Purpur and Folia servers running Minecraft 1.20.1 through 26.3. It gives players a clean, GUI-powered way to see who leads the server in balance, kills, playtime, blocks mined and more, with daily, weekly, monthly and all-time boards. Custom leaderboards can be built from any PlaceholderAPI placeholder, and a single lightweight jar covers every supported version.

# Features

| Feature                           | Description                                                                                                              |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Category Menu                     | Chest GUI that lists every built-in and custom leaderboard, opened with `/leaderboard`, `/lb`, `/top` or `/leaderboards` |
| Paginated Leaderboard GUI         | Six-row board with player heads, distinct styling for ranks 1 to 3, previous and next pages, and a back button           |
| Your Own Rank                     | A viewer rank slot shows where you stand, even when you are not on the current page                                      |
| Daily, Weekly, Monthly & All-Time | Switch periods with a button inside the GUI; period boundaries follow a configurable timezone                            |
| Built-In Categories               | Balance (Vault), kills, deaths, playtime, blocks mined, mob kills, animals bred, fish caught and damage dealt            |
| PlaceholderAPI Boards             | Create custom leaderboards from any PlaceholderAPI placeholder, plus a full placeholder expansion                        |
| SQLite & MySQL Storage            | SQLite by default, optional MySQL with HikariCP pooling and automatic schema migrations                                  |
| Async Cached Rankings             | Rankings refresh on a background executor so the main and region threads stay free                                       |
| Paper, Purpur, Spigot & Folia     | One jar for 1.20.1 through 26.3, with Folia region and global scheduler support                                          |
| Tiny Jar                          | Around 100 KB; database drivers are downloaded by the server at startup instead of being bundled                     |

<a href="https://builtbybit.com/creators/nightbeamstudio.617578/" rel="nofollow"><img src="https://i.imgur.com/IeI5PsC.png" width="350" alt="Upgrade to Premium" /></a>

# Category Menu

DonutLeaderboard opens with a category menu so players can jump straight to the board they care about.

The category menu includes:

- Every built-in category in one place
- Custom PlaceholderAPI categories from your config
- Configurable titles, slots, materials and lore
- Filler panes for a clean layout

Players open it with `/leaderboard`, `/lb`, `/top` or `/leaderboards`. No complicated command syntax is needed.

# Leaderboard GUI

Each category opens a paginated six-row leaderboard that is easy to read at a glance.

The leaderboard includes:

- Player heads showing rank, name and value
- Distinct styling for the top 3 players
- A viewer rank slot showing your own position
- Previous and next page buttons
- A back button to the category menu
- A period toggle button

One leaderboard inventory at a time is open per player, and all clicks, drags, number-key swaps and double-click collects are blocked to prevent item theft or duplication.

# Periods & Resets

Every category can be viewed across four periods:

- Daily
- Weekly
- Monthly
- All-Time

<img src="https://i.imgur.com/Sb8fMF5.png" style="display:block;margin-left:auto;margin-right:auto" width="400" alt="DonutLeaderboard Weekly Leaderboard" />

Players switch periods with the clock button inside the GUI. Period boundaries use the timezone set in `periods.timezone`, so resets happen at the right time for your community. Admins can also reset a category period manually with `/lb reset <category> <period>`.

# Placeholders

DonutLeaderboard includes a PlaceholderAPI expansion with the identifier `donutleaderboard`.

| Placeholder                                            | Description                                                  |
|--------------------------------------------------------|--------------------------------------------------------------|
| `%donutleaderboard_rank_<category>_<period>%`          | The viewer's rank in a category and period (`0` if unranked) |
| `%donutleaderboard_top_<n>_name_<category>_<period>%`  | Player name at rank *n*                                      |
| `%donutleaderboard_top_<n>_value_<category>_<period>%` | Value at rank *n*                                            |
| `%donutleaderboard_top_<n>_rank_<category>_<period>%`  | Rank number at position *n*                                  |

Period tokens are `daily`, `weekly`, `monthly` and `all_time`.

Use these in scoreboards, holograms, tab lists and chat to show your top players anywhere on the server.

# Commands & Permissions

| Command                                 | Description                      | Permission                       |
|-----------------------------------------|----------------------------------|----------------------------------|
| `/leaderboard`                          | Open the category menu           | `donutleaderboard.use`           |
| `/lb open [category] [period]`          | Open a specific leaderboard      | `donutleaderboard.use`           |
| `/lb rank <player> <category> [period]` | Show a player's rank             | `donutleaderboard.rank`          |
| `/lb reload`                            | Reload config and messages       | `donutleaderboard.admin.reload`  |
| `/lb reset <category> <period>`         | Reset stored scores for a period | `donutleaderboard.admin.reset`   |
| `/lb refresh [category] [period]`       | Force a ranking refresh          | `donutleaderboard.admin.refresh` |

Aliases are `lb`, `top` and `leaderboards`. `use` and `rank` are granted to everyone by default, and admin permissions default to operators. All commands include tab completion.

# Storage

DonutLeaderboard supports multiple storage options depending on the size of the server.

## SQLite

SQLite is the default storage option and is ideal for:

- Small servers
- Simple installations
- Servers that do not require an external database

It works out of the box without separate database infrastructure.

## MySQL

MySQL can be used for larger installations and server networks.

MySQL support includes:

- External database storage
- HikariCP connection pooling
- Automatic schema migrations
- Better suitability for larger networks

The database drivers are declared as server libraries and downloaded automatically at startup, which keeps the plugin jar tiny.

# Performance & Safety

DonutLeaderboard is designed to keep ranking work away from the main server thread.

The plugin includes:

- Asynchronous, cached ranking refreshes
- A configurable refresh interval (`cache.refresh-seconds`, minimum 15)
- Deduplicated refreshes so the same board is never refreshed twice at once
- Folia region and global scheduler support
- Protection against GUI item theft and duplication
- Open leaderboards closed safely on reload

# Development

DonutLeaderboard uses a modern Java plugin architecture.

Technical details include:

- Java 17 bytecode, running on Java 17 through Java 25
- Gradle build system
- Compiled against the Paper 1.20.1 API with no NMS
- Unit and MockBukkit GUI tests
- Embedded MariaDB storage tests
- Real server boot tests on Paper 1.20.1, 1.21.11 and 26.3, Purpur 26.3 and Folia 26.2

# Configuration

DonutLeaderboard is designed to be configurable for any server style.

Configurable options include:

- Built-in and custom categories
- GUI rows, titles and entry slots
- Filler material
- Viewer rank slot and period toggle slot
- Storage type and MySQL connection details
- Ranking refresh interval
- Period timezone
- Messages with MiniMessage and legacy `&` colour codes
- bStats (off by default)

Config files update automatically when new options are added, so upgrades never wipe your settings.

# Why Choose DonutLeaderboard?

1.  **Easy Setup**: Drop the jar into your `plugins` folder and use the default SQLite storage immediately.
2.  **Tiny Footprint**: Around 100 KB, with database drivers loaded by the server instead of bundled.
3.  **Player-Friendly UI**: Category menu, player heads, top-3 styling and your own rank, all without memorising commands.
4.  **Robust & Safe**: Async cached rankings plus protection against GUI theft and duplication.
5.  **Flexible**: Vault balance, vanilla stats and any PlaceholderAPI placeholder, across daily, weekly, monthly and all-time periods.
6.  **One Jar, Every Version**: Tested on real servers from 1.20.1 through 26.3, including Folia.

# Compatibility

- Spigot
- Paper
- Purpur
- Folia
- Minecraft 1.20.1 to 26.3
- Vault-compatible economies
- PlaceholderAPI
- SQLite
- MySQL

# Requirements

DonutLeaderboard requires a **Spigot, Paper, Purpur or Folia** server running Minecraft 1.20.1 to 26.3, with **Java 17 or newer** (Java 25 for 26.3 servers).

Optional:

- Vault and a Vault-compatible economy plugin, for the balance leaderboard
- PlaceholderAPI, for custom leaderboards and placeholders

If you encounter a bug or have a feature request, please join the Discord community or report it through the appropriate project page.

# License

DonutLeaderboard is licensed under the Apache License 2.0.

You are free to use, modify, and redistribute DonutLeaderboard in accordance with the terms of the Apache License 2.0.

<a href="https://nightbeam.dev/community" rel="nofollow"><img src="https://i.imgur.com/xrZhnWD.png" alt="Join the Nightbeam Community" /></a>

---

### More from NightBeam Studio

- **[Remnants: Ashen Legacy](https://www.curseforge.com/minecraft/modpacks/remnants-ashen-legacy)**: the story-driven soulslike RPG modpack built on NightBeam's mods
- **[Premium server plugins](https://nightbeam.dev/store)**: anti-cheat, ranked PvP, SMP setups and more on BuiltByBit
- **[NightBeam Pro](https://nightbeam.dev/memberships)**: the model library and NightBeam Forge credits, from $5 a month
- **[Discord](https://discord.gg/TJc3CxR4jK)**: support, bug reports, suggestions and early looks
- **[Support NightBeam](https://nightbeam.dev/support)**: help keep every mod free

**Follow:** [YouTube](https://www.youtube.com/@nightbeamstudio) · [Instagram](https://www.instagram.com/nightbeamstudio/) · [X](https://x.com/NightBeamLLC) · [Facebook](https://www.facebook.com/profile.php?id=61574823331471)
