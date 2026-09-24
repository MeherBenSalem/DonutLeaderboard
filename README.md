# DonutLeaderboard

Production-quality Donut-style leaderboards for **Nightbeam Studio (NAIZO)** — paginated chest GUIs, async cached rankings, Vault + vanilla stats + PlaceholderAPI categories, and Folia-safe scheduling.

## Build

```bash
./gradlew build
```

Output: `build/libs/DonutLeaderboard-1.0.0.jar` (~100 KB plugin code + bStats; HikariCP, MySQL, and SQLite JDBC load at runtime via `plugin.yml` `libraries:` on Paper/Spigot 1.16.5+).

## Requirements

- **Server:** Spigot, Paper, or Purpur **1.20.1 through 1.21.x and 26.3** (Java 17+; Paper 26.3 builds target Java 25 on the host JVM).
- **Folia:** Supported where the Folia API is present (`folia-supported: true` in `plugin.yml`).
- **Optional:** Vault (balance category), PlaceholderAPI (custom categories + expansion).

## Version compatibility approach

- Plugin bytecode is compiled with **`--release 17`** so the same jar runs on 1.20.1 servers (Java 17) and newer runtimes (including Java 25 for 26.3).
- Compiled against **Paper API 1.20.1**; no NMS. Newer API surface (inventory views, Folia schedulers) is accessed via reflection or legacy fallbacks where needed.
- Inventory titles and item text use legacy serialization for broad MockBukkit/server compatibility; chat messages support **MiniMessage** and `&` legacy in `messages.yml`.

## Commands

| Command | Description |
| --- | --- |
| `/leaderboard` | Open category menu |
| `/lb open [category] [period]` | Open a category (`daily`, `weekly`, `monthly`, `all_time`) |
| `/lb rank <player> <category> [period]` | Show a player's rank |
| `/lb reload` | Reload config (admin) |
| `/lb reset <category> <period>` | Reset stored period scores (admin) |
| `/lb refresh [category] [period]` | Force cache refresh (admin) |

Aliases: `lb`, `top`, `leaderboards`.

## Permissions

| Permission | Default |
| --- | --- |
| `donutleaderboard.use` | true |
| `donutleaderboard.rank` | true |
| `donutleaderboard.admin.reload` | op |
| `donutleaderboard.admin.reset` | op |
| `donutleaderboard.admin.refresh` | op |

## PlaceholderAPI

Prefix: `%donutleaderboard_...%`

| Placeholder | Example |
| --- | --- |
| `%donutleaderboard_rank_<category>_<period>%` | `%donutleaderboard_rank_kills_daily%` |
| `%donutleaderboard_top_<n>_name_<category>_<period>%` | Top name |
| `%donutleaderboard_top_<n>_value_<category>_<period>%` | Top value |
| `%donutleaderboard_top_<n>_rank_<category>_<period>%` | Top rank label |

## Config reference

- `storage.type` — `SQLITE` (default) or `MYSQL`
- `cache.refresh-seconds` — async refresh interval (default 120)
- `periods.timezone` — IANA zone for daily/weekly/monthly boundaries
- `categories.builtin` / `categories.custom` — enable, slot, icon, placeholder
- `gui.*` — rows, titles, entry slots, filler, viewer rank slot
- `bstats.enabled` — default **false**

See `src/main/resources/config.yml` and `messages.yml` for defaults.

## Tests

```bash
./gradlew build          # unit + MockBukkit GUI tests
./gradlew integrationTest # MariaDB4j embedded MySQL (+ Testcontainers when Docker available)
./gradlew smokeServerBoot # real Paper/Purpur/Folia boots (see docs/SMOKE_RESULTS.md)
```

## Smoke / version matrix

**Verified on this VM (2026-09-24):** Paper **1.20.1**, **1.21.11**, **26.3**; Purpur **26.3**; Folia **26.2** — all **PASS**. See [docs/SMOKE_RESULTS.md](docs/SMOKE_RESULTS.md) and `build/smoke-report.txt`.

```bash
./gradlew smokeServerBoot   # downloads jars, boots matrix, writes build/smoke-report.txt
```

---

<!-- Nightbeam listing draft (screenshots omitted in repo; capture in-game before publish) -->

<p align="center">
  <img src="https://raw.githubusercontent.com/Fuzss/modresources/main/pages/commons/headers/about.png" alt="About">
</p>

<p align="center">
  <a href="https://nightbeam.dev/support">
    <img src="https://i.imgur.com/R9eqHGA.png" alt="Support Nightbeam Studio" width="400">
  </a>
</p>

**DonutLeaderboard** — lightweight DonutSMP-style leaderboards with paginated chest GUIs, Vault balance, vanilla stats, PlaceholderAPI custom boards, SQLite/MySQL storage, and Folia support.

| Feature | DonutLeaderboard |
| --- | --- |
| Paginated chest GUI + category menu | Yes |
| Vault balance + vanilla stats | Yes |
| PlaceholderAPI custom categories | Yes |
| Daily / weekly / monthly / all-time | Yes |
| Async cached rankings | Yes |
| SQLite + MySQL (HikariCP) | Yes |
| Folia-safe schedulers | Yes |
| Single deployable jar (DB libs via Paper `libraries:`) | Yes |

<p align="center">
  <a href="https://builtbybit.com/creators/nightbeamstudio.617578/">
    <img src="https://i.imgur.com/IeI5PsC.png" alt="Upgrade to Premium" width="400">
  </a>
</p>

# Leaderboard GUI

Opens from `/leaderboard` or `/lb`. Top 3 ranks use distinct styling; the viewer's rank appears in the configured slot.

# Category menu

Pick balance, kills, deaths, playtime, blocks mined, mob kills, and more. Custom categories are defined in config with PlaceholderAPI placeholders.

# Why Choose DonutLeaderboard

- Matches Nightbeam Donut plugin conventions (config migration, messages, GUI patterns, Folia abstraction).
- Does not block region or main threads during refreshes.
- Works from **1.20.1** through **26.3** with Java 17 bytecode.

# Compatibility

- Minecraft **1.20.1 – 26.3** (Spigot, Paper, Purpur)
- **Folia** (latest API available on your server)
- Java **17+** on the server; **Java 25** required for Paper **26.3** hosts

# License

Apache License 2.0 — see [LICENSE](LICENSE).

<p align="center">
  <a href="https://nightbeam.dev/community">
    <img src="https://i.imgur.com/xrZhnWD.png" alt="Join the Nightbeam community" width="400">
  </a>
</p>

Discord: https://discord.gg/TJc3CxR4jK · GitHub: https://github.com/MeherBenSalem · Modrinth: https://modrinth.com/organization/nightbeam
