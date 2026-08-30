# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Counts **HYW soldiers** for Recruits **claim sieges** via the owner's faction (no scoreboard edits on entities)
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Install (one step)

Download and replace JAR in `Cannon ______\mods\`:

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.10.jar

1. Delete all older `cannon_territory_bridge-1.0.x.jar`
2. Copy **1.0.10** into Cannon profile **mods** folder
3. Launch — verify log line `Cannon Territory Bridge loaded` and `config/cannon_territory_bridge-common.toml`

## v1.0.10

- **Fix crash** with HYW 0.7.1 (removed broken `spawnArmy` mixin; mobilization via Forge events)
- HYW garrison counts as defenders by owner faction
- Mobilization only on own claim chunks
- On-screen siege timer

## Keys

| Key | Action |
|-----|--------|
| **U** | Recruits faction |
| **M** | Claim map |
| **R** | Blocked (Recruits commands) |

## Build

```bash
cd cannon-territory-bridge
./gradlew build
```
