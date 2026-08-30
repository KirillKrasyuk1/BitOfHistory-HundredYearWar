# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.22.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.22`

## v1.0.22

- **Fix launch crash** from 1.0.21 — `@ModifyVariable` invalid signature (Mixin 0.8.5 has no `@Local`)

## v1.0.21

- Fix instant capture at 2:1+ when defender player leaves (committed garrison, ratio lock, timer guards)

## v1.0.20

- Fix launch crash from 1.0.19 — split instance/static Mixin redirects for entity scan

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
