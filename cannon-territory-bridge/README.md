# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Syncs **HYW combat units** to the owner's **Recruits faction team** so HYW armies count for **claim sieges**
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Install (Cannon instance) — one step

1. Download **`cannon_territory_bridge-1.0.3.jar`** from the PR `release/` folder
2. Open folder: `Cannon ______\mods\`
3. **Delete** all older `cannon_territory_bridge-1.0.x.jar`
4. **Copy** the new JAR into `mods`
5. **Delete** (optional but recommended after upgrade) `config\cannon_territory_bridge-common.toml` — Forge recreates it with safer defaults
6. Restart Cannon in CurseForge

Direct download (PR branch):

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.3.jar

## Keys in game

| Key | Action |
|-----|--------|
| **U** | Recruits faction (create/join) |
| **M** | Claim map (Overworld) |
| **R** | Blocked intentionally (Recruits command screen) |

## In-game flow

1. **U** → create faction (10 emeralds + banner)
2. **M** → buy claim (64 emeralds)
3. **HYW** → settlement, hire army (**1 unit at a time**, wait a few seconds between hires)
4. Enemy diplomacy → **10+ HYW soldiers** in enemy claim for **10 minutes** → capture

## Config

`config/cannon_territory_bridge-common.toml` (auto-created; delete file to reset)

- `hyw_sync.syncHywTeams` — team sync for sieges (default: true)
- `hyw_sync.teamSyncIntervalTicks` — re-sync interval (default: **200**, was 40 in older configs)
- `hyw_sync.teamSyncRadius` — scan radius around players (default: 128)
- `recruits_strip.blockRecruitsEntities` — block Recruits NPCs (default: true)

## Build

```bash
cd cannon-territory-bridge
./gradlew build
```

Output: `build/libs/cannon_territory_bridge-1.0.3.jar`
