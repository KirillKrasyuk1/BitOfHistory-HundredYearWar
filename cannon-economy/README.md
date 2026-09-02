# Cannon Economy 2.0 — гайд для сервера Cannon

Три задачи мода:
1. **Плодородность почвы (1–5)** — скорость роста культур
2. **Залежи руд** — админские месторождения с конвертацией и регенерацией
3. **HYW баланс** — незерит заменён на алмазы, повышенный расход провизии армии

## Установка

1. JAR: `release/cannon_economy-2.0.2.jar` → `mods/` (клиент + сервер)
2. Нужен **Hundred Years War** (для переопределения найма и экипировки)
3. **Не нужны:** Custom Ore Veins, Regional Ore Veins, Restrictive Farming, cannon-datapacks
4. Запусти мир → появится `config/cannon_economy-common.toml`

---

## 1. Плодородность

| Уровень | Где | Скорость роста |
|---------|-----|----------------|
| **1** | Горы, тайга, лес, снег | ×0.5 (в 2 раза медленнее). **Посадка запрещена** |
| **2** | То же + умеренные леса, **саванна/пустыня без воды** | ×0.67 (в 1.5 раза медленнее) |
| **3** | Прочие биомы с орошением | ×1.0 (ванilla) |
| **4–5** | Равнины, луга с водой (случайно в ячейках 8×8) | ×1.5 – ×2.0 |
| **5 (Нил)** | **У реки / вода + речной биом** (в т.ч. саванна/пустыня у реки) | **×2.0 строго** |

### Орошение
- Посадка только **в 4 блоках от воды** (источник, не текущая вода под грядкой)
- **[Let's Do] Farm & Charm:** дождеватель (`water_sprinkler`) считается орошением (радиус 8); удобрённая почва даёт +1 к плодородности
- Пугало и дождь F&amp;C работают как в оригинале — наш мод только замедляет/ускоряет базовый рост по биому

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
3. **Регенерация** — только на **зафиксированных** позициях жилы, через `regenSeconds` после добычи.

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
