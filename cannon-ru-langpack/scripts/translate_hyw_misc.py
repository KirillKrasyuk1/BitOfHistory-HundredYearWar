#!/usr/bin/env python3
"""Quality rule-based translation for HYW army/entity/block/misc keys."""

from __future__ import annotations

import json
import re
from pathlib import Path

EXPORT = Path(__file__).resolve().parents[1] / "scripts/export"

UNITS = {
    "Archer": "Лучник",
    "Crossbowman": "Арбалетчик",
    "Crossbow Man": "Арбалетчик",
    "Warrior": "Воин",
    "Militia": "Ополчение",
    "Shieldman": "Щитоносец",
    "Cavalry": "Кавалерия",
    "Horse": "Конь",
    "Spear Man": "Копейщик",
    "Handgonne Man": "Стрелок с ручной пушкой",
    "Matchlock Man": "Стрелок с мушкетом",
    "Mounted Archer": "Конный лучник",
    "Mounted Lancer": "Конный копейщик",
    "Mounted Light Lancer": "Лёгкий конный копейщик",
    "Mounted Matchlock Man": "Конный стрелок с мушкетом",
    "Light Cavalry": "Лёгкая кавалерия",
    "Skeleton Light Cavalry": "Скелетная лёгкая кавалерия",
    "Skeleton Mounted Archer": "Скелетный конный лучник",
    "Skeleton Horse": "Скелет-конь",
    "Skeleton Siege Engineer": "Скелет-осадный инженер",
    "Swift Zombie": "Быстрый зомби",
    "Siege Engineer": "Осадный инженер",
    "Bandit Siege Engineer": "Разбойник-осадный инженер",
    "Desert Raider Siege Engineer": "Мародер пустыни — осадный инженер",
    "Wood Elf Siege Engineer": "Лесной эльф — осадный инженер",
    "Battering Ram": "Стенобитный таран",
    "Breeder": "Селекционер",
    "Craftsman": "Ремесленник",
    "Farmer": "Фермер",
    "Fisher": "Рыбак",
    "Lumberjack": "Лесоруб",
    "Miner": "Шахтёр",
    "Porter": "Грузчик",
    "Transport Worker": "Грузчик",
    "Priest": "Жрец",
    "Siege Tower": "Осадная башня",
    "Nest of Bees": "Гнездо пчел",
    "Bombard": "Бомбарда",
    "Cannon": "Пушка",
    "Culverin": "Кулеврина",
    "Great Bombard": "Большая бомбарда",
    "Mangonels": "Мангонели",
    "Trebuchets": "Требушеты",
    "Springald": "Спрингалд",
    "Ribauldequin": "Рибодекин",
    "Handgonne": "Ручная пушка",
    "Matchlock": "Мушкет",
}

BLOCKS = {
    "Breeding Workstation": "Станция разведения",
    "Crafting Workstation": "Ремесленная станция",
    "Farming Workstation": "Фермерская станция",
    "Fishing Workstation": "Рыболовная станция",
    "Lumber Workstation": "Лесопильная станция",
    "Mining Workstation": "Шахтёрская станция",
    "Transport Workstation": "Транспортная станция",
    "Warehouse Workstation": "Складская станция",
    "Placeholder Block": "Блок-заглушка",
    "Spawn Point": "Точка спавна",
    "Supply Point": "Точка снабжения",
}


def tr_army(value: str) -> str:
    m = re.fullmatch(r"§e(.+?) (I{1,3}|IV)§r", value)
    if m:
        name, rank = m.groups()
        for en, ru in sorted(UNITS.items(), key=lambda x: -len(x[0])):
            if name == en:
                return f"§e{ru} {rank}§r"
        return f"§e{name} {rank}§r"
    m = re.fullmatch(r"§e(.+?) · Equipment (\d+)§r", value)
    if m:
        name, eq = m.groups()
        unit = UNITS.get(name, name)
        return f"§e{unit} · Снаряжение {eq}§r"
    return value


def tr_simple(value: str, mapping: dict[str, str]) -> str:
    if value in mapping:
        return mapping[value]
    out = value
    for en, ru in sorted(mapping.items(), key=lambda x: -len(x[0])):
        out = re.sub(rf"\b{re.escape(en)}\b", ru, out)
    return out


def translate_entry(key: str, value: str) -> str:
    if key.startswith("army.hundred_years_war."):
        return tr_army(value)
    if key.startswith("block.hundred_years_war."):
        return tr_simple(value, BLOCKS)
    if key.startswith("entity.hundred_years_war."):
        return tr_simple(value, UNITS)
    if key.startswith("skin_pack.hundred_years_war."):
        return value.replace("Skin Pack", "Набор скинов").replace("Default", "Стандартный")
    if key.startswith("unit.hundred_years_war."):
        return tr_simple(value, UNITS)
    if key.startswith("recruitment.hundred_years_war.category."):
        cats = {"Generic": "§eОбщий§r", "Mercenary": "§eНаёмник§r", "Undead": "§eНежить§r"}
        return cats.get(value.replace("§e", "").replace("§r", ""), value.replace("Generic", "Общий").replace("Mercenary", "Наёмник"))
    return value


def main() -> None:
    en = json.loads((EXPORT / "en_entity_army_misc.json").read_text(encoding="utf-8"))
    ru = {k: translate_entry(k, v) for k, v in en.items()}
    (EXPORT / "ru_entity_army_misc.json").write_text(
        json.dumps(ru, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Wrote {len(ru)} keys")


if __name__ == "__main__":
    main()
