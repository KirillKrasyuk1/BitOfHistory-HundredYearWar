# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.23.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.23`

## v1.0.23

- **Live force counts** — killed or removed units drop from totals immediately (death event + per-tick rescan)
- **Removed sticky max()/ratio-lock** that froze counts and broke the timer overlay
- **Unloaded garrison reserve** — only during active capture (HP draining), chunk-unloaded defenders still count until confirmed dead
- **setHealth / setSiegeSuccess guards** — no instant transfer when ratio not met or reserved garrison remains

## v1.0.22

- Fix launch crash from 1.0.21 (Mixin `@ModifyVariable` signature)

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
