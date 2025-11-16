# AGENTS.md — 事件系统（注册与同步）

作用域：`src/main/java/kizuna/guzhenren_event_ext/**`

## 注册清单来源（单一真相）
- 触发器（Trigger）、条件（Condition）、动作（Action）的注册位于：
  - `src/main/java/kizuna/guzhenren_event_ext/GuzhenrenEventExtension.java:registerTriggersAndActions()`
- 事件 JSON 的构建期校验任务会校验 `type` 是否在“允许集合”中。

## 同步脚本（必须）
- 为避免“代码注册”与“构建期校验白名单”不一致，提供同步脚本：
  - `scripts/sync_event_validation_types.py`：解析 `GuzhenrenEventExtension.java` 中的 `register("type", new ...)` 调用，生成 `scripts/event_types.json`。
  - 运行方式：
    - `./gradlew syncEventValidationTypes`（Gradle 任务封装）
    - 或 `python3 scripts/sync_event_validation_types.py`
- `validateGuzhenrenEvents` 任务会优先读取 `scripts/event_types.json`，若不存在则退回到构建脚本内置默认集合。

## 变更流程要求
- 新增/删除 Trigger/Condition/Action 注册后：
  1) 运行 `./gradlew syncEventValidationTypes` 同步类型表；
  2) 运行 `./gradlew validateGuzhenrenEvents` 验证所有事件 JSON；
  3) 提交代码时同时提交 `scripts/event_types.json`（保证 CI 可重复）。

## 原则
- KISS：注册集中于单一方法；同步脚本无第三方依赖，仅用正则解析。
- DRY：构建校验直接消费 `event_types.json`，避免在多个位置手工维护列表。
- 单一职责：脚本只负责“提取写文件”，校验只负责“读取校验”。

