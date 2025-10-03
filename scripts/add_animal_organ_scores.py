#!/usr/bin/env python3
"""
Populate organScores for Guzhenren animal organs by animal type and rank.

Targets files under:
  ChestCavityForge/src/main/resources/data/chestcavity/organs/guzhenren/animal

Rules
- Animal base scores:
  犬(quan):  health 1, strength 1, speed 1
  虎(hu):    health 1, strength 2, speed 1, impact_resistant 1
  熊(xiong): health 2, strength 2, defense 2, impact_resistant 1
  狼(lang):  strength 1, arrow_dodging 1, speed 2
  羚(ling):  health 2, filtration 1, defense 2

- Rank bonuses:
  百兽王(bai):      guzhenren:zuida_zhenyuan 1, guzhenren:zuida_jingli 1
  千兽王(qian):     + zhenyuan 4, jingli 4, niantou_zuida 1
  万兽王(wan):      + zhenyuan 8, jingli 8, niantou_zuida 4, zuida_hunpo 1
  兽皇(shouhuang): + zhenyuan 16, jingli 16, niantou_zuida 8, zuida_hunpo 4, shouyuan 1

Usage
  Dry-run (default): prints what would change
    python scripts/add_animal_organ_scores.py

  Apply changes in-place:
    python scripts/add_animal_organ_scores.py --apply
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple

REPO_ROOT = Path(__file__).resolve().parents[1]
TARGET_DIR = REPO_ROOT / 'src/main/resources/data/chestcavity/organs/guzhenren/animal'


# Map animal keyword -> base organ scores
ANIMAL_BASE: Dict[str, Dict[str, int]] = {
    'quan': {
        'chestcavity:health': 10,
        'chestcavity:strength': 10,
        'chestcavity:speed': 10,
    },
    'hu': {
        'chestcavity:health': 10,
        'chestcavity:strength': 20,
        'chestcavity:speed': 10,
        'chestcavity:impact_resistant': 10,
    },
    'xiong': {
        'chestcavity:health': 20,
        'chestcavity:strength': 20,
        'chestcavity:defense': 20,
        'chestcavity:impact_resistant': 10,
    },
    'lang': {
        'chestcavity:strength': 10,
        'chestcavity:arrow_dodging': 10,
        'chestcavity:speed': 20,
    },
    # 羚(羊) — filenames may include ling or (typo) liangyang
    'ling': {
        'chestcavity:health': 20,
        'chestcavity:filtration': 10,
        'chestcavity:defense': 20,
    },
}

# Rank bonuses
RANK_BONUS: Dict[str, Dict[str, int]] = {
    'bai': {  # 百兽王
        'guzhenren:zuida_zhenyuan': 10,
        'guzhenren:zuida_jingli': 10,
    },
    'qian': {  # 千兽王
        'guzhenren:zuida_zhenyuan': 40,
        'guzhenren:zuida_jingli': 40,
        'guzhenren:niantou_zuida': 10,
    },
    'wan': {  # 万兽王
        'guzhenren:zuida_zhenyuan': 80,
        'guzhenren:zuida_jingli': 80,
        'guzhenren:niantou_zuida': 40,
        'guzhenren:zuida_hunpo': 10,
    },
    'shouhuang': {  # 兽皇
        'guzhenren:zuida_zhenyuan': 160,
        'guzhenren:zuida_jingli': 160,
        'guzhenren:niantou_zuida': 80,
        'guzhenren:zuida_hunpo': 40,
        'guzhenren:shouyuan': 10,
    },
}


ANIMAL_PATTERNS: List[Tuple[str, re.Pattern]] = [
    ('quan', re.compile(r'quan', re.I)),
    ('hu', re.compile(r'(?<!shou)hu(?!ang)', re.I)),  # avoid matching in 'shouhuang'
    ('xiong', re.compile(r'xiong', re.I)),
    ('lang', re.compile(r'lang', re.I)),
    ('ling', re.compile(r'ling|liangyang|lingyang', re.I)),
]

# Rank power for base scaling: add ANIMAL_BASE * (2^n)
# Effective base contribution becomes base * (1 + 2^n)
RANK_POWER: Dict[str, int] = {
    'bai': 1,
    'qian': 2,
    'wan': 3,
    'shouhuang': 4,
}


def detect_animal_key(path: Path) -> str | None:
    name = path.name.lower()
    for key, pat in ANIMAL_PATTERNS:
        if pat.search(name):
            return key
    # Try parent directory name if filename failed
    pdata = path.parent.name.lower()
    for key, pat in ANIMAL_PATTERNS:
        if pat.search(pdata):
            return key
    return None


def detect_rank_key(path: Path) -> str | None:
    # eye/muscle/skin have rank folders
    parent = path.parent
    if parent.name in RANK_BONUS:
        return parent.name

    # bone files encode rank in filename
    name = path.name.lower()
    if name.startswith('baishou') or 'baishouwang' in name:
        return 'bai'
    if name.startswith('qianshou') or 'qianshouwang' in name:
        return 'qian'
    if name.startswith('wanshou') or 'wanshouwang' in name:
        return 'wan'
    # tolerate typos like shouhang -> shouhuang
    if 'shouhuang' in name or 'shouhang' in name:
        return 'shouhuang'
    return None


def load_json(p: Path) -> dict:
    with p.open('r', encoding='utf-8') as f:
        return json.load(f)


def save_json(p: Path, data: dict) -> None:
    text = json.dumps(data, ensure_ascii=False, indent=2)
    text += "\n"
    with p.open('w', encoding='utf-8') as f:
        f.write(text)


def merge_scores(existing: List[dict], additions: Dict[str, int]) -> List[dict]:
    by_id: Dict[str, int] = {}
    for e in existing or []:
        try:
            by_id[e['id']] = int(str(e['value']))
        except Exception:
            # Keep as-is if unparsable
            try:
                by_id[e['id']] = e['value']
            except Exception:
                pass

    # Overwrite/insert new values
    for oid, val in additions.items():
        by_id[oid] = val

    # Stable-ish order: chestcavity:* first, then guzhenren:*, then others
    def sort_key(item: Tuple[str, int]) -> Tuple[int, str]:
        oid, _ = item
        if oid.startswith('chestcavity:'):
            return (0, oid)
        if oid.startswith('guzhenren:'):
            return (1, oid)
        return (2, oid)

    return [{"id": k, "value": str(v)} for k, v in sorted(by_id.items(), key=sort_key)]


@dataclass
class UpdateResult:
    path: Path
    animal: str | None
    rank: str | None
    applied: bool
    before_count: int
    after_count: int


def process_file(p: Path, apply: bool) -> UpdateResult:
    data = load_json(p)
    animal = detect_animal_key(p)
    rank = detect_rank_key(p)

    base = ANIMAL_BASE.get(animal or '', {})
    bonus = RANK_BONUS.get(rank or '', {})

    # Scale base by (1 + 2^n) where n depends on rank, if rank recognized
    mult = 1 + (2 ** RANK_POWER[rank]) if rank in RANK_POWER else 1
    scaled_base = {k: v * mult for k, v in base.items()}

    additions = {**scaled_base, **bonus}

    before = data.get('organScores', [])
    after = merge_scores(before, additions)

    applied = False
    if apply and after != before:
        data['organScores'] = after
        save_json(p, data)
        applied = True

    return UpdateResult(
        path=p,
        animal=animal,
        rank=rank,
        applied=applied,
        before_count=len(before or []),
        after_count=len(after or []),
    )


def main():
    ap = argparse.ArgumentParser(description='Populate Guzhenren animal organScores')
    ap.add_argument('--apply', action='store_true', help='Write changes to files')
    args = ap.parse_args()

    if not TARGET_DIR.exists():
        raise SystemExit(f'Target dir not found: {TARGET_DIR}')

    json_files = sorted(TARGET_DIR.rglob('*.json'))
    results: List[UpdateResult] = []
    for p in json_files:
        results.append(process_file(p, args.apply))

    # Summary
    updated = sum(1 for r in results if r.applied)
    print(f'Processed {len(results)} files. Will apply: {args.apply}. Updated: {updated}.')
    # Show a few examples per animal
    for r in results:
        if r.animal and r.rank and (r.applied or not args.apply):
            print(f'- {r.path.relative_to(REPO_ROOT)} :: animal={r.animal} rank={r.rank} scores {r.before_count}->{r.after_count} {"(applied)" if r.applied else ""}')


if __name__ == '__main__':
    main()
