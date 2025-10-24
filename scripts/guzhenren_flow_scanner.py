#!/usr/bin/env python3
"""扫描古真人反编译物品 tooltip，提取“流派”标识。"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

COLOR_CODE_PATTERN = re.compile(r"§[0-9a-fk-or]", re.IGNORECASE)
FLOW_LINE_PATTERN = re.compile(
    r'Component\.(?:literal|translatable)\(\s*"([^"]*流派[^"]*)"\s*\)'
)


@dataclass
class FlowEntry:
    class_name: str
    relative_path: Path
    line_no: int
    flow_text: str


def default_decompile_root(repo_root: Path) -> Path:
    return repo_root.parent / "decompile" / "10_6_decompile" / "net" / "guzhenren" / "item"


def extract_flow_text(raw: str) -> str | None:
    clean = COLOR_CODE_PATTERN.sub("", raw)
    clean = clean.replace("\\n", " ").replace("\\t", " ").strip()
    if "流派" not in clean:
        return None

    # 获取“流派”后内容，允许存在全角/半角冒号
    match = re.search(r"流派[:：]\s*(.+)", clean)
    if match:
        return match.group(1).strip()
    return clean


def scan_file(path: Path, display_root: Path) -> list[FlowEntry]:
    entries: list[FlowEntry] = []
    class_name = path.stem
    with path.open(encoding="utf-8") as fp:
        for idx, line in enumerate(fp, start=1):
            if "流派" not in line:
                continue
            match = FLOW_LINE_PATTERN.search(line)
            text = None
            if match:
                text = match.group(1)
            else:
                literal_match = re.search(r'"([^"]*流派[^"]*)"', line)
                if literal_match:
                    text = literal_match.group(1)

            if text is None:
                continue

            flow = extract_flow_text(text)
            if flow:
                entries.append(
                    FlowEntry(
                        class_name=class_name,
                        relative_path=path.relative_to(display_root),
                        line_no=idx,
                        flow_text=flow,
                    )
                )
    return entries


def scan_directory(root: Path) -> list[FlowEntry]:
    entries: list[FlowEntry] = []
    for file in sorted(root.glob("*.java")):
        entries.extend(scan_file(file, root))
    return sorted(entries, key=lambda e: (e.flow_text, e.class_name))


def emit_table(entries: list[FlowEntry]) -> None:
    print("| 流派 | Item 类 | 文件 | 行号 |")
    print("| --- | --- | --- | --- |")
    for entry in entries:
        rel_path = entry.relative_path
        print(
            f"| {entry.flow_text} | {entry.class_name} | {rel_path} | {entry.line_no} |"
        )


def emit_plain(entries: list[FlowEntry]) -> None:
    current_flow = None
    for entry in entries:
        if entry.flow_text != current_flow:
            current_flow = entry.flow_text
            print(f"\n[{current_flow}]")
        print(f"  - {entry.class_name} ({entry.relative_path}:{entry.line_no})")


def emit_json(entries: list[FlowEntry]) -> None:
    grouped: dict[str, list[dict[str, object]]] = {}
    for entry in entries:
        grouped.setdefault(entry.flow_text, []).append(
            {
                "class": entry.class_name,
                "file": str(entry.relative_path),
                "line": entry.line_no,
            }
        )
    json.dump(grouped, sys.stdout, ensure_ascii=False, indent=2)
    print()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="扫描古真人反编译物品 tooltip，提取“流派”标识。"
    )
    parser.add_argument(
        "--decompile-root",
        type=Path,
        help="指定反编译 item 目录，默认为 ../decompile/10_6_decompile/net/guzhenren/item",
    )
    parser.add_argument(
        "--format",
        choices=("table", "plain", "json"),
        default="table",
        help="输出格式（table/plain/json），默认 table。",
    )
    return parser.parse_args()


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    args = parse_args()
    root = (args.decompile_root or default_decompile_root(repo_root)).resolve()

    if not root.exists():
        print(f"未找到目录：{root}", file=sys.stderr)
        return 1

    entries = scan_directory(root)
    if not entries:
        print("未找到包含“流派”信息的 tooltip。", file=sys.stderr)
        return 1

    if args.format == "table":
        emit_table(entries)
    elif args.format == "plain":
        emit_plain(entries)
    else:
        emit_json(entries)
    return 0


if __name__ == "__main__":
    sys.exit(main())
