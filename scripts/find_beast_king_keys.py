#!/usr/bin/env python3
"""Extract keys whose values mention 蛊材_*兽王* or 蛊材_*兽皇* in the lang file."""

from __future__ import annotations

import json
import re
from pathlib import Path


def main() -> None:
    lang_path = Path("/home/kiz/Code/java/decompile/9_9decompile/cfr/temp/en_us.json")

    with lang_path.open(encoding="utf-8") as handle:
        entries = json.load(handle)

    pattern = re.compile(r"蛊材_.*兽[王皇].*")
    matches = [key for key, value in entries.items() if isinstance(value, str) and pattern.search(value)]

    if not matches:
        print("No matches found.")
        return


    buckets = {
        "eye": [],
        "skin": [],
        "muscle": [],
        "bone": [],
    }

    suffix_map = {
        "yan": "eye",
        "pi": "skin",
        "rou": "muscle",
        "gu": "bone",
    }

    for key in matches:
        tail = key.rsplit('.', 1)[-1]
        for suffix, bucket in suffix_map.items():
            if tail.endswith(suffix):
                buckets[bucket].append(key)
                break

    project_root = Path(__file__).resolve().parent.parent
    data_root = project_root / "src" / "main" / "resources" / "data" / "chestcavity" / "organs" / "guzhenren" / "animal"
    templates = {
        "muscle": [
            {"id": "chestcavity:strength", "value": "0.2"},
            {"id": "chestcavity:speed", "value": "0.2"},
        ],
        "bone": [
            {"id": "chestcavity:defense", "value": ".05"},
            {"id": "chestcavity:nerves", "value": "0.01"},
        ],
        "eye": [
            {"id": "chestcavity:luck", "value": "0.01"},
        ],
        "skin": [
            {"id": "chestcavity:defense", "value": "0.1"},
        ],
    }

    for bucket, keys in buckets.items():
        print(f"{bucket}:")
        bucket_dir = data_root / bucket
        bucket_dir.mkdir(parents=True, exist_ok=True)
        for key in sorted(keys):
            print(f"  {key}")
            parts = key.split('.')
            if len(parts) < 3:
                continue
            namespace = parts[1]
            name = parts[2]
            item_id = f"{namespace}:{name}"
            content = {
                "itemID": item_id,
                "organScores": templates[bucket],
            }
            file_name = f"{key}_{bucket}.json"
            target_path = bucket_dir / file_name
            with target_path.open('w', encoding='utf-8') as output:
                json.dump(content, output, ensure_ascii=False, indent=2)
                output.write('\n')


if __name__ == "__main__":
    main()
