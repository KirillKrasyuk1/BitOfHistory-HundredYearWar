# Cannon Russian Langpack

Русификатор для модпака **Cannon** — качественный перевод ключевых модов связки HYW + Recruits.

## Что переведено (v1.2.0)

| Мод | Статус |
|-----|--------|
| **Hundred Years War 0.7.1** | Полный перевод **2340** строк (ручная редактура + глоссарий) |
| **Villager Recruits** | 32 новых ключа (эмбарго, осадный инженер, приоритеты) |
| **Siege Weapons** | 23 строки (полный перевод) |
| **Villager Workers** | 4 новых ключа |

> **village_recruits** (TC-сборка Cannon) — отдельный JAR, нужен файл мода для перевода.  
> Остальные моды в паке часто уже содержат `ru_ru` — проверяй аудитом.

## Установка

1. Скачай `cannon_ru_langpack-1.2.0.jar` из `release/`
2. Положи в `Cannon ______\mods\`
3. Язык: **Русский** → перезапуск

https://raw.githubusercontent.com/KirillKrasyuk1/BitOfHistory-HundredYearWar/cursor/cannon-ru-langpack-5fac/cannon-ru-langpack/release/cannon_ru_langpack-1.2.0.jar

Удали старые `cannon_ru_langpack-1.0.x` / `1.1.x`.

## Аудит

```bash
python3 cannon-ru-langpack/scripts/audit_mod_langs.py "C:/Users/dkras/curseforge/minecraft/Instances/Cannon ______/mods"
```

## Обновление перевода HYW

```bash
# 1. Экспорт ключей для доработки
python3 scripts/export_hyw_chunks.py   # или вручную из en_us JAR
# 2. Редактируй scripts/export/ru_*.json
# 3. Сборка
python3 scripts/merge_hyw_ru.py
./gradlew build
```

## Глоссарий

| EN | RU |
|----|-----|
| faction | фракция |
| claim | клейм |
| siege | осада |
| team | отряд |
| formation | построение |
| RTS mode | режим RTS |
| Mercenary | Наёмник |
