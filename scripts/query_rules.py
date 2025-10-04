#!/usr/bin/env python3
"""
Query GuScript rules by keyword on result/tags and print:
  filename - required(recipe) - priority - arity

Usage:
  python3 scripts/query_rules.py <keyword> [rules_dir] [--all]

Notes:
  - Scans: ChestCavityForge/src/main/resources/data/chestcavity/guscript/rules by default.
  - Match when keyword is a substring of result or any tag in tags[].
  - If --all is provided, ignore keyword filter and list all.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RULES_DIR = PROJECT_ROOT / 'ChestCavityForge' / 'src/main/resources/data/chestcavity/guscript/rules'
ALT_RULES_DIR = PROJECT_ROOT / 'src/main/resources/data/chestcavity/guscript/rules'
if ALT_RULES_DIR.is_dir():
    DEFAULT_RULES_DIR = ALT_RULES_DIR


def fmt_required(req) -> str:
    if isinstance(req, dict):
        parts = []
        for k in sorted(req.keys()):
            v = req[k]
            parts.append(f"{k}:{v}")
        return ', '.join(parts)
    return ''


def main(argv) -> int:
    if len(argv) < 2:
        print('Usage: python3 scripts/query_rules.py <keyword> [rules_dir] [--all]')
        return 2
    keyword = str(argv[1]).strip()
    list_all = False
    rules_dir = DEFAULT_RULES_DIR
    for arg in argv[2:]:
        if arg == '--all':
            list_all = True
        else:
            p = Path(arg)
            if p.exists():
                rules_dir = p

    if not rules_dir.is_dir():
        print(f'Rules directory not found: {rules_dir}', file=sys.stderr)
        return 1

    matched = 0
    for path in sorted(rules_dir.rglob('*.json')):
        try:
            data = json.loads(path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            continue
        result = data.get('result')
        tags = data.get('tags') or []
        if not isinstance(tags, list):
            tags = []
        tags_strs = [str(t) for t in tags]

        cond = list_all or (
            (isinstance(result, str) and keyword in result) or any(keyword in t for t in tags_strs)
        )
        if not cond:
            continue

        required = fmt_required(data.get('required'))
        priority = data.get('priority')
        arity = data.get('arity')
        filename = path.name
        print(f'{filename} - {required} - {priority} - {arity}')
        matched += 1

    if matched == 0:
        print('No matches found.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main(sys.argv))

