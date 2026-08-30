# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Counts **HYW soldiers** for Recruits **claim sieges** via the owner's faction (no scoreboard edits on entities)
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Install (one step)

Download and replace JAR in `Cannon ______\mods\`:

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.17.jar

1. Delete all older `cannon_territory_bridge-1.0.x.jar`
2. Copy **1.0.17** into Cannon profile **mods** folder
3. Launch — verify log line `cannon_territory_bridge 1.0.17`

## v1.0.17

- **Fix instant capture** when defending player leaves/dies but HYW garrison stays inside the claim
- Zero defenders no longer triggers max capture speed (Recruits default)
- HYW owned by claim owner faction always counts as defenders, regardless of player location

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
