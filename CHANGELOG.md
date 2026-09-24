# Changelog

## 1.0.0 — 2026-09-24

- Initial release of DonutLeaderboard for Nightbeam Studio (NAIZO).
- Paginated chest GUI with category menu, viewer rank slot, and Donut-style layout.
- Built-in categories: balance (Vault), kills, deaths, playtime, blocks mined, mob kills, animals bred, fish caught, damage dealt.
- Custom PlaceholderAPI categories, daily/weekly/monthly/all-time periods, and scheduled resets.
- Async cached rankings with SQLite (default) or MySQL/MariaDB via HikariCP; versioned schema migrations.
- PlaceholderAPI expansion, full command set, granular permissions, MiniMessage + legacy messages, config migration.
- Folia-safe scheduler abstraction; single shaded jar; Java 17 bytecode for broad server compatibility.
