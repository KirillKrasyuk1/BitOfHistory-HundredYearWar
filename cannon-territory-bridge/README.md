# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.19.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.19`

## v1.0.19

- **Timer shows remaining time** — server syncs claim HP every siege tick (Recruits only updated players in-party)
- **Players never affect siege** — Recruits scanned only `ServerPlayer` in claim; now scans HYW armies only
- **No instant capture on player leave** — blocks premature `setUnderSiege(false)` and invalid `setSiegeSuccess` when HYW armies still hold the ratio

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
