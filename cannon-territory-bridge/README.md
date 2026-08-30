# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Counts **HYW soldiers** for Recruits **claim sieges** via the owner's faction (no scoreboard edits on entities)
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Install (one step)

Download and replace JAR in `Cannon ______\mods\`:

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.18.jar

1. Delete all older `cannon_territory_bridge-1.0.x.jar`
2. Copy **1.0.18** into Cannon profile **mods** folder
3. Launch — verify log line `cannon_territory_bridge 1.0.18`

## v1.0.18

- **Fix launch crash** (Mixin static handler on `calculateSiegeSpeedPercent`)
- **Fix frozen siege timer** — capture progress and overlay now use the same ratio-based speed
- At **2:1** attackers:defenders capture runs at baseline speed; higher ratios accelerate (3:1 → 3×, etc.)
- **0 defenders** = slow baseline capture (not Recruits instant max speed); garrison still counts via sticky forces

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
