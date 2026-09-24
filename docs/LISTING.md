<p align="center">
  <img src="https://raw.githubusercontent.com/Fuzss/modresources/main/pages/commons/headers/about.png" alt="About">
</p>

<p align="center">
  <img src="docs/images/category-menu.png" alt="DonutLeaderboard category menu" width="400">
</p>

<p align="center">
  <a href="https://nightbeam.dev/support">
    <img src="https://i.imgur.com/R9eqHGA.png" alt="Support Nightbeam Studio" width="400">
  </a>
</p>

<p align="center">
  <strong>DonutLeaderboard</strong> — DonutSMP-style paginated leaderboards with chest GUIs, Vault balance, vanilla statistics, PlaceholderAPI categories, SQLite/MySQL storage, and Folia-safe async refreshes.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Donut%20SMP-Style-ffcc00?style=for-the-badge" alt="Donut SMP Style">
  <img src="https://img.shields.io/badge/Paper%20%7C%20Folia-Supported-3b82f6?style=for-the-badge" alt="Paper and Folia">
  <img src="https://img.shields.io/badge/Vault-Optional-22c55e?style=for-the-badge" alt="Vault">
  <img src="https://img.shields.io/badge/PlaceholderAPI-Custom%20boards-a855f7?style=for-the-badge" alt="PlaceholderAPI">
  <img src="https://img.shields.io/badge/SQLite%20%7C%20MySQL-HikariCP-f97316?style=for-the-badge" alt="Storage">
</p>

| Feature | DonutLeaderboard |
| --- | --- |
| Category menu + paginated board GUIs | Yes |
| Vault balance + vanilla stats categories | Yes |
| PlaceholderAPI custom categories | Yes |
| Daily / weekly / monthly / all-time periods | Yes |
| Async cached rankings (configurable interval) | Yes |
| SQLite + MySQL via HikariCP | Yes |
| Folia region/global schedulers | Yes |
| Compact jar (~100 KB); DB drivers via Paper `libraries:` | Yes |
| bStats (opt-in, plugin ID 24242) | Yes |

<p align="center">
  <a href="https://builtbybit.com/creators/nightbeamstudio.617578/">
    <img src="https://i.imgur.com/IeI5PsC.png" alt="Upgrade to Premium" width="400">
  </a>
</p>

# Category Menu

Opens from `/leaderboard`, `/lb`, `/top`, or `/leaderboards`. Pick a built-in category (balance, kills, deaths, playtime, blocks mined, mob kills, animals bred, fish caught, damage dealt) or custom PlaceholderAPI boards from config.

<p align="center">
  <img src="docs/images/category-menu.png" alt="Category menu" width="400">
</p>

# Leaderboard GUI

Six-row chest GUI with filler panes, distinct styling for ranks 1–3, configurable entry slots, previous/next pagination, back button, period toggle, and a viewer rank slot when you are not on the current page.

<p align="center">
  <img src="docs/images/leaderboard-populated.png" alt="Populated leaderboard" width="400">
</p>

# Periods & Resets

Toggle daily, weekly, monthly, and all-time from the board GUI (default slot 47). Period boundaries use the configured IANA timezone (`periods.timezone`). Operators can reset a category period with `/lb reset <category> <period>`.

<p align="center">
  <img src="docs/images/leaderboard-weekly.png" alt="Weekly period leaderboard" width="400">
</p>

<p align="center">
  <img src="docs/images/leaderboard-page2.png" alt="Leaderboard page 2" width="400">
</p>

# Placeholders

Requires **PlaceholderAPI**. Expansion identifier: `donutleaderboard`.

| Placeholder | Description |
| --- | --- |
| `%donutleaderboard_rank_<category>_<period>%` | Viewer's rank in a category/period (`0` if unranked) |
| `%donutleaderboard_top_<n>_name_<category>_<period>%` | Name at rank *n* |
| `%donutleaderboard_top_<n>_value_<category>_<period>%` | Value at rank *n* |
| `%donutleaderboard_top_<n>_rank_<category>_<period>%` | Rank number at position *n* |

Period tokens: `daily`, `weekly`, `monthly`, `all_time` (same as commands/config).

