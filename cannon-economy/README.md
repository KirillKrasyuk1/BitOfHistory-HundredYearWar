# Cannon Economy

Regional resources, admin deposits, biome farming rules, and trade routes for the Cannon GoT modpack.

## Features

- **Admin deposits** — `/cannoneconomy deposit add gold 48` at map points (Casterly Rock, mines, etc.)
- **Farming** — wheat/carrots/potatoes only on plains/river within 4 blocks of water (configurable)
- **Trade posts** — place block, `/cannoneconomy post name kings_landing`, create routes between named posts
- **Caravans** — automatic item transport with optional tariff and Recruits embargo hooks

## Commands (OP level 2)

```
/cannoneconomy deposit add <gold|iron|silver|gems|coal|dragonsteel|fertile> [radius] [label]
/cannoneconomy deposit list
/cannoneconomy deposit remove <uuid>

/cannoneconomy post name <name>
/cannoneconomy route create <route> <fromPost> <toPost> <item> <amount> [interval] [tariff]
/cannoneconomy route list
/cannoneconomy route remove <name>
```

## Build

```bash
cd cannon-economy && ./gradlew build
```

JAR: `build/libs/cannon_economy-1.0.0.jar`

## Companion datapack

See `../cannon-datapacks/regional-resources/` and `../docs/ECONOMY_GOT.md`.

## Recommended mods

Custom Ore Veins, Regional Ore Veins, Restrictive Farming, Trotting Wagons, Caravans & Convoys, Better Caravans, Ice and Fire.
