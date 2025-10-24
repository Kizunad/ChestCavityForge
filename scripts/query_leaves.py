#!/usr/bin/env python3
"""
Query GuScript leaves by keyword and print: filename - item value (from docs) - all keywords

Usage:
  python3 scripts/query_leaves.py <keyword>

Notes:
  - Scans: ChestCavityForge/src/main/resources/data/chestcavity/guscript/leaves
  - Leaf JSON is expected to contain either "item" or "itemID" (e.g., "guzhenren:xie_di_gu").
  - Keywords are read from "tags" (list) or fallback "keywords" (list).
  - Looks up display value from docs/9_19.json via key: item.<namespace>.<path>.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
LEAVES_DIR = PROJECT_ROOT / 'ChestCavityForge' / 'src/main/resources/data/chestcavity/guscript/leaves'
DOC_JSON = PROJECT_ROOT / 'docs' / '9_19.json'


def build_item_lookup(doc_path: Path) -> dict[str, str]:
    """Build a flat map of item.<ns>.<path> -> value (string) from docs JSON."""
    try:
        data = json.loads(doc_path.read_text(encoding='utf-8'))
    except Exception:
        return {}
    mapping: dict[str, str] = {}
    def visit(obj):
        if not isinstance(obj, dict):
            return
        for k, v in obj.items():
            if isinstance(k, str) and k.startswith('item.') and isinstance(v, str):
                mapping[k] = v
            elif isinstance(v, dict):
                visit(v)
    if isinstance(data, dict) and isinstance(data.get('item'), dict):
        visit(data['item'])
    return mapping


def main(argv) -> int:
    if len(argv) < 2:
        print('Usage: python3 scripts/query_leaves.py <keyword>')
        return 2
    query = str(argv[1]).strip()
    if not LEAVES_DIR.is_dir():
        print(f'Leaves directory not found: {LEAVES_DIR}', file=sys.stderr)
        return 1

    item_map = build_item_lookup(DOC_JSON)

    matched = 0
    for path in sorted(LEAVES_DIR.rglob('*.json')):
        try:
            data = json.loads(path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            continue
        item_id = data.get('item') or data.get('itemID') or ''
        if not isinstance(item_id, str):
            continue
        tags = data.get('tags') or data.get('keywords') or []
        if not isinstance(tags, list):
            tags = []
        tags_strs = [str(t) for t in tags]

        # Filter by keyword (substring, case-sensitive by default)
        if query and not any(query in t for t in tags_strs):
            continue

        # Lookup doc value
        value = ''
        if ':' in item_id:
            ns, path_id = item_id.split(':', 1)
            doc_key = f'item.{ns}.{path_id}'
            value = item_map.get(doc_key, '')

        # Output: filename - value - all keywords
        filename = path.name
        print(f'{filename} - {value or "<no-doc>"} - {", ".join(tags_strs)}')
        matched += 1

    if matched == 0:
        print('No matches found.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main(sys.argv))

