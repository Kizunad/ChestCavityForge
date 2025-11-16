#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
从 GuzhenrenEventExtension.java 中解析已注册的 Trigger/Condition/Action 类型，生成 scripts/event_types.json。
规则：匹配 register("<type>", new <Class>(...)); 并根据前文的 Registry 变量名判定类别。
"""

import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_FILE = ROOT / "src/main/java/kizuna/guzhenren_event_ext/GuzhenrenEventExtension.java"
OUT_FILE = ROOT / "scripts/event_types.json"

SRC_ENCODING = "utf-8"


def parse_registered_types(text: str):
    # 在整个文件范围内匹配 3 类 Registry 的 register 调用
    # 形式：TriggerRegistry.getInstance().register("type", new Xxx(...))
    patt = re.compile(
        r"(TriggerRegistry|ConditionRegistry|ActionRegistry)\.getInstance\(\)\.register\(\s*\"([^\"]+)\"\s*,",
        re.M,
    )

    result = {"triggers": [], "conditions": [], "actions": []}

    for m in patt.finditer(text):
        kind = m.group(1)
        type_id = m.group(2)
        if kind == "TriggerRegistry":
            result["triggers"].append(type_id)
        elif kind == "ConditionRegistry":
            result["conditions"].append(type_id)
        elif kind == "ActionRegistry":
            result["actions"].append(type_id)

    for k in result:
        result[k] = sorted(set(result[k]))
    return result


def main():
    if not JAVA_FILE.exists():
        raise SystemExit(f"Java file not found: {JAVA_FILE}")

    text = JAVA_FILE.read_text(encoding=SRC_ENCODING)
    data = parse_registered_types(text)

    OUT_FILE.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUT_FILE} with: triggers={len(data['triggers'])}, conditions={len(data['conditions'])}, actions={len(data['actions'])}")


if __name__ == "__main__":
    main()
