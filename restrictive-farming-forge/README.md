# Restrictive Farming — Forge 1.20.1 port

Port of [Mrbysco/RestrictiveFarming](https://github.com/Mrbysco/RestrictiveFarming) (MIT) for **Forge 1.20.1**.

NeoForge 1.21+ uses `data_maps`; this port loads the same rules from JSON datapack files.

## Install

Copy `release/restrictive_farming-1.0.0-forge.jar` into your `mods/` folder.

## Datapack format

Path: `data/<namespace>/restrictive_farming/crop_whitelist/<any_name>.json`

```json
{
  "block": "minecraft:wheat",
  "biomes": ["minecraft:plains", "minecraft:river"],
  "growthReduction": -1.0,
  "isCrop": true
}
```

- `biomes` accepts biome IDs or tags like `#minecraft:is_overworld`
- `growthReduction`: `-1` = use config default (0.5), `1.0` = never grows outside whitelist
- Set `defaultRestrictions=false` in config to disable built-in nether/overworld split

## Config

`config/restrictive_farming-common.toml`

## Example (Cannon GoT)

See `example_datapack/` — wheat only on plains/river/meadow.
