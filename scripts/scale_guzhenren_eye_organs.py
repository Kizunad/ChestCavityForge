import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / 'src/main/resources/data/chestcavity/organs/guzhenren/animal/eye'
BASE_DIR = ROOT / 'bai'
TARGETS = {
    'qian': 2.0,
    'wan': 4.0,
    'shouhuang': 8.0,
}


def load_base_profiles():
    profiles = {}
    for path in sorted(BASE_DIR.glob('*.json')):
        data = json.loads(path.read_text())
        item_id = data.get('itemID', '')
        name = item_id.split(':', 1)[1] if ':' in item_id else item_id
        suffix = name.removeprefix('baishou')
        if not suffix:
            continue
        scores = {}
        for entry in data.get('organScores', []):
            score_id = entry.get('id')
            if score_id == 'chestcavity:luck':
                continue
            try:
                value = float(entry.get('value', 0.0))
            except (TypeError, ValueError):
                continue
            scores[score_id] = value
        if scores:
            profiles[suffix] = scores
    return profiles


def update_target(path: Path, profiles: dict[str, dict[str, float]], factor: float) -> bool:
    data = json.loads(path.read_text())
    item_id = data.get('itemID', '')
    name = item_id.split(':', 1)[1] if ':' in item_id else item_id
    matched_suffix = None
    for suffix in profiles:
        if name.endswith(suffix):
            matched_suffix = suffix
            break
    if matched_suffix is None:
        return False
    base_scores = profiles[matched_suffix]
    organ_scores = data.setdefault('organScores', [])
    existing = {entry.get('id'): entry for entry in organ_scores}
    changed = False
    for score_id, base_value in base_scores.items():
        scaled = base_value * factor
        formatted = format(scaled, '.6f').rstrip('0').rstrip('.')
        if not formatted:
            formatted = '0'
        entry = existing.get(score_id)
        if entry is None:
            organ_scores.append({'id': score_id, 'value': formatted})
            changed = True
        elif entry.get('value') != formatted:
            entry['value'] = formatted
            changed = True
    if changed:
        luck_entries = [entry for entry in organ_scores if entry.get('id') == 'chestcavity:luck']
        others = [entry for entry in organ_scores if entry.get('id') != 'chestcavity:luck']
        data['organScores'] = luck_entries + others
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    return changed


def main():
    profiles = load_base_profiles()
    if not profiles:
        raise SystemExit('No base profiles found in bai directory')
    total = 0
    for folder, factor in TARGETS.items():
        dir_path = ROOT / folder
        if not dir_path.is_dir():
            continue
        for path in sorted(dir_path.glob('*.json')):
            if update_target(path, profiles, factor):
                total += 1
    print(f'Updated {total} files')


if __name__ == '__main__':
    main()
