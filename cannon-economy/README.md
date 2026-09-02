# Cannon Economy 2.0 — гайд для сервера Cannon

Три задачи мода:
1. **Плодородность почвы (1–5)** — скорость роста культур
2. **Залежи руд** — админские месторождения с конвертацией и регенерацией
3. **HYW баланс** — незерит заменён на алмазы, повышенный расход провизии армии
4. **Блокировка структур** — Recruits/Village Recruits/HYW не спавнят военные базы в мире

## Установка

1. JAR: `release/cannon_economy-2.0.4.jar` → `mods/` (клиент + сервер)
2. Нужен **Hundred Years War** (для переопределения найма и экипировки)
3. **Не нужны:** Custom Ore Veins, Regional Ore Veins, Restrictive Farming, cannon-datapacks
4. Запусти мир → появится `config/cannon_economy-common.toml`

---

## 1. Плодородность

| Уровень | Где | Скорость роста |
|---------|-----|----------------|
| **1** | Пустыня, горы, тайга, саванна | ×0.25 — без орошения не растёт |
| **2** | То же регионы | ×0.75 |
| **3** | Прочие биомы | ×1.0 |
| **4** | Равнины у природных водоёмов | ×1.25 |
| **5** | **Все реки** (в UI всегда 5/5) | ×1.5; у реки в пустыне/саванне — ×2 от базы биома |

### Орошение
- **Постановка блоков не ограничена** — растения просто не растут на сухой земле
- **Природная вода** (озёра, реки при генерации) и дождеватель = орошение
- **Вода из ведра игрока не даёт бафф** плодородности
- Farm & Charm: дождеватель и удобрённая почва работают как орошение

### Как проверить
- Мотыгой ПКМ по земле/грядке — подсказка «Плодородность: N/5»
- Или: `/cannoneconomy fertility` (стоя на блоке)

### Конфиг
```toml
[fertility]
enableFertility = true
requireWaterForCrops = true
waterRadius = 4
riverFloodplainRadius = 8
riverFertility = 5
cellSize = 8
showFertilityOnHoe = true
farmCharmSprinklerCountsAsWater = true
farmCharmSprinklerRadius = 8
farmCharmFertilizedBonus = true
fertilizedSoilFertilityBonus = 1
```

### Провизия армии (HYW)
```toml
[hyw]
supplyConsumptionMultiplier = 2.0   # армия ест в 2 раза больше
diamondsPerNetherite = 3            # уже учтено в JSON найма
```

---

## 2. Залежи руд (регенерирующие жилы)

Админ встаёт в **центр жилы** (уровень Y = середина по глубине):

```
/cannoneconomy deposit create gold 32 3 16 600
/cannoneconomy deposit create gold 32 3 16 600 Casterly Rock
/cannoneconomy deposit create iron          # значения по умолчанию из конфига
/cannoneconomy deposit list
/cannoneconomy deposit remove <uuid>
```

### 4 параметра жилы

| Параметр | Пример | Описание |
|----------|--------|----------|
| **radius** | `32` | Радиус жилы в **блоках** (горизонтально) |
| **percent** | `3` | Процент камня, заменяемого на руду при создании |
| **depth** | `16` | Глубина жилы по Y (толщина, центр = ваша позиция) |
| **regenSeconds** | `600` | Через сколько секунд вскопанная руда вернётся **на том же месте** |

### Пресеты
`iron`, `gold`, `coal`, `copper`, `diamond`, `emerald`, `lapis`, `redstone`, `silver`, `sapphire`

Или полный id: `minecraft:iron_ore`, `iceandfire:silver_ore`

### Что происходит
1. **Разметка жилы** — сканируется цилиндр (radius × depth). Выбранные блоки **запоминаются навсегда** — золото всегда в одних и тех же координатах.
2. **Добыча** — игроки копают как обычно.
3. **Регенерация** — только **вскопанный блок руды** восстанавливается через `regenSeconds` на том же месте

### Конфиг (значения по умолчанию для команды без параметров)
```toml
[deposits]
defaultBlockRadius = 24
defaultReplacePercent = 3
defaultDepth = 12
defaultRegenSeconds = 300
oresPerRegen = 2
convertBlocksPerTick = 8000
```

> Работает в **уже сгенерированных** чанках — не нужен новый мир. Старые залежи (chunk-radius) подхватятся с миграцией, но лучше пересоздать.

---

## 3. Незерит → алмазы (HYW)

Мод подменяет JSON Hundred Years War:

- **Найм:** `netherite_ingot` → +3 алмаза за каждый бывший незерит
- **Экипировка элитных юнитов:** полный **алмазный** сет вместо незеритового
- **Оружие HYW/Epic Knights:** `netherite_*` → `diamond_*`

Ад и Энд закрыты — элитный tier доступен через алмазы.

---

## 4. Блокировка структур Recruits / HYW

Мод автоматически отключает «военную атмосферу» от аддонов:

| Источник | Что блокируется |
|----------|-----------------|
| **Village Recruits** | Башни-спавнеры вместо деревень, небесные деревни (datapack + config) |
| **Recruits** | Патрули, noble villager, pillager spawn |
| **Recruits Warium** | Патрули наёмников |
| **HYW** | Автогенерация ближайших структур (лагеря, форпосты) |

Вместо башен Village Recruits генерируются **обычные ванильные деревни**.

> Уже сгенерированные структуры в старых чанках **не удаляются** — только новые чанки и новые патрули.

### Конфиг
```toml
[worldgen]
blockRecruitsStructures = true
blockHywNearbyStructures = true
```

---

## Быстрый старт админа

```
# 1. Месторождения (радиус в блоках, % , глубина, реген в секундах)
/cannoneconomy deposit create iron 32 3 16 600 Casterly Rock
/cannoneconomy deposit create gold 24 2 12 900 Lannisport

# 2. Проверка плодородия у реки на равнинах
/cannoneconomy fertility

# 3. Конфиг: supplyConsumptionMultiplier = 2.0 (или выше)
```

Игрокам: «Еда только у рек и на равнинах, железо — только с нашей шахты, армию кормить вдвое сложнее».
