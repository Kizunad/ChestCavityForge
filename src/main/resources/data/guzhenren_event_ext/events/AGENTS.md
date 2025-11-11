name: event-def-keys
description: Guzhenren Event Extension 事件定义可配置键速查表

# 事件定义可配置键（速查）

本文件列出 `data/guzhenren_event_ext/events/*.json` 中可使用的所有键，按层级与模块分类，便于编写与校验事件。

> **规范提醒**：所有事件配置文件必须以 `[]` 包裹事件对象（即 1 个或多个 event 对象的数组）。每个 `trigger`/`condition`/`action` 中必须使用 `"type"` 字段标识其类型，老式的 `"id"` 映射已在加载器内兼容但建议避免。

## 顶层结构（每个事件对象）

- `id` 字符串，必填。事件唯一ID（形如 `guzhenren_event_ext:example/low_zhenyuan_warning`）。
- `enabled` 布尔，默认 `true`。是否启用该事件。
- `trigger_once` 布尔，默认 `false`。是否仅触发一次（按玩家维度，内部状态由附件维护）。
- `trigger` 对象，必填。触发器定义（见下文“Triggers”）。
- `conditions` 数组，可选。条件列表，全部通过才会执行（见下文“Conditions”）。
- `actions` 数组，必填。动作列表，依序执行（见下文“Actions”）。

---

## Triggers（触发器）

1) `guzhenren_event_ext:player_obtained_item`
- `item` 字符串，必填。物品ID（如 `minecraft:dirt` / `guzhenren:shou_gu`）。
- `min_count` 数字，可选。最小获得数量，默认不限制。

2) `guzhenren_event_ext:player_stat_change`
- `stat` 字符串，必填。被监控的蛊真人资源字段标识（与 `GuzhenrenResourceBridge` 一致，如 `zhenyuan`、`health` 等）。
- `from` 对象，可选。旧值约束：
  - `min` 数字，最小值；`max` 数字，最大值。
  - `min_percent` 数字，最小百分比；`max_percent` 数字，最大百分比（按 `{stat}_max` 计算）。
- `to` 对象，可选。新值约束：同 `from` 字段。

说明：`player_stat_change` 会自动驱动 `PlayerStatWatcher` 监听对应字段。

---

## Conditions（条件）

1) `minecraft:random_chance`
- `chance` 数字，0.0~1.0，默认 `1.0`（必定通过）。

2) `guzhenren:player_health_percent`
- `min` 数字，可选。最小生命百分比（0.0~1.0）。
- `max` 数字，可选。最大生命百分比（0.0~1.0）。

3) `guzhenren:player_daode`
- `min` 数字，可选。道德下限。
- `max` 数字，可选。道德上限。

4) `guzhenren:player_daohen`
- `field` 字符串，必填。道痕字段标识（支持 `daohen_xxx`、中文文档名如“水道道痕”，或 PlayerField 枚举名）。
- `min` 数字，可选。道痕下限。
- `max` 数字，可选。道痕上限。

5) `guzhenren_event_ext:check_variable`
- `scope` 字符串，必填。`"player"` 或 `"world"`。
- `variable` 字符串，必填。变量键（自定义命名空间，推荐 `.` 分层，如 `quest.main_story_chapter`）。
- `condition` 字符串，必填。`"==" | "!=" | ">" | ">=" | "<" | "<=" | "exists" | "not_exists"`。
- `value` 任意，可选。与 `exists/not_exists` 配合时可省略；其余比较需提供（支持 boolean/int/double/string）。

---

## Actions（动作）

1) `guzhenren_event_ext:send_message`
- `message` 字符串，必填。发送给玩家的文本（支持颜色符号）。

2) `guzhenren_event_ext:run_command`
- `command` 字符串，必填。执行的命令（支持 `{player}` 占位符替换为玩家名；`@s/@p` 指向触发者）。
- `as_player` 布尔，可选，默认 `false`。是否以玩家权限执行（否则以服务端、权限等级2执行）。

3) `guzhenren_event_ext:adjust_player_stat`
- `stat` 字符串，必填。调整的蛊真人资源字段标识。
- `operation` 字符串，可选，默认 `"add"`。`"add" | "subtract" | "multiply" | "set"`。
- `value` 数字，必填。调整值。

4) `guzhenren_event_ext:set_variable`
- `scope` 字符串，必填。`"player"` 或 `"world"`。
- `variable` 字符串，必填。变量键。
- `value` 任意，必填。赋值（支持 boolean/int/double/string）。

5) `guzhenren_event_ext:remove_variable`
- `scope` 字符串，必填。`"player"` 或 `"world"`。
- `variable` 字符串，必填。变量键。

6) `guzhenren_event_ext:spawn_hostile_gucultivator`
- `count` 数字，必填。生成数量（>=1）。
- `min_distance` 数字，可选，默认 `5.0`。生成距离下限。
- `max_distance` 数字，可选，默认 `10.0`。生成距离上限（不小于 `min_distance`）。
- `health_multiplier` 数字，可选，默认 `1.0`。生命倍数。
- `damage_multiplier` 数字，可选，默认 `1.0`。攻击倍数。
- `speed_multiplier` 数字，可选，默认 `1.0`。移速倍数。
- `entity_id` 字符串，可选。指定生成的实体ID（若未提供则从内置蛊修列表随机）。
- `custom_name` 字符串，可选。实体自定义名称（显示）。
- `notification_message` 字符串，可选。生成成功后发给玩家的提示；支持 `{count}` 占位符。
- `dialogue_message` 字符串，可选。模拟由该实体“对玩家说”的台词（以系统消息发送，前缀为实体名）。
- `promote_to_same_realm` 布尔，可选，默认 `false`。若为 `true`：
  - 镜像玩家基础属性：生命上限（含回满）、攻击力、护甲、护甲韧性、移动速度。
  - 读取玩家 `转数`（1..5，四舍五入），从标签 `guzhenren:yewaigushiguchong{tier}` 随机抽取6个蛊虫并装备至：头、胸、腿、脚、主手、副手。

---

## 其他说明

- 触发器与条件、动作均为“并联组合”：
  - 多个条件需要全部通过。
  - 动作将按顺序逐个执行。
- Watcher 激活：
  - 出现 `player_stat_change` 会激活对应字段的 `PlayerStatWatcher`（轮询间隔见配置）。
  - 出现 `player_obtained_item` 会激活 `PlayerInventoryWatcher`。
- 运行时资源：
  - 蛊虫标签：`guzhenren:yewaigushiguchong1..5`（五阶可能引用四阶池）。
  - 玩家资源读取：通过 `GuzhenrenResourceBridge` 反射桥接（避免直接依赖目标模组API）。
