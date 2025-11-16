# AGENTS.md — Guzhenren Event JSON 规范

作用域：本文件所在目录及其子目录（`src/main/resources/data/guzhenren_event_ext/events/**`）。

## 必须遵循的要点

- 所有事件 JSON 必须通过构建期校验：`./gradlew validateGuzhenrenEvents`。
- 校验已挂载至 `compileJava`、`check`、`build` 阶段；一旦校验失败将阻断编译与构建。
- 支持两种根结构：
  - 数组：`[ { ...event1 }, { ...event2 } ]`
  - 单对象：`{ ...event }`
- 必填字段：
  - `id`：字符串，建议以 `guzhenren_event_ext:` 命名空间开头，需全仓唯一。
  - `trigger`：对象，且须包含 `type`（或兼容的 `id` 键）。
- 可选字段：
  - `enabled`：布尔，默认 `true`。
  - `trigger_once`：布尔，默认 `false`。
  - `conditions`：数组（每项为对象，需有 `type`/`id`）。
  - `actions`：数组（每项为对象，需有 `type`/`id`）。
- 现有已注册类型（以构建脚本为准）：
  - 触发器：`guzhenren_event_ext:player_obtained_item`、`guzhenren_event_ext:player_stat_change`、`guzhenren_event_ext:special_entity_killed`
  - 条件：`minecraft:random_chance`、`guzhenren:player_health_percent`、`guzhenren:player_daode`、`guzhenren:player_daohen`、`guzhenren_event_ext:check_variable`、`guzhenren_event_ext:check_entity_tag`
  - 动作：`guzhenren_event_ext:send_message`、`guzhenren_event_ext:run_command`、`guzhenren_event_ext:adjust_player_stat`、`guzhenren_event_ext:spawn_hostile_gucultivator`、`guzhenren_event_ext:set_variable`、`guzhenren_event_ext:remove_variable`、`guzhenren_event_ext:give_item`

## 与代码同步（重要）

- 当你在 `GuzhenrenEventExtension.java` 中新增/删除触发器、条件、动作的注册时，必须同步更新构建期校验所使用的“允许类型列表”。
- 推荐做法：运行 `./gradlew syncEventValidationTypes` 或执行 `scripts/sync_event_validation_types.py`，将注册类型写入 `scripts/event_types.json`，构建校验会优先读取该文件。
- 如果未生成该 JSON，则校验任务会退回到构建脚本内置的默认集合（可能滞后）。

## 额外建议

- `player_stat_change`：建议提供 `from` 或 `to` 区段以限定范围；`stat` 必填。
- 对于概率条件 `minecraft:random_chance`，`chance` 取值须在 `[0,1]`。
- 若仅用于本地调试，可临时移除 `trigger_once` 或提高 `chance`，上线前务必恢复。
