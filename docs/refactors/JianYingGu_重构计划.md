# 剑影蛊（JianYingGu）重构计划

本文档给出“剑影蛊”行为的分层重构与数值调整方案，目标是：对齐统一架构、提升鲁棒性，并在平衡可控前提下实现“冷却调长、几率调大、伤害调高”。

—— 适用版本：NeoForge 1.21.1；ChestCavityForge 现行主线

---

## 背景与现状
- 行为入口：`compat/guzhenren/item/jian_dao/behavior/organ/JianYingGuOrganBehavior.java`
  - 已采用 enum 单例 + static 注册到 `OrganActivationListeners`，符合“Attack Ability 注册规范”。
  - 被动：近战命中时抽样触发“影袭”与“残影”；主动：`guzhenren:jian_ying_fenshen` 召唤短时分身。
  - 依赖：`MultiCooldown`（冷却）、`ResourceOps`/`LedgerOps`（消耗与增伤）、`JianYingTuning`/`JianYingCalculator`（参数与纯计算）、`ShadowService`（分身与皮肤/投射体）、`ActivationHookRegistry`（已有剑道家族快照但未覆盖本技能）。
- 已知改进点
  - 常量重复：`PASSIVE_TRIGGER_CHANCE`、`AFTERIMAGE_CHANCE/AFTERIMAGE_RADIUS` 在行为内硬编码，与 `JianYingTuning` 存在重复/分散。
  - “参数快照”未覆盖 `jian_ying_fenshen` 主动技能，存在激活帧读取动态字段的潜在竞态。
  - 残影调度使用行为内 List + `tickLevel` 分发，可统一接入通用延时调度器以提升模块复用性。
  - 日志粒度可控性不足（建议统一走 DEBUG 开关）。

---

## 目标与非目标
- 目标
  - 架构对齐：行为 → 调度/执行（runtime） → 计算（calculator） → 参数（tuning）清晰分层，Minecraft 依赖尽量只留在 runtime 层。
  - 鲁棒性：
    - 严格避免伤害重入（已用 REENTRY_GUARD，补充异常保护与最小化副作用）。
    - 冷却统一 `MultiCooldown` 与滑窗限流并存（被动命中 5s→8s；主动召唤 20s→30s）。
    - 参数快照覆盖主动技能，消除动态查询竞态。
    - 移除/合并重复常量，所有数值集中 `JianYingTuning`，可配置。
  - 数值方向：
    - 几率调大：被动影袭、残影触发概率上调。
    - 伤害调高：基础伤害、分身与残影系数上调。
    - 冷却调长：OnHit 节流与主动分身冷却上调以平衡强度。
- 非目标
  - 不改变技能的视觉/音效风格与队友识别逻辑。
  - 不新增玩家 UI（后续可选项）。

---

## 分层设计（落地结构）
- 行为层（Behavior）
  - 仅承载事件钩子与入参校验，委派给 runtime 层；保留 enum+static 注册与 `OrganOnHitListener` 接入。
  - 从行为中移除硬编码常量，改为读取 `JianYingTuning`。
- 执行层（Runtime / Executor）
  - `SwordShadowRuntime`（新）：
    - 方法：`attemptPassiveStrike(...)`、`trySpawnAfterimage(...)`、`activateClone(...)`、`commandClones(...)`、`applyTrueLikeDamage(...)`。
    - 依赖 `ShadowService`、`ResourceOps`、`LedgerOps`、`MultiCooldown`；不包含数学公式。
  - `AfterimageScheduler`（新，可选复用通用 `DelayedTaskScheduler`）：
    - 负责残影延迟 AoE 的跨 tick 执行；提供 `queueAfterimage(...)`、`tickLevel(...)`。
- 皮肤/影像（Skin）
  - `ShadowService` 扩展：新增 `captureTint(LivingEntity, ReplicaStyle)` 与 `resolveDisplayStack(LivingEntity)`；
    - Player 分支：仍走 `PlayerSkinUtil.capture(...)`，随后叠色。
    - 非玩家分支：无法解析皮肤时回退至史蒂夫默认纹理的“影子”。
- 计算层（Calculator）
  - 保持 `JianYingCalculator`：倍率衰减、分身/残影伤害计算。
- 参数层（Tuning）
  - 保持 `JianYingTuning`；新增/迁移被动/残影概率与半径常量、被动触发概率等；全部支持 `BehaviorConfigAccess` 覆盖。
- 快照层（ActivationHook）
  - 在 `ActivationHookRegistry` 为 `guzhenren:jian_ying_fenshen` 注册 `ResourceFieldSnapshotEffect("jiandao:", ["daohen_jiandao", "liupai_jiandao"])`，行为层在激活帧优先读取快照。

---

## 数值调整（默认值，可配置）
- 几率相关
  - 被动影袭触发：`PASSIVE_TRIGGER_CHANCE` 0.10 → 0.20（迁移至 `JianYingTuning`）。
  - 残影概率：`AFTERIMAGE_CHANCE` 0.10 → 0.20（以 `JianYingTuning` 为准；行为内去重）。
- 伤害相关
  - 基础伤害：`BASE_DAMAGE` 150.0 → 230.0。
  - 分身伤害系数：`CLONE_DAMAGE_RATIO` 0.25 → 0.30（单体定向增强）。
  - 残影伤害系数：`AFTERIMAGE_DAMAGE_RATIO` 0.20 → 0.25（小范围 AoE 适度增强）。
- 冷却/节流
  - OnHit 冷却：`ON_HIT_COOLDOWN_TICKS` 100 → 160（5s→8s）。
  - 分身冷却：`CLONE_COOLDOWN_TICKS` 400 → 600（20s→30s）。
