# Cannon Russian Langpack

Русификатор для модпака **Cannon** — дополняет моды, у которых нет полноценного `ru_ru.json`.

## Что переведено (v1.1.1)

| Мод | Статус |
|-----|--------|
| **Hundred Years War 0.7.1** | Полный перевод (2340 строк) |
| **Villager Recruits** | 32 новых ключа (эмбарго, осадный инженер, приоритеты целей) |
| **Siege Weapons** | Полный перевод (23 строки; в JAR был пустой ru_ru) |
| **Villager Workers** | 4 новых ключа (курьер, сохранение зоны добычи) |
| Остальные моды | См. аудит — многие уже имеют ru на CurseForge |

## Установка

1. Скачай `cannon_ru_langpack-1.1.1.jar` из `release/`
2. Положи в `Cannon ______\mods\` рядом с остальными модами
3. В Minecraft: **Настройки → Язык → Русский**
4. Перезапусти игру

Прямая ссылка (ветка PR):

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-ru-langpack-5fac/cannon-ru-langpack/release/cannon_ru_langpack-1.1.1.jar

## Аудит модов без русификатора

На своём ПК (папка модов Cannon):

```bash
python3 cannon-ru-langpack/scripts/audit_mod_langs.py "C:/Users/dkras/curseforge/minecraft/Instances/Cannon ______/mods"
```

Скрипт покажет, у каких JAR есть `ru_ru`, а у каких нет (включая пустые файлы-заглушки).

## Сборка

```bash
cd cannon-ru-langpack
./gradlew build
```

## Обновление перевода HYW

При выходе новой версии мода:

```bash
# Положи новый JAR в libs/ как HundredYearsWar-0.7.1r-fix1-1.20.1-forge.jar
python3 scripts/build_hyw_ru.py
python3 scripts/fix_hyw_ru.py
```

## Добавление переводов других модов

1. Распакуй `en_us.json` из JAR: `assets/<modid>/lang/en_us.json`
2. Переведи значения → `src/main/resources/assets/<modid>/lang/ru_ru.json`
3. Добавь зависимость в `META-INF/mods.toml` (`ordering="AFTER"`)
4. Собери и проверь в игре

## Глоссарий (Cannon)

| EN | RU |
|----|-----|
| faction | фракция |
| claim | клейм / территория |
| siege | осада |
| team | отряд |
| recruit | рекрут (Recruits NPC; HYW — «солдат/юнит») |
