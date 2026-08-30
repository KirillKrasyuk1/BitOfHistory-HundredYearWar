# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.21.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.21`

## v1.0.21

- **Fix instant capture at 2:1+** when defender player leaves — committed HYW garrison counts even if chunk-unloaded
- **Ratio lock** — once capture starts, defender count for ratio does not drop when player walks off
- **Timer no longer resets** — blocks Recruits `resetHealth` / siege teardown while bridge armies remain

## v1.0.20

- **Fix launch crash** from 1.0.19 — split instance/static Mixin redirects for entity scan

## v1.0.19

- Remaining siege timer (HP sync), HYW-only force counts, no player-dependent capture

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
