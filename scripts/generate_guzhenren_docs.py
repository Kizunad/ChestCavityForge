#!/usr/bin/env python3
"""
批量为蛊真人动物器官生成 ModernUI 图鉴文档，按需可扩展到其他分组。

# 输出路径：src/main/resources/assets/guzhenren/docs/<group>/...
规则：
  - id 与 icon = itemID
  - title 来自主资源包（decompile/.../lang/en_us.json）的 item 本地化
  - summary 置空
  - tags = [title, itemID]
  - details = 按 organScores 列出 “<简体名称>：<值>”
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence

REPO_ROOT = Path(__file__).resolve().parent.parent

CHEST_ZH_LANG_PATH = REPO_ROOT / "src" / "main" / "resources" / "assets" / "chestcavity" / "lang" / "zh_cn.json"

ORGANS_BASE = REPO_ROOT / "src" / "main" / "resources" / "data" / "chestcavity" / "organs" / "guzhenren"
DOCS_ROOT = REPO_ROOT / "src" / "main" / "resources" / "assets" / "guzhenren" / "docs"

ALL_GROUPS: Sequence[str] = ("animal", "gu_cai", "human")
DEFAULT_GROUPS: Sequence[str] = ("animal",)

SCORE_OVERRIDE_PATH = REPO_ROOT / "scripts" / "data" / "score_name_overrides.json"


def _load_overrides() -> Dict[str, str]:
    if not SCORE_OVERRIDE_PATH.exists():
        print(f"[warn] 未找到评分名称映射文件：{SCORE_OVERRIDE_PATH}", file=sys.stderr)
        return {}
    with SCORE_OVERRIDE_PATH.open(encoding="utf-8") as fp:
        data = json.load(fp)
    if not isinstance(data, dict):
        print(f"[warn] 映射文件格式异常（非对象）：{SCORE_OVERRIDE_PATH}", file=sys.stderr)
        return {}
    return {str(k): str(v) for k, v in data.items()}


# 额外重命名表：来自预生成 JSON，便于人工校验
SCORE_NAME_OVERRIDES: Dict[str, str] = _load_overrides()


@dataclass(frozen=True)
class Context:
    item_lang: Dict[str, str]
    chest_lang: Dict[str, str]


def resolve_item_lang_path(lang_override: Path | None) -> Path:
    if lang_override:
        override_path = lang_override.resolve()
        if not override_path.exists():
            raise SystemExit(f"[error] 指定的语言文件不存在：{override_path}")
        return override_path

    candidates = [
        REPO_ROOT / "decompile" / "10_6_decompile" / "assets" / "guzhenren" / "lang" / "en_us.json",
        REPO_ROOT.parent / "decompile" / "10_6_decompile" / "assets" / "guzhenren" / "lang" / "en_us.json",
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate

    print("[error] 未找到蛊真人物品语言文件，请传入路径或检查 decompile 目录。", file=sys.stderr)
    raise SystemExit(1)


def load_language(lang_override: Path | None) -> Context:
    lang_path = resolve_item_lang_path(lang_override)
    with lang_path.open(encoding="utf-8") as fp:
        item_lang = json.load(fp)

    if CHEST_ZH_LANG_PATH.exists():
        with CHEST_ZH_LANG_PATH.open(encoding="utf-8") as fp:
            chest_lang = json.load(fp)
    else:
        chest_lang = {}

    return Context(item_lang=item_lang, chest_lang=chest_lang)


def score_display_name(score_id: str, ctx: Context) -> str:
    if score_id in SCORE_NAME_OVERRIDES:
        return SCORE_NAME_OVERRIDES[score_id]

    try:
        namespace, path = score_id.split(":", 1)
    except ValueError:
        return score_id

    key = f"organscore.{namespace}.{path}"
    raw = ctx.chest_lang.get(key)
    if raw:
        name = raw.replace("%s", "").strip()
        if name:
            return name
    return score_id


def resolve_item_title(item_id: str, ctx: Context) -> str:
    if ":" not in item_id:
        return item_id
    namespace, path = item_id.split(":", 1)
    lang_key = f"item.{namespace}.{path}"
    title = ctx.item_lang.get(lang_key)
    if title:
        return title
    # 回退：尝试简单格式化 id
    return path.replace("_", " ")


def build_doc(entry_path: Path, ctx: Context) -> Dict[str, object]:
    with entry_path.open(encoding="utf-8") as fp:
        data = json.load(fp)

    item_id = data.get("itemID")
    if not item_id:
        raise ValueError(f"{entry_path} 缺少 itemID 字段")

    title = resolve_item_title(item_id, ctx)

    details: List[str] = []
    for node in data.get("organScores", []):
        score_id = node.get("id")
        value = node.get("value")
        if not score_id or value is None:
            continue
        name = score_display_name(score_id, ctx)
        details.append(f"{name}：{format_value(value)}")

    return {
        "id": item_id,
        "title": title,
        "summary": "",
        "details": details,
        "tags": [title, item_id],
        "icon": item_id,
    }


def is_number_value(value) -> bool:
    if isinstance(value, (int, float)):
        return True
    if isinstance(value, str):
        try:
            float(value)
            return True
        except ValueError:
            return False
    return False


def format_value(value) -> str:
    if isinstance(value, str):
        value = value.strip()
    if is_number_value(value):
        num = float(value)
        if abs(num) >= 1:
            formatted = f"{num:.3f}".rstrip("0").rstrip(".")
        else:
            formatted = f"{num:.4f}".rstrip("0").rstrip(".")
        return formatted if formatted else "0"
    return str(value)


def iter_target_groups(groups: Iterable[str]) -> List[str]:
    selected = list(dict.fromkeys(groups))
    normalized: List[str] = []
    for group in selected:
        if group == "all":
            normalized.extend(ALL_GROUPS)
        else:
            normalized.append(group)
    # Preserve order but deduplicate
    seen = set()
    ordered: List[str] = []
    for g in normalized:
        if g not in ALL_GROUPS:
            raise SystemExit(f"[error] 不支持的分组：{g}")
        if g not in seen:
            seen.add(g)
            ordered.append(g)
    return ordered or list(DEFAULT_GROUPS)


def main() -> None:
    parser = argparse.ArgumentParser(description="生成蛊真人器官图鉴文档")
    parser.add_argument(
        "--groups",
        nargs="+",
        default=list(DEFAULT_GROUPS),
        help="指定要生成的分组，默认仅 animal。可用：animal、gu_cai、human、all",
    )
    parser.add_argument(
        "--lang-path",
        type=Path,
        help="覆盖默认的 en_us.json 路径",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="仅展示将被写入的文件，不修改磁盘",
    )
    args = parser.parse_args()

    groups = iter_target_groups(args.groups)

    if not ORGANS_BASE.exists():
        print(f"[error] 找不到器官数据目录：{ORGANS_BASE}", file=sys.stderr)
        raise SystemExit(1)

    ctx = load_language(args.lang_path)
    apply_changes = not args.dry_run
    created = 0
    updated = 0
    skipped = 0
    dry_run_changes: List[str] = []

    for group in groups:
        source_root = ORGANS_BASE / group
        if not source_root.exists():
            print(f"[warn] 跳过缺失目录 {source_root}", file=sys.stderr)
            continue

        for src_path in sorted(source_root.rglob("*.json")):
            relative = src_path.relative_to(source_root)
            out_path = DOCS_ROOT / group / relative
            out_path.parent.mkdir(parents=True, exist_ok=True)

            doc = build_doc(src_path, ctx)
            had_existing = out_path.exists()
            existing_doc = None

            if had_existing:
                try:
                    with out_path.open("r", encoding="utf-8") as fp:
                        existing_doc = json.load(fp)
                except json.JSONDecodeError:
                    existing_doc = None

                if existing_doc == doc:
                    skipped += 1
                    continue

            if apply_changes:
                with out_path.open("w", encoding="utf-8") as fp:
                    json.dump(doc, fp, ensure_ascii=False, indent=2)
                    fp.write("\n")
            else:
                dry_run_changes.append(str(out_path))

            if had_existing:
                updated += 1
            else:
                created += 1

    if apply_changes:
        print(
            f"[done] 新增 {created} 个图鉴模板，更新 {updated} 个文件（跳过 {skipped} 个无需变更），输出目录：{DOCS_ROOT}"
        )
    else:
        print(
            f"[dry-run] 预计新增 {created} 个、更新 {updated} 个、跳过 {skipped} 个。受影响文件："
        )
        for path in dry_run_changes:
            print(f" - {path}")
        print("移除 --dry-run 即可写入文件。")


if __name__ == "__main__":
    main()
