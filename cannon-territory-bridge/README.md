# Cannon Territory Bridge

Forge 1.20.1 bridge mod for the **Cannon** modpack.

## Install

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-territory-bridge-5fac/cannon-territory-bridge/release/cannon_territory_bridge-1.0.27.jar

Delete all older `cannon_territory_bridge-1.0.x.jar`, verify log: `cannon_territory_bridge 1.0.27`

## v1.0.27

- **Siege capture no longer stalls while the defender hides on the claim.** Ownership transfers when claim HP hits 0 and the 5:1 ratio still holds. Surviving NPCs / defending player presence do not block transfer.
- Sticky unloaded garrison still counts for the **ratio** (leaving the claim does not instantly zero defenders).
- After a successful capture, `removeActiveSiege` is always allowed to tear the siege down.

## v1.0.26

- **Crash fix:** eating food / finishing any item use no longer crashes. `LivingEntityUseItemEvent.Finish` is not cancelable; the handler now only runs for HYW staff/scrolls and never calls `setCanceled`.
- Charge start for staff/scroll is canceled via `LivingEntityUseItemEvent.Start` outside own claim.

## v1.0.25

- **Capture ratio 5:1** — siege timer / HP drain starts only when attackers are **5×** defenders (was 2:1)

If you already have a world config, edit `config/cannon_territory_bridge-common.toml`:

```toml
captureAdvantageRatio = 5.0
```

## Build

```bash
cd cannon-territory-bridge && ./gradlew build
```