# Commands & Permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/leaderboard` | Open category menu | `donutleaderboard.use` |
| `/lb open [category] [period]` | Open a specific board | `donutleaderboard.use` |
| `/lb rank <player> <category> [period]` | Show a player's rank | `donutleaderboard.rank` |
| `/lb reload` | Reload config and messages | `donutleaderboard.admin.reload` |
| `/lb reset <category> <period>` | Reset stored scores for a period | `donutleaderboard.admin.reset` |
| `/lb refresh [category] [period]` | Force cache refresh | `donutleaderboard.admin.refresh` |

Aliases: `lb`, `top`, `leaderboards`. Defaults: `use` and `rank` are **true**; admin nodes are **op**.

# Storage

Default **SQLite** file (`storage.sqlite.file`, default `leaderboard.db`). Optional **MySQL** with HikariCP pool settings under `storage.mysql`. Schema migrations run automatically on enable. HikariCP, MySQL Connector/J, and SQLite JDBC are declared in `plugin.yml` `libraries:` and downloaded by Paper/Spigot 1.16.5+ at runtime (not shaded into the plugin jar).

# Performance & Safety

Rankings refresh on a background executor (`cache.refresh-seconds`, minimum 15). Refreshes are deduplicated per category/period while in flight. GUI clicks are handled on the appropriate thread; Folia uses region/global schedulers when detected. Reload closes open leaderboard inventories before applying config.

# Configuration

Key files: `config.yml` (categories, GUI layout, storage, cache, periods, bStats toggle) and `messages.yml` (MiniMessage and `&` legacy strings). Built-in categories and custom PlaceholderAPI categories live under `categories.builtin` / `categories.custom`. GUI: rows, titles, entry slot list, filler material, viewer rank slot (default 48), period toggle slot (default 47).

# Development

Apache License 2.0. Build with `./gradlew build` (Java 17 bytecode, `--release 17`). Tests: unit/MockBukkit (`./gradlew test`), embedded MariaDB storage (`./gradlew integrationTest`), multi-version server smoke (`./gradlew smokeServerBoot`). Compiled against Paper API 1.20.1; no NMS.

# Why Choose DonutLeaderboard

* Matches Nightbeam Donut plugin conventions (config migration, messages, GUI patterns, Folia abstraction).
* Keeps ranking I/O and Vault/PAPI lookups off the main/region thread during refreshes.
* One small jar for every supported version from **1.20.1** through **26.3** (Java 17 plugin bytecode; use Java 25 on the host for Paper 26.3).

# Compatibility

* **Minecraft:** 1.20.1 – 26.3 on Spigot, Paper, and Purpur (Paper-family first-class).
* **Folia:** Supported (`folia-supported: true`).
* **Java:** 17+ on the server; Paper **26.3** builds typically run on **Java 25**.

# Requirements

* Paper, Purpur, or Spigot **1.20.1–26.3**
* Java **17+** (Java **25** for 26.3 hosts)
* Optional: **Vault** (balance category), **PlaceholderAPI** (custom categories + expansion)

# Community & Support

Discord: https://discord.gg/TJc3CxR4jK · Website: https://nightbeam.dev · Store: https://nightbeam.dev/store · GitHub: https://github.com/MeherBenSalem · Modrinth: https://modrinth.com/organization/nightbeam · CurseForge: https://www.curseforge.com/members/nightbeamstudio/projects

# License

Licensed under the **Apache License 2.0** — see the bundled [LICENSE](LICENSE) file.

<p align="center">
  <a href="https://nightbeam.dev/community">
    <img src="https://i.imgur.com/xrZhnWD.png" alt="Join the Nightbeam community" width="400">
  </a>
</p>

<p align="center">
  <a href="https://discord.gg/TJc3CxR4jK">Discord</a> ·
  <a href="https://www.youtube.com/@nightbeamstudio">YouTube</a> ·
  <a href="https://github.com/MeherBenSalem">GitHub</a> ·
  <a href="https://modrinth.com/organization/nightbeam">Modrinth</a> ·
  <a href="https://nightbeam.dev">Website</a>
</p>
