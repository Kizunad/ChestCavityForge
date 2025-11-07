# 飞剑命令与 TUI 重构计划（NeoForge 1.21.1）

状态：草案 v1（可执行方案）  
范围：`compat/guzhenren/flyingsword` 子系统（命令、TUI、会话与存储交互）

## 背景与问题

- 聊天 TUI 不会刷新缓冲，旧页面长期留存，用户可能点击“过期按钮”。
- 现有 `*_index` 命令依赖动态列表，列表变化后易越界或指向错误目标。
- 频繁打开/翻页会刷屏；导航位置不固定，可读性一般。
- 权限诉求：`/flyingsword ui` 面向普通玩家，`spawn/test` 仅管理员（已完成）。

## 目标

- 稳健交互：TUI 按钮绑定稳定 ID（UUID/ItemUUID），旧消息点击要么安全失败并提示刷新，要么仍能正确定位。
- 防刷屏：为 TUI 引入“会话/令牌 + TTL + 限流”，控制输出行数与刷新频率。
- 统一样式：标题/分区/按钮规范一致，导航固定在底部，信息密度适中，整体更美观。
- 向后兼容：保留 index 命令；TUI 优先使用 ID 命令。

非目标：

- 不改变飞剑 AI/战斗/伤害/修复等业务逻辑；仅改命令与 TUI 交互层。

## 方案概述

1) 稳定定位（ID 命令）

- 新增面向 TUI 的 ID 子命令（实体 UUID / 存储 ItemUUID）：
  - 实体侧：`recall_id <uuid>`、`mode_id <uuid> <mode>`、`repair_id <uuid>`、`select_id <uuid>`、`group_id <uuid> <group>`。
  - 存储侧：`restore_item <itemUuid>`、`withdraw_item <itemUuid>`、`deposit_item <itemUuid>`。
- TUI 按钮全部改用 ID 版本，杜绝 index 偏移导致的误操作。
- 现有 `*_index` 命令保留，兼容手动输入与过渡期使用。

2) 会话令牌与过期（sid + TTL + 限流）

- 为每名玩家维护 TUI 会话：`sid`（随机短 ID）、`expiresAt`、`lastTuiSentAt`。
- 打开/翻页生成或刷新 `sid`，所有按钮命令追加 `sid` 参数。
- 命令处理侧校验 `sid` 与会话一致且未过期；失败返回统一“界面已过期，请[刷新]”提示。
- TTL 建议：60s；最小刷新间隔：1s（可配置）。

3) TUI 输出规范

- 结构：标题 → 行为条 → 全体操作 → 交互条 → 当前选中 → 列表/分页 → 导航（底部固定）。
- 列表单页条目数：6（可调）；总行数可控，避免刷屏。
- 样式：保留模式药丸；统一按钮颜色/下划线；减少冗长文本，改“标签:值”。

4) 友好失败与刷新

- 当 UUID 不存在或条目被移除：提示“目标不存在/已变化”，附“[刷新]”。
- 旧消息点击且 `sid` 过期：提示“此界面已过期，请[刷新]”。

5) 向后兼容与演进

- Index 命令继续支持；TUI 默认使用 ID 命令。
- 可选为 index 按钮追加 `snap`（列表快照哈希）做双保险；后续可逐步弃用 index。

## 权限与可见性

- 普通玩家可见/可用：`ui / ui_active / ui_storage / mode / recall / list / status / clear / select / group_* / repair_* / storage`（TUI 使用 ID 命令）。
- 管理员专用：`spawn`、`test`（已实现 `hasPermission(2)`）。可选将 `debug` 也限定管理员。

## 代码改动一览（模块）

- 命令注册：`FlyingSwordCommand`
  - 新增上述 ID/ItemUUID 子命令；所有 ID 命令接受可选 `sid` 参数。
  - 校验 `sid`；失败返回统一过期提示组件。
- 会话状态：`SwordCommandCenter.CommandSession`
  - 新增 `tuiSessionId`、`tuiSessionExpiresAt` 字段；沿用 `lastTuiSentAt` 限流。
- 新工具：
  - `TUISessionManager`：生成/刷新 `sid`、TTL 与限流判断；`currentSid(player)`、`ensureFreshSession(player, now)`。
  - `TUICommandGuard`：从命令参数读取并校验 `sid`，生成“已过期，请[刷新]”提示。
- TUI 构建：`FlyingSwordTUI`
  - 按钮命令改为 ID 版并追加 `sid`；分页与导航固定；文本精简与样式统一。
- 存储映射：`FlyingSwordStorage`
  - 已有 `displayItemUUID`；命令按此定位存储项，避免 index 偏移。

## 实施步骤（里程碑）

1. 新增 ID 子命令与存储 ItemUUID 命令（仅命令层，不改 TUI）。
2. 扩展会话：`sid` + TTL + 限流；提供 Guard。
3. TUI 全量切换至 ID 命令；追加 `sid`；导航与分页规范化。
4. 视觉与文案微调（药丸、颜色、标签化字段）。
5. 可选：为 `*_index` 增加 `snap` 快照校验（防误点双保险）。
6. 单元测试（纯逻辑）：`sid/TTL/限流`、`UUID/ItemUUID 解析`、`分页与文本构建`。
7. 文档与使用指南更新；提交变更说明与配置项说明。