- 其余保持：分身时长/残影半径等先不调整；如需再平衡，通过配置覆盖。

---

## 非玩家兼容（新增）
- 触发链路：短期内仍仅对玩家开放主动“分身”；被动“影袭/残影”在后续评估是否开放给拥有该器官的非玩家实体。
- 视觉与皮肤：
  - 使用 `ShadowService.captureTint(LivingEntity, style)` 获取“影子”皮肤；
  - 非玩家无法解析 Mojang 皮肤时，统一回退为史蒂夫皮肤 + 对应色调；
  - 锚点与朝向逻辑已基于 `LivingEntity`（`swordAnchor/swordTip`），对非玩家无差异。


---

## 代码改动清单（按序实施）
1) 参数集中与去重
   - 在 `JianYingTuning` 新增：`PASSIVE_TRIGGER_CHANCE`、`AFTERIMAGE_CHANCE`、`AFTERIMAGE_RADIUS`，并接入 `BehaviorConfigAccess`。
   - 在 `JianYingGuOrganBehavior` 移除重复常量，改为引用 `JianYingTuning`；删除行为层对概率/半径的硬编码。
2) 执行层抽离
   - 新建 `compat/guzhenren/item/jian_dao/runtime/SwordShadowRuntime.java`；迁移“触发/生成/下令/施伤”执行路径。
   - 新建 `compat/guzhenren/item/jian_dao/runtime/AfterimageScheduler.java`；迁移 `AFTERIMAGES` 任务队列与 `tickLevel` 分发。
3) 激活前参数快照
   - 在 `ActivationHookRegistry.register()` 为 `guzhenren:jian_ying_fenshen` 注册 `ResourceFieldSnapshotEffect("jiandao:", ["daohen_jiandao", "liupai_jiandao"])`。
   - 在行为激活路径读取快照元数据（若缺失再回退实时读取），用于效率/冷却提示计算。
4) 冷却与滑窗
   - 保持 `MultiCooldown` 统一记录就绪时间；OnHit/Active 使用独立 Key（已有：`OnHitReadyAt` / `ActiveReadyAt`）。
   - 维持分身“滑动窗口限并发”（`COOLDOWN_HISTORY`）：窗口=分身冷却，容量=器官数，先到先服务。
5) 日志与调试开关
   - 统一通过 `LOGGER.debug` 输出关键事件，受 `DEBUG` 总开关控制；Warn 仅用于错误态与速率限制拒绝。
6) 文档
   - 本文档随改动更新；在 `docs/TESTING_SUMMARY.md` 增加“剑影蛊”验证条目。

---

## 鲁棒性检查清单
- 伤害应用
  - REENTRY_GUARD 全路径覆盖；异常捕获后回退为最小副作用（不残留 invulnerableTime/hurtTime）。
  - “准真实伤害”阶段：先 `hurt`，后手动校正吸收与生命上限差值，避免倍增。
- 状态与持久化
  - `SwordShadowState` 存储位置：器官 `OrganState` 或玩家映射；迁移时保留默认值，容忍缺失。
  - `MultiCooldown` 与状态栈同步（`withSync(cc, stack)`），装备/卸下不遗留冷却。
- 资源一致性
  - `ResourceOps.tryAdjustJingli` 与 `tryConsumeTieredZhenyuan` 均走可回滚路径；失败时回补。
- 并发/生命周期
  - 残影调度器逐刻执行，跨维度过滤；世界卸载时清理。

---

## 测试计划（JUnit 5 + Mockito，纯逻辑优先）
- 计算与参数
  - `passiveMultiplier`：窗口外重置；窗口内按 `DECAY_STEP` 衰减；下界裁剪。
  - 伤害值：`cloneDamage` / `afterimageDamage` 在给定效率下与期望相等。
- 节流与冷却（纯函数化）
  - OnHit 冷却：`isReady(now)` 在不同 `readyAt` 下行为正确；多次触发后 `readyAt=now+CD`。
  - 滑窗限并发：队列容量=器官数，超出时拒绝；窗口期满后允许新入队。
- 快照读取
  - 有快照时读取快照；无快照回退读取实时；异常时走安全默认。
- 注意：避免 Mockito mock Minecraft 核心类；仅对纯函数/轻量数据做单测。
- 运行：`./gradlew test`（框架已配置）。

---

## 验证步骤（手测）
- 构建：`./gradlew compileJava`；进入游戏 `./gradlew runClient`。
- 场景
  - 近战连续命中：观察被动影袭/残影触发率显著提升；OnHit 冷却拉长后触发节奏更稳。
  - 主动召唤：分身输出更高但冷却拉长；资源不足时明确信号，无负冷却。
  - 账本与冷却：装备/卸下不残留，重进世界不负时间戳。
  - 反应系统：命中后叠加 `reaction/sword_scar` 正常。

---

## 时间线与拆解
- 第 1 天：参数集中与去重；行为改读 Tuning；编译通过。
- 第 2 天：抽出 Runtime/Scheduler；迁移行为逻辑；编译+基本手测。
- 第 3 天：快照接入与回退路径；完善日志；写核心单测。
- 第 4 天：数值微调回归；在实验世界强度对比；补文档。

---

## 风险与回退
- 风险
  - 几率与伤害双上调可能导致前期强度陡升；通过拉长冷却与可配置项对冲。
  - 残影 AoE 在群怪场景的边际增益放大；必要时下调 `AFTERIMAGE_RADIUS` 或系数。
- 回退
  - 快速回退路径：仅还原 `JianYingTuning` 数值（无需回滚结构）；或临时下调概率/伤害并缩短冷却。

---

责任人：ChestCavityForge 开发团队
最后更新：2025-11-07
