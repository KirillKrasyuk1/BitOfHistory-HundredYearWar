# Cannon — экономика в духе Игры Престолов

Сдвиг от «античности» к фэнтези (Ice and Fire, Recruits-фракции, HYW армии). Цель: **разные регионы дают разные ресурсы**, торговля между королевствами — осмысленная, а не «повозка с сундуком».

---

## Стек модов (рекомендация для CurseForge-инстанса)

### Региональная генерация руд

| Мод | Роль | Forge 1.20.1 |
|-----|------|--------------|
| [Custom Ore Veins](https://modrinth.com/mod/customoreveins) | API для крупных жил через datapack | ✅ |
| [Regional Ore Veins](https://www.curseforge.com/minecraft/mc-mods/regional-ore-veins) | Готовый баланс «богатый регион / бедный регион» (уголь vs редстоун, железо vs медь…) | ✅ (datapack внутри мода) |
| [ore-veins](https://modrinth.com/mod/ore-veins) | Альтернатива: гибкие JSON-жилы с фильтром по биомам | ✅ |

**В репозитории:** `cannon-datapacks/regional-resources/` — дополнительные жилы (золото в badlands, серебро Ice and Fire в горах, самоцветы в редких биомах Terralith).

### Земледелие по регионам

| Мод | Роль |
|-----|------|
| [Restrictive Farming](https://www.curseforge.com/minecraft/mc-mods/restrictive-farming) | Культуры только в whitelisted биомах; замедление вне зоны |
| **cannon-economy** (наш) | Правило «плодородная почва у воды на равнинах» + админ-зоны `FERTILE` |

Пшеница/морковь/картофель — **plains + river/meadow**, в 4 блоках от воды (настраивается в `cannon_economy-common.toml`).

### Транспорт и торговые пути

| Мод | Роль |
|-----|------|
| [Trotting Wagons](https://www.curseforge.com/minecraft/mc-mods/trotting-wagons) | Повозки для игроков и NPC |
| [Caravans & Convoys](https://www.curseforge.com/minecraft/mc-mods/caravans-convoys) | **Автоматические** маршруты между «доками» (зависит от Trotting Wagons) |
| [Better Caravans](https://modrinth.com/mod/better-caravans) | Бродячие караваны с биом-зависимым лутом и торговлей (datapack) |
| [Little Logistics](https://www.curseforge.com/minecraft/mc-mods/little-logistics) | Реки и ж/д для массовых грузов (опционально) |
| **cannon-economy** | Торговые посты, тарифы, эмбargo Recruits, админ-маршруты |

HYW transport routes (лимит **50 блоков**) остаются для **склад ↔ армия** внутри claim. Между королевствами — Caravans & Convoys + cannon-economy.

### Ice and Fire

- Dragonsteel / Valyrian steel — **не генерировать везде**: жилы только в admin-deposit или редких биомах (datapack + `/cannoneconomy deposit add`).
- Серебровые руды IaF — привязать к `#cannon:cold_highlands` (Terralith snowy peaks).

---

## Архитектура cannon-economy

```
┌─────────────────┐     embargo/tariff     ┌──────────────────┐
│  Recruits       │◄──────────────────────►│  Trade Routes    │
│  factions       │                        │  (караваны)      │
└─────────────────┘                        └────────┬─────────┘
                                                    │
┌─────────────────┐     bonus yield          ┌──────▼─────────┐
│  Admin deposits │◄── mining in radius ────►│  Trade Posts   │
│  GOLD/IRON/…    │                          │  (склады)      │
└─────────────────┘                          └────────────────┘
         ▲
         │ biome + water rules
┌────────┴────────┐
│  Crop growth    │
└─────────────────┘
```

### Админ-залежи (`/cannoneconomy deposit`)

Типы: `GOLD`, `IRON`, `SILVER`, `GEMS`, `COAL`, `FERTILE`, `DRAGONSTEEL`.

- Админ встаёт в точку карты → `deposit add GOLD 48`
- В радиусе: бонусные дропы при добыче соответствующих руд / камня
- `FERTILE` — пшеница и овощи растут без ограничения «у воды»

### Торговые маршруты (`/cannoneconomy route`)

1. Поставить **Trade Post** (блок из мода) в городе A и B.
2. `route create iron_route --from A --to B --item minecraft:iron_ingot --amount 32 --interval 6000 --tariff 10`
3. Каждые N тиков спавнится караван (лошадь + сундук), везёт груз по прямой с проверкой чанков.
4. При пересечении claim Recruits: если **эмбargo** — караван разворачивается; иначе списывается **тариф** (% от груза) в «post» claim-владельца (упрощённо — часть груза остаётся в промежуточном посте).

### Конфигурация

`config/cannon_economy-common.toml` — радиус залежей, правила урожая, включение интеграции Recruits.

---

## Установка в модпак

1. Добавить в CurseForge: Custom Ore Veins, Regional Ore Veins, Restrictive Farming, Trotting Wagons, Caravans & Convoys, Better Caravans (по желанию), Ice and Fire.
2. Скопировать `cannon-datapacks/regional-resources` в `saves/<мир>/datapacks/` или собрать в отдельный datapack-mod.
3. Установить JAR: `cannon-economy/release/cannon_economy-*.jar`
4. На сервере: OP расставляет залежи золота/железа на карте Westeros/Essos вручную.

---

## Матрица ресурсов (черновик для админов)

| Регион (биом / зона) | Избыток | Дефицит |
|----------------------|---------|---------|
| River + Plains | Пшеница, рыба, глина | Руда |
| Badlands / Mesa | Золото, терракота | Дерево, еда |
| Snowy peaks (Terralith) | Серебро, железо | Зерно |
| Swamp | Торф, слизь, IaF Myrmex | Камень |
| Desert | Соль, стекло, верблюды (Better Caravans) | Дерево, металл |
| Admin: King's Landing | — | Всё — через торговлю |
| Admin: Casterly Rock | `deposit IRON` | — |

---

## Дальнейшие шаги

- [ ] Связать Trade Post с HYW warehouse (автопополнение supply армии)
- [ ] Dynmap/BlueMap слой для залежей и маршрутов
- [ ] Datapack торгов Ice and Fire (dragon bone, scales) в Better Caravans
- [ ] KubeJS для динамических цен от спроса сервера