## 时间预估

- 步骤 1：0.5 天（命令注册 + 解析）
- 步骤 2：0.5 天（会话管理 + 校验）
- 步骤 3：0.5 天（TUI 切换与参数注入）
- 步骤 4：0.5 天（UI 微调）
- 步骤 5：0.5 天（可选 snap）
- 步骤 6：0.5 天（测试）
- 步骤 7：0.25 天（文档）

## 验收标准

- 旧 TUI 消息点击不会执行错误操作：要么正确定位，要么过期提示 + 刷新入口。
- 刷屏显著减少：重复打开/翻页受限流控制，总行数可控。
- 权限正确：普通玩家无法执行 `spawn/test`；TUI 功能完整可用。
- 编译与测试通过：`./gradlew compileJava` 与 `./gradlew test` 绿灯。
- 手动验证：
  - 过期/刷新流程自然顺滑；
  - 存储项按 ItemUUID 可被准确“召唤/取出/放回”；
 - 列表变化时，旧按钮不会误操作其他条目。

## 聊天 TUI 边框与 Emoji 样式（仅规划）

目标
- 借助 Modern UI 的聊天 emoji 支持，优化 TUI 的 banner/分隔线/模式药丸显示，提升可读性与辨识度。
- 保持向下兼容：客户端字体不完整或禁用时降级为 ASCII 样式；不影响功能逻辑。
- 避免刷屏：边框长度与装饰字符适度、分页与导航一行内呈现。

设计要点
- FANCY 模式开关：`tui.fancyEmoji`（默认开启），可热切换；禁用时使用 ASCII 方案。
- 主题常量（不立即实现，仅规划）：
  - 颜色：`ACCENT/BUTTON/DIM` 延用现有主题；
  - Emoji：`EMOJI_SPARK=✦`、`HUNT=⚔`、`GUARD=🛡`、`ORBIT=🌀`、`HOVER=⏸`、`RECALL=🔁`、`SWARM=🌿`。
- Banner/分隔：
  - FANCY 顶部：`╭ ✦ <标题> ✦ ╮`；底部：`╰────────────────────╯`（长度适中，避免过宽换行）。
  - ASCII 回退：顶部 `===== <标题> =====`；分隔线 `——————————————`。
- 模式药丸：
  - FANCY：以 emoji + 文本（如 `⚔ 出击`、`🛡 守护`）显示，颜色沿用模式色；
  - ASCII：保留`[出击]/[守护]/[环绕]/[悬浮]/[召回]/[集群]`。
- 导航行：固定显示在底部，使用 `◀ 上一页 | 返回 | 下一页 ▶`（或 ASCII：[上一页]/[返回]/[下一页]）。
- 文案精简：将“耐久/距离/等级/组”统一为“标签:值”形式，缩短行宽，避免折行。

兼容与回退
- 若客户端环境检测到 emoji 渲染不可用或用户关闭 FANCY：统一使用 ASCII 渲染；
- 所有按钮与命令行为不变，纯视觉增强；
- 结合 `sid` 过期提示时，提供“[刷新]”按钮，避免用户因旧边框误认为可交互。

实施步骤（并入主计划）
1. 增补主题与 emoji 映射（常量/配置项定义）；
2. 定义 banner/hr 渲染策略（FANCY/ASCII 两套）；
3. 模式药丸支持 emoji 标签与回退标签；
4. 导航行统一样式与长度控制；
5. 客户端可选配置项与默认值说明；
6. 视觉验收（不同字体与窗口宽度）与截图记录。

风险与缓解
- 字体/宽度差异导致换行：控制边框字符数量，使用短 emoji，分页条保持单行；
- 某些终端字体缺字：提供 ASCII 回退；
- 误导交互：旧消息点击仍通过 `sid` 校验与过期提示兜底。

## 风险与缓解

- UUID 不可得/冲突：ItemUUID 由 `ItemIdentityUtil` 生成并持久化，冲突概率极低；缺失时提示刷新。
- 服务器重启后 `sid` 失效：TTL 设计使然，提示刷新即可。
- 聊天长度超限：限制单页条目数与信息密度，分页控量。
- 兼容期混用 index/ID：保留 index 命令并可选启用 snap，避免硬切带来回归。

## 回滚方案

- 保留现有 index 命令不移除；TUI 使用 ID 命令的改动可通过一次提交撤回。
- 若出现严重回归，恢复到仅限“会话+限流”的轻度版本（无需 ID 命令）。

## 配置建议

- `tui.session.ttlSeconds = 60`
- `tui.session.minRefreshMillis = 1000`
- `tui.page.size = 6`
- `commands.debug.adminOnly = true`（可选）

---

维护：ChestCavityForge 开发团队  
最后更新：2025-11-07
