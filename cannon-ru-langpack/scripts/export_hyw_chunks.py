#!/usr/bin/env python3
"""Export HYW 0.7.1 keys that need translation into scripts/export/."""

from __future__ import annotations

import json
import zipfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPORT = ROOT / "scripts/export"
HYW_JAR = ROOT / "libs/HundredYearsWar-0.7.1r-fix1-1.20.1-forge.jar"
OLD_JAR = ROOT / "libs/HundredYearsWar-0.3.7b-1.20.1-forge.jar"
MANUAL = Path("/tmp/hyw_ru_manual_717.json")


def load_en(jar: Path) -> dict[str, str]:
    with zipfile.ZipFile(jar) as zf:
        return json.loads(zf.read("assets/hundred_years_war/lang/en_us.json"))


def main() -> None:
    if not MANUAL.exists():
        raise SystemExit(f"Run: git show e7702f1:.../ru_ru.json > {MANUAL}")

    en071 = load_en(HYW_JAR)
    en037 = load_en(OLD_JAR)
    manual = json.loads(MANUAL.read_text(encoding="utf-8"))
    need = {
        k: v
        for k, v in en071.items()
        if not (k in en037 and en037[k] == v and k in manual)
    }

    EXPORT.mkdir(parents=True, exist_ok=True)
    groups: dict[str, dict[str, str]] = defaultdict(dict)

    def bucket(key: str) -> str:
        if key.startswith("gui.hundred_years_war"):
            return "gui"
        if key.startswith("item.hundred_years_war"):
            return "item"
        if key.startswith(
            (
                "ui.hundred_years_war",
                "message.hundred_years_war",
                "command.hundred_years_war",
                "msg.hyw",
                "key.hyw",
                "hud.hundred_years_war",
                "wheel.hundred_years_war",
                "recruitment.hundred_years_war",
                "action.hundred_years_war",
                "tooltip.hundred_years_war",
            )
        ):
            return "ui_msg_cmd"
        return "entity_army_misc"

    for k, v in need.items():
        groups[bucket(k)][k] = v

    for name, data in groups.items():
        path = EXPORT / f"en_{name}.json"
        path.write_text(json.dumps(dict(sorted(data.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"{name}: {len(data)}")

    print(f"total need: {len(need)}")


if __name__ == "__main__":
    main()
