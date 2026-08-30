#!/usr/bin/env python3
"""Merge quality HYW translation chunks into ru_ru.json."""

from __future__ import annotations

import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPORT = ROOT / "scripts/export"
OUT = ROOT / "src/main/resources/assets/hundred_years_war/lang/ru_ru.json"
HYW_JAR = ROOT / "libs/HundredYearsWar-0.7.1r-fix1-1.20.1-forge.jar"
OLD_JAR = ROOT / "libs/HundredYearsWar-0.3.7b-1.20.1-forge.jar"
MANUAL_BASE = Path("/tmp/hyw_ru_manual_717.json")

CHUNK_FILES = [
    "ru_gui_1.json",
    "ru_gui_2.json",
    "ru_item_1.json",
    "ru_item_2.json",
    "ru_ui_1.json",
    "ru_ui_2.json",
    "ru_ui_3.json",
    "ru_entity_army_misc.json",
]


def load_en(jar: Path) -> dict[str, str]:
    with zipfile.ZipFile(jar) as zf:
        return json.loads(zf.read("assets/hundred_years_war/lang/en_us.json"))


def patch_changed(old_en: str, old_ru: str, new_en: str) -> str | None:
    import re

    if old_en == new_en:
        return old_ru
    m_old = __import__("re").fullmatch(r"Scroll of (.+?) (I{1,3}|IV)", old_en)
    m_new = __import__("re").fullmatch(r"Scroll of (.+?) \(Rank (I{1,3}|IV)\)", new_en)
    if m_old and m_new and m_old.groups() == m_new.groups():
        unit, rank = m_old.groups()
        scroll_ru = old_ru.split(":")[0] if ":" in old_ru else f"Свиток: {unit}"
        unit_part = scroll_ru.replace("Свиток:", "").strip()
        return f"Свиток:{unit_part} ({rank})"
    if new_en.startswith("§aRace:") and "§eLight§r ranged unit" in new_en:
        race_line = "§aРаса: §eНаёмник§r"
        body = old_ru
        if not body.startswith("§aРаса:"):
            body = body.replace("§eLight§r", "§eЛёгкий§r").replace("§elight§r", "§eлёгкие§r")
            body = body.replace("§acounters§r", "§aэффективен против§r")
            body = body.replace("ranged unit", "стрелок")
        if "\n" not in body and race_line not in body:
            body = race_line + "\n" + body
        return body
    return None


def main() -> int:
    if not MANUAL_BASE.exists():
        print("Missing manual base:", MANUAL_BASE, file=__import__("sys").stderr)
        return 1

    en071 = load_en(HYW_JAR)
    en037 = load_en(OLD_JAR)
    manual = json.loads(MANUAL_BASE.read_text(encoding="utf-8"))

    merged: dict[str, str] = {}
    for key, value in en071.items():
        if key in en037 and en037[key] == value and key in manual:
            merged[key] = manual[key]
        elif key in en037 and en037[key] != value and key in manual:
            patched = patch_changed(en037[key], manual[key], value)
            if patched:
                merged[key] = patched

    missing_chunks: list[str] = []
    for name in CHUNK_FILES:
        path = EXPORT / name
        if not path.exists():
            missing_chunks.append(name)
            continue
        chunk = json.loads(path.read_text(encoding="utf-8"))
        merged.update(chunk)

    missing_keys = sorted(set(en071) - set(merged))
    if missing_keys:
        print(f"WARNING: {len(missing_keys)} keys still untranslated")
        for k in missing_keys[:20]:
            print(" ", k)
        if missing_chunks:
            print("Missing chunk files:", ", ".join(missing_chunks))
        return 1

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(merged)} keys to {OUT}")
    assert set(merged) == set(en071)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
