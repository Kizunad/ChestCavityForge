import json
from pathlib import Path
root = Path('src/main/resources/data/chestcavity/organs/guzhenren')
items = set()
for path in root.rglob('*.json'):
    try:
        data = json.loads(path.read_text())
    except Exception:
        continue
    for score in data.get('organScores', []):
        if score.get('id') == 'chestcavity:strength':
            item_id = data.get('itemID')
            if item_id:
                items.add(item_id)
            break
for item in sorted(items):
    print(item)
