#!/usr/bin/env python3
"""Generate cultivator gu bug pools from Guzhenren organ registries."""
from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Iterable, List, Set

REPO_ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = REPO_ROOT / "src" / "main" / "java" / "net" / "tigereye" / "chestcavity" / "compat" / "guzhenren" / "item"
CULTIVATOR_TYPES_DIR = REPO_ROOT / "src" / "main" / "resources" / "data" / "chestcavity" / "types" / "compatibility" / "guzhenren" / "cultivators"

# Regex patterns covering the ID declarations inside *OrganRegistry classes.
FROM_NAMESPACE_PATTERN = re.compile(
    r'ResourceLocation\.fromNamespaceAndPath\(\s*(?:MOD_ID|"(?P<namespace>[^"]+)")\s*,\s*"(?P<path>[^"]+)"\s*\)',
    re.MULTILINE,
)
PARSE_PATTERN = re.compile(
    r'ResourceLocation\.(?:parse|tryParse)\(\s*"(?P<full>[^"]+:?[^"]*)"\s*\)',
    re.MULTILINE,
)

MOD_NAMESPACE = "guzhenren"


def is_gu_bug_id(namespace: str, path: str) -> bool:
    if namespace != MOD_NAMESPACE:
        return False
    lower = path.lower()
    if lower.endswith("_gu") or lower.endswith("gu"):
        return True
    if "_gu_" in lower or "gu_" in lower or "_gu" in lower:
        return True
    if "chong" in lower:
        return True
    return False


def iter_registry_files(root: Path) -> Iterable[Path]:
    for path in root.rglob("*OrganRegistry.java"):
        if path.is_file():
            yield path


def extract_ids_from_file(path: Path) -> Set[str]:
    text = path.read_text(encoding="utf-8")
    ids: Set[str] = set()

    for match in FROM_NAMESPACE_PATTERN.finditer(text):
        namespace = match.group("namespace") or MOD_NAMESPACE
        id_path = match.group("path")
        if not namespace:
            namespace = MOD_NAMESPACE
        if is_gu_bug_id(namespace, id_path):
            ids.add(f"{namespace}:{id_path}")

    for match in PARSE_PATTERN.finditer(text):
        full = match.group("full")
        if not full:
            continue
        if ":" not in full:
            # Skip bare IDs that would default to minecraft namespace.
            continue
        namespace, id_path = full.split(":", 1)
        if namespace == "minecraft":
            continue
        if is_gu_bug_id(namespace, id_path):
            ids.add(f"{namespace}:{id_path}")

    return ids


def collect_all_ids(root: Path) -> List[str]:
    collected: Set[str] = set()
    for file in iter_registry_files(root):
        collected.update(extract_ids_from_file(file))
    return sorted(collected)


def build_generator_entry(generator_id: str, items: List[str]) -> dict:
    return {
        "id": generator_id,
        "count": {
            "min": 0,
            "max": 3,
        },
        "items": items,
    }


def update_type_file(path: Path, items: List[str]) -> bool:
    data = json.loads(path.read_text(encoding="utf-8"))
    generator_id = f"chestcavity:cultivators/{path.stem}_gu_bugs"
    generators = data.get("randomGenerators", [])

    # Replace or append the generator entry for this file.
    updated = False
    for index, entry in enumerate(generators):
        if not isinstance(entry, dict):
            continue
        if entry.get("id") == generator_id:
            generators[index] = build_generator_entry(generator_id, items)
            updated = True
            break
    if not updated:
        generators.append(build_generator_entry(generator_id, items))
        updated = True

    data["randomGenerators"] = generators

    serialized = json.dumps(data, ensure_ascii=False, indent=2)
    serialized += "\n"

    if path.read_text(encoding="utf-8") == serialized:
        return False

    path.write_text(serialized, encoding="utf-8")
    return True


def main() -> None:
    if not JAVA_ROOT.exists() or not CULTIVATOR_TYPES_DIR.exists():
        raise SystemExit("Expected directories are missing; aborting.")

    items = collect_all_ids(JAVA_ROOT)
    if not items:
        raise SystemExit("No Guzhenren organ IDs discovered; cannot update pools.")

    changed_any = False
    for type_file in sorted(CULTIVATOR_TYPES_DIR.glob("*.json")):
        if update_type_file(type_file, items):
            changed_any = True

    if changed_any:
        print("Updated cultivator gu bug pools based on Guzhenren organ registries.")
    else:
        print("Cultivator gu bug pools already up to date.")


if __name__ == "__main__":
    main()
