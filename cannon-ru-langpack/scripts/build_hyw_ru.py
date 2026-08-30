#!/usr/bin/env python3
"""Build HYW ru_ru.json from en_us, preserving existing translations."""

from __future__ import annotations

import json
import re
import time
import zipfile
from pathlib import Path

from deep_translator import MyMemoryTranslator

ROOT = Path(__file__).resolve().parents[1]
OLD_JAR = ROOT / "libs/HundredYearsWar-0.3.7b-1.20.1-forge.jar"
NEW_JAR = ROOT / "libs/HundredYearsWar-0.7.1r-fix1-1.20.1-forge.jar"
OUT = ROOT / "src/main/resources/assets/hundred_years_war/lang/ru_ru.json"

PH_RE = re.compile(
    r"(§.|%[0-9]*\$?[sdif]|\\n|\n|@Tooltip(?:\[\d+])?)"
)

GLOSSARY = {
    "Team": "Отряд",
    "team": "отряд",
    "Squad": "Отряд",
    "squad": "отряд",
    "Faction": "Фракция",
    "faction": "фракция",
    "Claim": "Клейм",
    "claim": "клейм",
    "Siege": "Осада",
    "siege": "осада",
    "Mercenary": "Наёмник",
    "Archer": "Лучник",
    "Crossbowman": "Арбалетчик",
    "Warrior": "Воин",
    "Militia": "Ополчение",
    "Cavalry": "Кавалерия",
    "Shieldman": "Щитоносец",
    "Bombard": "Бомбарда",
    "Cannon": "Пушка",
    "Culverin": "Кулеврина",
    "Trebuchet": "Требушет",
    "Trebuchets": "Требушеты",
    "Mangonel": "Мангонел",
    "Mangonels": "Мангонели",
    "Battering Ram": "Таран",
    "Handgonne": "Ручная пушка",
    "Matchlock": "Мушкет",
    "Freecam": "Свободная камера",
    "freecam": "свободная камера",
}


def load_en(jar: Path) -> dict[str, str]:
    with zipfile.ZipFile(jar) as zf:
        return json.loads(zf.read("assets/hundred_years_war/lang/en_us.json"))


def protect(text: str) -> tuple[str, list[str]]:
    tokens: list[str] = []

    def repl(match: re.Match[str]) -> str:
        tokens.append(match.group(0))
        return f"\ue000{len(tokens) - 1}\ue001"

    return PH_RE.sub(repl, text), tokens


def restore(text: str, tokens: list[str]) -> str:
    for i, token in enumerate(tokens):
        text = text.replace(f"\ue000{i}\ue001", token)
        text = text.replace(f"⟦{i}⟧", token)
    return text


def apply_glossary(text: str) -> str:
    for en, ru in sorted(GLOSSARY.items(), key=lambda x: -len(x[0])):
        text = re.sub(rf"\b{re.escape(en)}\b", ru, text)
    return text


def translate_value(text: str, translator: MyMemoryTranslator, cache: dict[str, str]) -> str:
    if text in cache:
        return cache[text]
    protected, tokens = protect(text)
    if not protected.strip():
        cache[text] = text
        return text
    try:
        translated = translator.translate(protected)
        time.sleep(0.25)
    except Exception:
        translated = protected
    translated = restore(translated, tokens)
    translated = apply_glossary(translated)
    cache[text] = translated
    return translated


def patch_changed(old_en: str, old_ru: str, new_en: str) -> str | None:
    """Reuse old translation when only minor English edits happened."""
    if old_en == new_en:
        return old_ru
    # Rank suffix rename: Scroll of Archer I -> Scroll of Archer (Rank I)
    m_old = re.fullmatch(r"Scroll of (.+?) (I{1,3}|IV)", old_en)
    m_new = re.fullmatch(r"Scroll of (.+?) \(Rank (I{1,3}|IV)\)", new_en)
    if m_old and m_new and m_old.groups() == m_new.groups():
        unit, rank = m_old.groups()
        unit_ru = GLOSSARY.get(unit, unit)
        return f"Свиток: {unit_ru} ({rank})" if rank else f"Свиток: {unit_ru}"
    if new_en.startswith("§aRace:") and "§eLight§r ranged unit" in new_en:
        base = old_ru
        if not base.startswith("§aРаса:"):
            base = re.sub(r"^§eLight§r", "§aРаса: §eНаёмник§r\n§eLight§r", old_ru, count=1)
            base = base.replace("§eLight§r", "§eЛёгкий§r", 1)
            base = base.replace("§elight§r", "§eлёгкие§r")
            base = base.replace("§acounters§r", "§aэффективен против§r")
            base = base.replace("ranged unit", "стрелок")
        return apply_glossary(base.replace("Race:", "Раса:").replace("Mercenary", "Наёмник"))
    return None


def main() -> None:
    old_en = load_en(OLD_JAR)
    new_en = load_en(NEW_JAR)
    existing_ru = json.loads(OUT.read_text(encoding="utf-8")) if OUT.exists() else {}

    translator = MyMemoryTranslator(source="en-US", target="ru-RU")
    cache: dict[str, str] = {}
    result: dict[str, str] = {}

    for key in sorted(new_en):
        en_val = new_en[key]
        if key in existing_ru and key in old_en and old_en[key] == en_val:
            result[key] = existing_ru[key]
            continue
        if key in existing_ru and key in old_en and old_en[key] != en_val:
            patched = patch_changed(old_en[key], existing_ru[key], en_val)
            if patched:
                result[key] = patched
                continue
        result[key] = translate_value(en_val, translator, cache)
        if len(result) % 100 == 0:
            print(f"translated {len(result)}/{len(new_en)}", flush=True)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(result)} keys to {OUT}")


if __name__ == "__main__":
    main()
