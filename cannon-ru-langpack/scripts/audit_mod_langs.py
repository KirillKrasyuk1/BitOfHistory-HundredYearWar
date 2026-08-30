#!/usr/bin/env python3
"""Scan a Minecraft mods folder for missing ru_ru language files."""

from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path


def lang_files(jar: zipfile.ZipFile) -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for name in jar.namelist():
        if "/lang/" not in name or not name.endswith(".json"):
            continue
        parts = name.split("/")
        try:
            idx = parts.index("lang")
            modid = parts[idx - 1]
        except (ValueError, IndexError):
            continue
        locale = parts[idx + 1].replace(".json", "")
        result.setdefault(modid, []).append(locale)
    return result


def _ru_is_real(jar: zipfile.ZipFile, modid: str) -> bool:
    path = f"assets/{modid}/lang/ru_ru.json"
    try:
        data = json.loads(jar.read(path))
    except (KeyError, json.JSONDecodeError):
        return False
    return bool(data)


def main() -> int:
    mods_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "mods")
    if not mods_dir.is_dir():
        print(f"Not a directory: {mods_dir}", file=sys.stderr)
        return 1

    has_ru: list[str] = []
    missing_ru: list[str] = []
    no_lang: list[str] = []

    for jar_path in sorted(mods_dir.glob("*.jar")):
        try:
            with zipfile.ZipFile(jar_path) as zf:
                langs = lang_files(zf)
                if not langs:
                    no_lang.append(jar_path.name)
                    continue
                modids = sorted(langs)
                ru_ok = all(
                    "ru_ru" in langs[m] and _ru_is_real(zf, m) for m in modids
                )
        except zipfile.BadZipFile:
            continue
        entry = f"{jar_path.name}  ({', '.join(modids)})"
        if ru_ok:
            has_ru.append(entry)
        else:
            missing_ru.append(entry + "  → " + ", ".join(
                f"{m}:[{','.join(sorted(langs[m]))}]" for m in modids
            ))

    print(f"=== Mods WITH ru_ru ({len(has_ru)}) ===")
    for line in has_ru:
        print(line)
    print(f"\n=== Mods MISSING ru_ru ({len(missing_ru)}) ===")
    for line in missing_ru:
        print(line)
    print(f"\n=== No lang files ({len(no_lang)}) ===")
    for line in no_lang[:30]:
        print(line)
    if len(no_lang) > 30:
        print(f"... and {len(no_lang) - 30} more")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
