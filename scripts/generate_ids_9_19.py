#!/usr/bin/env python3
import json
from pathlib import Path

INPUT_PATH = Path('Code/java/decompile/9_16_decompile/assets/guzhenren/lang/en_us.json')
OUTPUT_PATH = Path('docs/ids/9_19.json')

# Filters
EXCLUDE_PREFIXES = (
    'gui',
    'advancements',
    'subtitles',
)
EXCLUDE_SUFFIXES = (
    'spawn_egg',
)

# Group categories (root keys) and their match prefixes
CATEGORIES = (
    'effect',
    'item',
    'entity',
)


def should_exclude(key: str) -> bool:
    for p in EXCLUDE_PREFIXES:
        if key.startswith(p):
            return True
    for s in EXCLUDE_SUFFIXES:
        if key.endswith(s):
            return True
    return False


def detect_category(key: str):
    for cat in CATEGORIES:
        if key.startswith(cat):
            return cat
    return None


def main():
    if not INPUT_PATH.exists():
        raise SystemExit(f"Input file not found: {INPUT_PATH}")

    with INPUT_PATH.open('r', encoding='utf-8') as f:
        data = json.load(f)

    grouped = {cat: {} for cat in CATEGORIES}

    for k, v in data.items():
        if should_exclude(k):
            continue
        cat = detect_category(k)
        if cat is None:
            continue  # skip keys outside requested categories
        grouped[cat][k] = v

    # Ensure output directory exists
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    with OUTPUT_PATH.open('w', encoding='utf-8') as f:
        json.dump(grouped, f, ensure_ascii=False, indent=2, sort_keys=True)

    # Simple summary
    counts = {k: len(v) for k, v in grouped.items()}
    print(json.dumps({"written": str(OUTPUT_PATH), "counts": counts}, ensure_ascii=False))


if __name__ == '__main__':
    main()

