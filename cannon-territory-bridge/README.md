# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.26.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.26`

## v1.0.26

- **Crash fix:** eating food / finishing any item use no longer crashes. `LivingEntityUseItemEvent.Finish` is not cancelable; the handler now only runs for HYW staff/scrolls and never calls `setCanceled`.
- Charge start for staff/scroll is canceled via `LivingEntityUseItemEvent.Start` outside own claim.

## v1.0.25

- **Capture ratio 5:1** — siege timer / HP drain starts only when attackers are **5×** defenders (was 2:1)

If you already have a world config, edit `config/cannon_territory_bridge-common.toml`:

```toml
captureAdvantageRatio = 5.0
```

## v1.0.24

- Defender UUID preservation during active capture, live vs ratio counts

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
