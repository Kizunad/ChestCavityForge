#!/usr/bin/env python3
"""
将人道/剑道(jian_dao) 文档统一为：

details = [上半部分说明..., "----", 下半部分“器官属性：数值”列表]

规则：
- 上半部分：保留现有 details 中非“名称：数字”的行（不改变顺序与内容）；
- 下半部分：从对应 organs 源 JSON 重建属性行（名称取自 overrides -> zh_cn 翻译，数值格式与生成脚本一致）；
- 若既无上半也无属性，则不改动；若仅有上半或仅有属性，不强行加入分隔符；
- 默认 dry-run，需 --apply 写入。
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple

REPO_ROOT = Path(__file__).resolve().parent.parent

DOCS_DIR = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "guzhenren"
    / "docs"
    / "human"
    / "jian_dao"
)

ORGANS_DIR = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "chestcavity"
    / "organs"
    / "guzhenren"
    / "human"
    / "jian_dao"
)

CHEST_ZH_LANG_PATH = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "chestcavity"
    / "lang"
    / "zh_cn.json"
)

SCORE_OVERRIDE_PATH = REPO_ROOT / "scripts" / "data" / "score_name_overrides.json"


def _load_json(path: Path):
    with path.open(encoding="utf-8") as fp:
        return json.load(fp)


def _load_overrides() -> Dict[str, str]:
    if not SCORE_OVERRIDE_PATH.exists():
        return {}
    try:
        data = _load_json(SCORE_OVERRIDE_PATH)
    except Exception:
        return {}
    if isinstance(data, dict):
        return {str(k): str(v) for k, v in data.items()}
    return {}


def _load_chest_lang() -> Dict[str, str]:
    if not CHEST_ZH_LANG_PATH.exists():
        return {}
    try:
        data = _load_json(CHEST_ZH_LANG_PATH)
    except Exception:
        return {}
    if isinstance(data, dict):
        return {str(k): str(v) for k, v in data.items()}
    return {}


SCORE_NAME_OVERRIDES = _load_overrides()
CHEST_LANG = _load_chest_lang()


def score_display_name(score_id: str) -> str:
    if score_id in SCORE_NAME_OVERRIDES:
        return SCORE_NAME_OVERRIDES[score_id]
    try:
        namespace, path = score_id.split(":", 1)
    except ValueError:
        return score_id
    key = f"organscore.{namespace}.{path}"
    raw = CHEST_LANG.get(key)
    if raw:
        name = raw.replace("%s", "").strip()
        if name:
            return name
    return score_id


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


ATTR_LINE_RE = re.compile(r"^(?P<name>.+?)：\s*(?P<value>[-+]?\d+(?:\.\d+)?)\s*$")


def build_attributes_from_organ(src_path: Path) -> List[str]:
    data = _load_json(src_path)
    out: List[str] = []
    for node in data.get("organScores", []):
        sid = str(node.get("id"))
        val = node.get("value")
        if not sid or val is None:
            continue
        out.append(f"{score_display_name(sid)}：{format_value(val)}")
    return out


def restructure_doc(doc_path: Path, apply: bool) -> Tuple[bool, int, int]:
    # returns (changed, top_count, attr_count)
    rel = doc_path.name
    src_path = ORGANS_DIR / rel

    with doc_path.open(encoding="utf-8") as fp:
        doc = json.load(fp)

    orig_details = doc.get("details")
    if not isinstance(orig_details, list):
        return (False, 0, 0)

    # 分离现有上半部分（非属性行）
    top: List[str] = []
    for item in orig_details:
        if isinstance(item, str) and not ATTR_LINE_RE.match(item) and item.strip() != "----":
            top.append(item)

    # 构建下半部分属性
    attrs: List[str] = []
    if src_path.exists():
        try:
            attrs = build_attributes_from_organ(src_path)
        except Exception:
            attrs = []

    # 若没有任何改变必要，直接跳过
    if not top and not attrs:
        return (False, 0, 0)

    new_details: List[str] = []
    if top:
        new_details.extend(top)
    if top and attrs:
        new_details.append("----")
    if attrs:
        new_details.extend(attrs)

    if new_details == orig_details:
        return (False, len(top), len(attrs))

    if apply:
        doc["details"] = new_details
        with doc_path.open("w", encoding="utf-8") as fp:
            json.dump(doc, fp, ensure_ascii=False, indent=2)
            fp.write("\n")

    return (True, len(top), len(attrs))


def main() -> None:
    parser = argparse.ArgumentParser(description="重构 jian_dao 文档为 上半/----/下半(器官属性) 结构")
    parser.add_argument("--apply", action="store_true", help="写入更改（默认 dry-run）")
    args = parser.parse_args()

    if not DOCS_DIR.exists():
        print(f"[error] 未找到文档目录：{DOCS_DIR}", file=sys.stderr)
        raise SystemExit(1)

    changed = 0
    scanned = 0
    for p in sorted(DOCS_DIR.glob("*.json")):
        scanned += 1
        c, top_n, attr_n = restructure_doc(p, apply=args.apply)
        if c:
            changed += 1

    mode = "write" if args.apply else "dry-run"
    print(f"[done:{mode}] 扫描 {scanned} 个文件，变更 {changed} 个。")


if __name__ == "__main__":
    main()

