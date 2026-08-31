# Cannon Economy 2.0 — гайд для сервера Cannon

Три задачи мода:
1. **Плодородность почвы (1–5)** — скорость роста культур
2. **Залежи руд** — админские месторождения с конвертацией и регенерацией
3. **HYW баланс** — незерит заменён на алмазы, повышенный расход провизии армии

## Установка

1. JAR: `release/cannon_economy-2.0.0.jar` → `mods/` (клиент + сервер)
2. Нужен **Hundred Years War** (для переопределения найма и экипировки)
3. **Не нужны:** Custom Ore Veins, Regional Ore Veins, Restrictive Farming, cannon-datapacks
4. Запусти мир → появится `config/cannon_economy-common.toml`

---

## 1. Плодородность

| Уровень | Где | Скорость роста |
|---------|-----|----------------|
| **1** | Горы, тайга, лес, снег | ×0.5 (в 2 раза медленнее). **Посадка запрещена** |
| **2** | То же + умеренные леса | ×0.67 (в 1.5 раза медленнее) |
| **3** | Прочие биомы | ×1.0 (ванilla) |
| **4–5** | Равнины, реки, луга (случайно в ячейках 8×8) | ×1.5 – ×2.0 |

### Как проверить
- Мотыгой ПКМ по земле/грядке — подсказка «Плодородность: N/5»
- Или: `/cannoneconomy fertility` (стоя на блоке)

### Конфиг
```toml
[fertility]
enableFertility = true
cellSize = 8          # размер ячейки случайного 3–5 на равнинах
showFertilityOnHoe = true
```

### Провизия армии (HYW)
```toml
[hyw]
supplyConsumptionMultiplier = 2.0   # армия ест в 2 раза больше
diamondsPerNetherite = 3            # уже учтено в JSON найма
```

---

## 2. Залежи руд (регенерирующие)

Админ встаёт в центр месторождения:

```
/cannoneconomy deposit create iron 5 Casterly Rock
/cannoneconomy deposit create gold 3
/cannoneconomy deposit create minecraft:diamond_ore 5
/cannoneconomy deposit create silver 5    # Ice and Fire, если мод есть
/cannoneconomy deposit list
/cannoneconomy deposit remove <uuid>
```

### Пресеты
`iron`, `gold`, `coal`, `copper`, `diamond`, `emerald`, `lapis`, `redstone`, `silver`, `sapphire`

Или полный id: `minecraft:iron_ore`, `iceandfire:silver_ore`

### Что происходит
1. **Конвертация** — по чанкам в радиусе: существующие руды заменяются на целевую, ~12% камня тоже → жилы
2. **Добыча** — игроки копают как обычно
3. **Регенерация** — каждые 10 сек (настраивается) в случайных точках зоны камень → руда. **Недавно вскопанные блоки** (10 мин) не регенерируют — руда появляется в другом месте

### Конфиг
```toml
[deposits]
defaultChunkRadius = 5
oreMinY = -64
oreMaxY = 64
regenIntervalTicks = 200      # 10 сек
oresPerRegen = 4              # блоков руды за тик регенa
minedCooldownTicks = 12000    # 10 мин «чёрный список» после добычи
convertBlocksPerTick = 8000
```

> Работает в **уже сгенерированных** чанках — не нужен новый мир.

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
# 1. Месторождения
/cannoneconomy deposit create iron 5 Casterly Rock
/cannoneconomy deposit create gold 5 Lannisport

# 2. Проверка плодородия у реки на равнинах
/cannoneconomy fertility

# 3. Конфиг: supplyConsumptionMultiplier = 2.0 (или выше)
```

Игрокам: «Еда только у рек и на равнинах, железо — только с нашей шахты, армию кормить вдвое сложнее».
