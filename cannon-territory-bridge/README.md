# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## What it does

- Syncs **HYW combat units** to the owner's **Recruits faction team** so HYW armies count for **claim sieges**
- Mirrors **Recruits diplomacy** into HYW `RelationSystem`
- **Disables Recruits settlement/army layer**: no recruit NPC spawns, no hiring, no villager claim takeover, no command/hire UI
- Keeps Recruits: **faction creation**, **claim map**, **diplomacy**, **sieges**

## Dependencies

- Minecraft Forge 1.20.1
- [Villager Recruits](https://modrinth.com/mod/villager-recruits)
- [Hundred Years Warfare](https://modrinth.com/mod/hundred-years-warfare)

## Install (Cannon instance)

Copy the built JAR into your mods folder (remove any older `cannon_territory_bridge-1.0.0*.jar` first):

```
C:\Users\dkras\curseforge\minecraft\Instances\Cannon ______\mods\cannon_territory_bridge-1.0.1.jar
```

## Recruits config (recommended)

In `config/recruits-common.toml` set:

```toml
[Patrols]
ShouldRecruitPatrolsSpawn = false

[Villager]
NobleVillagerSpawns = false
MaxSpawnRecruitsInVillage = 0
```

The bridge mod blocks entities and UI even without these, but the config avoids wasted spawn attempts.

## In-game flow

1. **R → Faction** — create/join faction, diplomacy
2. **Map key** — buy and manage claims
3. **HYW** — build settlement, hire army, fight with RTS
4. Mark enemy faction → bring **10+ HYW soldiers** into enemy claim for **10 minutes** → claim captured

## Config

`config/cannon_territory_bridge-common.toml`

- `hyw_sync.syncHywTeams` — team sync for siege counting (default: true)
- `recruits_strip.blockRecruitsEntities` — remove Recruits NPCs (default: true)
- `hyw_sync.countMountedHorsesForSiege` — count horses separately in sieges (default: false)

## Build

```bash
cd cannon-territory-bridge
./gradlew build
```

Output: `build/libs/cannon_territory_bridge-1.0.0.jar`
