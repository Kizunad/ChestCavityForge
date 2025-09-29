#!/usr/bin/env python3
"""Utility: dump the unique set of tags used by all GuScript leaf JSON files."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEAVES_DIR = ROOT / "src/main/resources/data/chestcavity/guscript/leaves"


def collect_unique_tags() -> set[str]:
    unique: set[str] = set()
    for path in sorted(LEAVES_DIR.rglob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            sys.stderr.write(f"Failed to parse {path}: {exc}\n")
            continue
        tags = data.get("tags", [])
        if not isinstance(tags, list):
            sys.stderr.write(f"Ignoring non-list tags in {path}\n")
            continue
        unique.update(str(tag) for tag in tags)
    return unique


def main() -> int:
    if not LEAVES_DIR.is_dir():
        sys.stderr.write(f"Leaf directory not found: {LEAVES_DIR}\n")
        return 1
    tags = sorted(collect_unique_tags())
    for tag in tags:
        print(tag,end=', ')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
