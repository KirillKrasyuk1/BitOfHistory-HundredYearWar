#!/usr/bin/env python3
"""Fix broken HYW ru entries (placeholder corruption, bad machine translations)."""

from __future__ import annotations

import json
import re
import time
import zipfile
from pathlib import Path

from deep_translator import MyMemoryTranslator

ROOT = Path(__file__).resolve().parents[1]
NEW_JAR = ROOT / "libs/HundredYearsWar-0.7.1r-fix1-1.20.1-forge.jar"
RU_PATH = ROOT / "src/main/resources/assets/hundred_years_war/lang/ru_ru.json"

PH_RE = re.compile(r"(§.|%[0-9]*\$?[sdif]|\\n|\n)")

MANUAL: dict[str, str] = {
    "gui.hundred_years_war.scroll_summon_config.owner_team": "Отряд: %s",
    "recruitment.hundred_years_war.category.generic": "§eОбщий§r",
}


def load_en() -> dict[str, str]:
    with zipfile.ZipFile(NEW_JAR) as zf:
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


def translate(text: str, translator: MyMemoryTranslator, cache: dict[str, str]) -> str:
    if text in cache:
        return cache[text]
    protected, tokens = protect(text)
    try:
        out = translator.translate(protected)
        time.sleep(0.25)
    except Exception:
        out = protected
    out = restore(out, tokens)
    cache[text] = out
    return out


def needs_fix(value: str) -> bool:
    return (
        "\ue000" in value
        or "⟦" in value
        or "⟧" in value
        or "Team:" in value
        or value.endswith(" %1")
    )


def main() -> None:
    en = load_en()
    ru = json.loads(RU_PATH.read_text(encoding="utf-8"))
    translator = MyMemoryTranslator(source="en-US", target="ru-RU")
    cache: dict[str, str] = {}
    fixed = 0

    for key, manual in MANUAL.items():
        if key in en:
            ru[key] = manual
            fixed += 1

    for key, value in list(ru.items()):
        if key in MANUAL:
            continue
        if not needs_fix(value):
            continue
        if key not in en:
            continue
        ru[key] = translate(en[key], translator, cache)
        fixed += 1
        if fixed % 50 == 0:
            print(f"fixed {fixed}", flush=True)

    RU_PATH.write_text(json.dumps(ru, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    remaining = sum(1 for v in ru.values() if needs_fix(v))
    print(f"Updated {fixed} keys; remaining issues: {remaining}")


if __name__ == "__main__":
    main()
