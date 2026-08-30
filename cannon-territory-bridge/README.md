# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Counts **HYW soldiers** for Recruits **claim sieges** via the owner's faction (no scoreboard edits on entities)
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Install (one step)

Download and replace JAR in `Cannon ______\mods\`:

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.9.jar

1. Delete all older `cannon_territory_bridge-1.0.x.jar`
2. Copy **1.0.9** into Cannon profile **mods** folder
3. Launch once — verify log line `Cannon Territory Bridge loaded` and file `config/cannon_territory_bridge-common.toml`
4. No need to recreate the world

## Siege (v1.0.9)

- HYW garrison counts as defenders by **owner Recruits faction** (works when you leave the claim)
- **Mobilization** (Conqueror's Staff + scrolls) only on **your own** claim (`requireOwnClaimToMobilize`)
- On-screen siege timer (`showSiegeTimer`), min capture ~3 min, garrison holds while `defenders >= attackers`

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
