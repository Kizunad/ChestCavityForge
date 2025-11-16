#!/usr/bin/env python3
"""
更新 guzhenren 文档(details 数值) 与占位符替换工具。

用途：
- 仅更新 UI 文档中形如 "<名称>：<数字>" 的行的数值，以器官源 JSON 的 organScores 为准；
- 可选：将文档中的占位符 {VAR} 用 Tuning JSON 中的数值替换（支持 summary 与 details）。

设计要点（KISS/DRY/YAGNI）：
- 不重写整份文档，仅做增量数值纠正；
- 名称解析沿用生成脚本逻辑：score_name_overrides.json 优先，其次 chestcavity zh_cn.json 的 organscore.*；
- 默认 dry-run；需 --apply 才落盘；
- 仅支持 JSON 的 Tuning 文件（需要 YAML 可后续扩展）。
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, MutableMapping, Optional, Tuple
import ast

REPO_ROOT = Path(__file__).resolve().parent.parent

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

ORGANS_BASE = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "chestcavity"
    / "organs"
    / "guzhenren"
)

DOCS_ROOT = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "guzhenren"
    / "docs"
)

ALL_GROUPS: Tuple[str, ...] = ("animal", "gu_cai", "human")

SCORE_OVERRIDE_PATH = REPO_ROOT / "scripts" / "data" / "score_name_overrides.json"

TUNING_JAVA_DIR = (
    REPO_ROOT
    / "src"
    / "main"
    / "java"
    / "net"
    / "tigereye"
    / "chestcavity"
    / "compat"
    / "guzhenren"
    / "item"
    / "jian_dao"
    / "tuning"
)

PLACEHOLDER_BACKUP_DIR = REPO_ROOT / "build" / "tmp" / "guzhenren_docs_placeholder_backup"


def _load_json(path: Path) -> object:
    with path.open(encoding="utf-8") as fp:
        return json.load(fp)


def _load_overrides() -> Dict[str, str]:
    if not SCORE_OVERRIDE_PATH.exists():
        print(f"[warn] 未找到评分名称映射文件：{SCORE_OVERRIDE_PATH}", file=sys.stderr)
        return {}
    data = _load_json(SCORE_OVERRIDE_PATH)
    if not isinstance(data, dict):
        print(f"[warn] 映射文件格式异常（非对象）：{SCORE_OVERRIDE_PATH}", file=sys.stderr)
        return {}
    return {str(k): str(v) for k, v in data.items()}


SCORE_NAME_OVERRIDES: Dict[str, str] = _load_overrides()


def _load_chest_lang() -> Dict[str, str]:
    if CHEST_ZH_LANG_PATH.exists():
        data = _load_json(CHEST_ZH_LANG_PATH)
        if isinstance(data, dict):
            return {str(k): str(v) for k, v in data.items()}
    return {}


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


@dataclass(frozen=True)
class UpdateStats:
    files_scanned: int = 0
    files_changed: int = 0
    value_updates: int = 0
    placeholder_updates: int = 0


NAME_VALUE_RE = re.compile(r"^(?P<name>.+?)：\s*(?P<value>[-+]?\d+(?:\.\d+)?)\s*$")


def build_expected_name_to_value_map(src_path: Path) -> Dict[str, str]:
    data = _load_json(src_path)
    result: Dict[str, str] = {}
    for node in data.get("organScores", []):
        score_id = node.get("id")
        value = node.get("value")
        if not score_id or value is None:
            continue
        name = score_display_name(str(score_id))
        result[name] = format_value(value)
    return result


def substitute_placeholders_in_str(s: str, vars_map: Mapping[str, object]) -> Tuple[str, int]:
    # 简单 {VAR} 替换（不含格式化），仅替换存在的 key。
    changed = 0
    def repl(match: re.Match[str]) -> str:
        nonlocal changed
        key = match.group(1)
        if key in vars_map:
            changed += 1
            return str(vars_map[key])
        return match.group(0)

    out = re.sub(r"\{([A-Za-z0-9_\.]+)\}", repl, s)
    return out, changed


def process_doc_file(
    doc_path: Path,
    group: str,
    tuning: Optional[Mapping[str, object]],
    apply: bool,
    render_placeholders: bool,
) -> Tuple[bool, int, int]:
    # returns: (changed?, value_updates, placeholder_updates)
    # 推导源 JSON 路径：与生成器保持同构目录结构
    try:
        rel = doc_path.relative_to(DOCS_ROOT / group)
    except Exception:
        return (False, 0, 0)

    src_path = ORGANS_BASE / group / rel
    expected_map: Dict[str, str] = {}
    if src_path.exists():
        expected_map = build_expected_name_to_value_map(src_path)

    with doc_path.open(encoding="utf-8") as fp:
        doc = json.load(fp)

    changed = False
    value_updates = 0
    placeholder_updates = 0

    details = doc.get("details")
    if isinstance(details, list):
        new_details: List[object] = []
        for item in details:
            if isinstance(item, str):
                m = NAME_VALUE_RE.match(item)
                if m:
                    name = m.group("name")
                    old_val = m.group("value")
                    if name in expected_map:
                        new_val = expected_map[name]
                        if new_val != old_val:
                            item = f"{name}：{new_val}"
                            changed = True
                            value_updates += 1
                # 占位符替换（details 字符串）
                if render_placeholders and tuning:
                    replaced, cnt = substitute_placeholders_in_str(item, tuning)
                    if cnt > 0 and replaced != item:
                        item = replaced
                        changed = True
                        placeholder_updates += cnt
                new_details.append(item)
            else:
                new_details.append(item)
        doc["details"] = new_details

    # 占位符替换（summary）
    if render_placeholders and tuning and isinstance(doc.get("summary"), str):
        replaced, cnt = substitute_placeholders_in_str(doc["summary"], tuning)
        if cnt > 0 and replaced != doc["summary"]:
            doc["summary"] = replaced
            changed = True
            placeholder_updates += cnt

    if changed and apply:
        with doc_path.open("w", encoding="utf-8") as fp:
            json.dump(doc, fp, ensure_ascii=False, indent=2)
            fp.write("\n")

    return (changed, value_updates, placeholder_updates)


def iter_target_groups(groups: Iterable[str]) -> List[str]:
    selected = list(dict.fromkeys(groups))
    normalized: List[str] = []
    for group in selected:
        if group == "all":
            normalized.extend(ALL_GROUPS)
        else:
            normalized.append(group)
    # Preserve order but deduplicate and validate
    seen = set()
    ordered: List[str] = []
    for g in normalized:
        if g not in ALL_GROUPS:
            raise SystemExit(f"[error] 不支持的分组：{g}")
        if g not in seen:
            seen.add(g)
            ordered.append(g)
    return ordered or list(ALL_GROUPS)


def list_doc_files(groups: List[str]) -> List[tuple[str, Path]]:
    entries: List[tuple[str, Path]] = []
    for group in groups:
        doc_root = DOCS_ROOT / group
        if not doc_root.exists():
            print(f"[warn] 跳过缺失目录 {doc_root}", file=sys.stderr)
            continue
        for doc_path in sorted(doc_root.rglob("*.json")):
            entries.append((group, doc_path))
    return entries


def load_tuning_json(path: Optional[Path]) -> Optional[Mapping[str, object]]:
    if not path:
        return None
    if not path.exists():
        raise SystemExit(f"[error] Tuning 文件不存在：{path}")
    if path.suffix.lower() not in {".json"}:
        raise SystemExit("[error] 目前仅支持 JSON 格式的 Tuning 文件")
    data = _load_json(path)
    if not isinstance(data, dict):
        raise SystemExit("[error] Tuning 文件必须为对象(JSON) 格式")
    return data


NUM_LITERAL_TOKEN_RE = re.compile(r"(?P<num>\d+(?:\.\d+)?)(?:[fFdDlL])\b")
CONFIG_DEFAULT_RE = re.compile(
    r"BehaviorConfigAccess\.get(?:Float|Int)\([^,]+,[^,]+,(?P<default>[^\)]+)\)"
)


def _strip_java_numeric_suffixes(expr: str) -> str:
    # 移除数字字面量后缀 f/F/d/D/l/L
    return NUM_LITERAL_TOKEN_RE.sub(lambda m: m.group("num"), expr)


def _safe_eval_numeric_expr(expr: str) -> Optional[float]:
    # 仅支持 + - * / // % 括号 与数字字面量
    expr = expr.strip()
    if not expr:
        return None
    # 去除显式类型转换
    expr = re.sub(r"^\((?:double|float|int|long)\)\s*", "", expr)
    # BehaviorConfigAccess 默认值提取
    config_match = CONFIG_DEFAULT_RE.search(expr)
    if config_match:
        expr = config_match.group("default")
    # 去掉 Java 数字分隔下划线
    expr = expr.replace("_", "")
    # 去掉可能的类型后缀
    expr = _strip_java_numeric_suffixes(expr)
    try:
        node = ast.parse(expr, mode="eval")
    except SyntaxError:
        return None

    allowed_nodes = (
        ast.Expression,
        ast.BinOp,
        ast.UnaryOp,
        ast.Add,
        ast.Sub,
        ast.Mult,
        ast.Div,
        ast.FloorDiv,
        ast.Mod,
        ast.Pow,
        ast.USub,
        ast.UAdd,
        ast.Constant,
        ast.Load,
        ast.Tuple,
    )

    for n in ast.walk(node):
        if not isinstance(n, allowed_nodes):
            return None
        if isinstance(n, ast.Constant) and not isinstance(n.value, (int, float)):
            return None

    try:
        value = eval(compile(node, "<expr>", "eval"), {"__builtins__": {}}, {})
    except Exception:
        return None
    try:
        return float(value)
    except Exception:
        return None


CONST_DEF_RE = re.compile(
    r"(?P<qual>(?:public|private|protected)\s+)?static\s+final\s+"
    r"(?P<type>int|long|float|double|short|byte)\s+"
    r"(?P<name>[A-Z0-9_]+)\s*=\s*(?P<expr>[^;]+);",
    re.IGNORECASE | re.MULTILINE,
)


def parse_java_constants(java_path: Path, class_name_hint: Optional[str] = None) -> Dict[str, str]:
    try:
        content = java_path.read_text(encoding="utf-8")
    except Exception:
        return {}
    # 粗略移除 /* */ 注释，保留换行对位
    content = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), content, flags=re.S)
    # 移除行内 // 注释内容
    content = re.sub(r"//.*", "", content)

    # 提取类名
    class_name = class_name_hint
    if not class_name:
        m = re.search(r"class\s+([A-Za-z0-9_]+)", content)
        if m:
            class_name = m.group(1)

    result: Dict[str, str] = {}
    for m in CONST_DEF_RE.finditer(content):
        name = m.group("name")
        expr = m.group("expr").strip()
        value = _safe_eval_numeric_expr(expr)
        if value is None:
            # 放弃无法静态评估的表达式
            continue
        formatted = format_value(value)
        result[name] = formatted
        if class_name:
            result[f"{class_name}.{name}"] = formatted
    return result


def load_tuning_from_java(java_dir: Optional[Path]) -> Dict[str, str]:
    mapping: Dict[str, str] = {}
    base = java_dir if java_dir else TUNING_JAVA_DIR
    if not base.exists():
        return mapping
    for p in sorted(base.rglob("*.java")):
        mapping.update(parse_java_constants(p))
    return mapping


def backup_placeholder_sources(entries: List[tuple[str, Path]]) -> None:
    if PLACEHOLDER_BACKUP_DIR.exists():
        shutil.rmtree(PLACEHOLDER_BACKUP_DIR)
    for _, doc_path in entries:
        rel = doc_path.relative_to(DOCS_ROOT)
        backup_path = PLACEHOLDER_BACKUP_DIR / rel
        backup_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(doc_path, backup_path)


def restore_placeholders_from_backup() -> int:
    if not PLACEHOLDER_BACKUP_DIR.exists():
        print("[info] 未找到占位符备份目录，无需还原。")
        return 0
    restored = 0
    for backup_path in sorted(PLACEHOLDER_BACKUP_DIR.rglob("*.json")):
        rel = backup_path.relative_to(PLACEHOLDER_BACKUP_DIR)
        target = DOCS_ROOT / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(backup_path, target)
        restored += 1
    shutil.rmtree(PLACEHOLDER_BACKUP_DIR)
    return restored


def main() -> None:
    parser = argparse.ArgumentParser(description="更新 guzhenren 文档中的数值与占位符")
    parser.add_argument(
        "--groups",
        nargs="+",
        default=["animal", "gu_cai", "human"],
        help="指定要更新的分组，可用：animal、gu_cai、human、all",
    )
    parser.add_argument(
        "--tuning",
        type=Path,
        help="Tuning JSON 文件路径（用于 {VAR} 占位符替换）",
    )
    parser.add_argument(
        "--tuning-java-dir",
        type=Path,
        default=TUNING_JAVA_DIR,
        help="从 Java 源码扫描常量(默认扫描 jian_dao/tuning 目录)",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="执行写入（默认 dry-run，仅显示将发生的更改统计）",
    )
    parser.add_argument(
        "--render-placeholders",
        action="store_true",
        help="构建前渲染 {VAR} → 数值（会自动备份占位符版本）",
    )
    parser.add_argument(
        "--restore-placeholders",
        action="store_true",
        help="构建后还原占位符版本（依赖 render 时生成的备份）",
    )

    args = parser.parse_args()

    if args.render_placeholders and args.restore_placeholders:
        raise SystemExit("[error] --render-placeholders 与 --restore-placeholders 不能同时使用")

    groups = iter_target_groups(args.groups)

    if not ORGANS_BASE.exists():
        print(f"[error] 找不到器官数据目录：{ORGANS_BASE}", file=sys.stderr)
        raise SystemExit(1)
    if not DOCS_ROOT.exists():
        print(f"[error] 找不到文档目录：{DOCS_ROOT}", file=sys.stderr)
        raise SystemExit(1)

    if args.restore_placeholders:
        restored = restore_placeholders_from_backup()
        print(f"[done:restore] 还原 {restored} 个文件。")
        return

    doc_entries = list_doc_files(groups)

    if args.render_placeholders:
        if not args.apply:
            print("[info] render 模式自动启用写入 (--apply)")
            args.apply = True
        backup_placeholder_sources(doc_entries)

    tuning_map: Dict[str, object] = {}
    # 1) Java 常量（自动）
    java_vars = load_tuning_from_java(args.tuning_java_dir)
    tuning_map.update(java_vars)
    # 2) JSON（可覆盖 Java 同名键）
    json_vars = load_tuning_json(args.tuning)
    if json_vars:
        tuning_map.update({k: v for k, v in json_vars.items()})

    files_scanned = 0
    files_changed = 0
    total_val_updates = 0
    total_ph_updates = 0

    render_flag = bool(args.render_placeholders)
    tuning_for_render = tuning_map if render_flag else None

    for group, doc_path in doc_entries:
        files_scanned += 1
        changed, vu, pu = process_doc_file(
            doc_path,
            group,
            tuning_for_render,
            args.apply,
            render_flag,
        )
        if changed:
            files_changed += 1
            total_val_updates += vu
            total_ph_updates += pu

    mode = "write" if args.apply else "dry-run"
    if args.render_placeholders:
        mode += "+render"
    print(
        f"[done:{mode}] 扫描 {files_scanned} 个文件，变更 {files_changed} 个；数值更新 {total_val_updates} 处，占位符替换 {total_ph_updates} 处。"
    )


if __name__ == "__main__":
    main()
