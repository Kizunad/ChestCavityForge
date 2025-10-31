# ChestCavityForge 古真人模块迁移方案（NeoForge 1.21.1）

> 目标：在保持原有玩法与数值体验一致的前提下，完成对资源/账本/冷却/护盾与激活链路的统一重构；将复杂逻辑抽离为可测试的纯函数；确保 DoT 反应系统与灵魂体系在迁移后行为一致且稳定。

---

## 范围与优先级
- 代码范围：`src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/**`
- 首批优先桶：雷/食（其余家族随后跟进）
- 必遵守规范：
  - DoT Reaction：`DoTTypes` + `ReactionRegistry` + `ReactionStatuses`，仅使用带 `typeId` 的 `DoTEngine.schedulePerSecond(...)` 重载
  - 统一助手：`LedgerOps`、`ResourceOps`、`MultiCooldown`、`AbsorptionHelper`、`SteelBoneComboHelper`
  - 行为注册：以 `ActiveSkillRegistry` 为主入口，`OrganActivationListeners` 仅作后端桥接（准备移除）
  - 参数快照：对动态参数技能使用 `ActivationHookRegistry` 注册 `ResourceFieldSnapshotEffect`
  - 测试策略：JUnit 5 测纯逻辑，避免直接依赖 Minecraft 核心类

---

## 结构模式（参考 bing_xue_dao 与 combo）
- 行为分层：
  - behavior：编排流程（尽量无重状态/分支）
  - runtime：逐 tick/帧调度、与世界交互
  - calculator|ops：参数计算/筛选/曲线/概率/资源校验（纯函数，可单测）
  - tuning：数值与阈值配置
  - state|keys：状态键与存取封装
  - fx|messages：表现与提示解耦
- 目录示例：
  - `behavior/active|passive`、`runtime/**`、`calculator/**`、`tuning/**`、`state/**`、`fx/**`、`messages/**`

---

## 激活链路（统一到 ActiveSkillRegistry）
- 权威注册与触发：
  - 在 `ActiveSkillRegistry.bootstrap()` 中使用
    `register(skillId, abilityId, organId, tags, description, sourceHint, () -> ensureClassLoaded(Behavior.INSTANCE), cooldownHint)`
  - 行为类用 `enum` 单例，在 `static{}` 中完成“能力处理器注册”（当前仍注册到 `OrganActivationListeners`，后续可替换为新后端）
- 触发入口统一：
  - 客户端热键与网络包改为调用 `ActiveSkillRegistry.trigger(player, skillId)`
  - `trigger` 内部完成装备校验、Pre/Post Hook、并桥接到能力处理器
- 迁移注意：
  - 客户端热键列表仅使用字面 `ResourceLocation` 的“技能 ID”（非能力 ID），避免引用行为类常量以防 classloading 早触发
  - `ActivationBootstrap` 负责懒加载，避免在监听器维护硬编码列表

---

## DoT Reaction 规范
- 所有 DoT 必须携带 `typeId`（来自 `DoTTypes`），仅用带 `typeId` 的 `DoTEngine.schedulePerSecond(...)`
- 在伤害执行前统一走 `ReactionRegistry.preApplyDoT(...)`；需要取消或联动在此完成
- 新增反应用 `ReactionRegistry.register(...)` 注册，实现保持轻量且可组合
- 默认规则（范例）：火衣光环 × 油涂层 => 爆炸并移除油；窗口为 0，不做连锁屏蔽

---

## 统一助手替换清单
- 资源：`ResourceOps.canPay / tryPay / adjust / set`
- 账本：`LedgerOps.adjust / set / remove`（支持账本重建，移除直连 `LinkageChannel.adjust`/`ledger.remove`）
- 冷却：`MultiCooldown.setIfReady / getRemaining / clearFor`（移除分散时间戳/私有计时）
- 护盾：`AbsorptionHelper.give|refresh|clear`，变更前确保 `SteelBoneComboHelper.ensureAbsorptionCapacity()`

---

## 分阶段迁移步骤
1) 库存盘点（创建跟踪表）
   - 搜索：`LinkageManager.getContext|getOrCreateChannel|adjust|GuzhenrenResourceBridge.open|NBTCharge|ledger.remove`
   - 搜索：`schedulePerSecond(`（检查是否缺失 `typeId`）
   - 搜索：`new .*Cooldown|lastUsed|cooldownMillis`（合并到 `MultiCooldown`）
   - 搜索：`MAX_ABSORPTION|addTransientModifier|removeModifier`（统一到 `AbsorptionHelper`）
   - 搜索：`OrganActivationListeners.register|activate`（改走 `ActiveSkillRegistry`）
2) 桶级重构（雷/食 → 其余）
   - 对齐分层目录，抽出 calculator/ops 纯逻辑
   - 替换资源/账本/冷却/护盾为统一助手
   - DoT 全量类型化 + 反应注册
3) 激活链路统一
   - 行为类改 enum 单例，`static{}` 内只做“处理器注册”
   - `ActiveSkillRegistry.bootstrap()` 完成集中登记（含懒加载）
   - 客户端热键与网络包改用 `ActiveSkillRegistry.trigger`（技能 ID）
4) 参数快照与灵魂系统
   - 对受“道痕/经验”等影响的技能，在 `ActivationHookRegistry` 注册 `ResourceFieldSnapshotEffect`，行为中仅读快照
   - 假人/SoulBeast 的 Action 复用 `MultiCooldown` 与 `DelayedTaskScheduler`，并保持限流与并发安全
5) 测试与验证
   - 为每桶的 calculator/ops 增加 JUnit5 单测（纯函数）
   - `./gradlew compileJava`，`./gradlew test`
   - 实机手测：装备/卸下、区块重载、触发与护盾刷新

---

## 测试策略与样例建议
- 资源扣费：余额恰好/不足/并发扣费
- 冷却窗口：窗口内拒绝/窗口外通过；持久化后仍正确
- AoE 衰减/筛选：同心圆单调性与阈值裁剪
- 概率/权重：稳定性与边界
- 反应取消：`preApplyDoT` 命中取消/替代逻辑
- 残留域限流：合并策略正确（传入简化参数校验）

---

## 验收标准
- 编译与测试：`./gradlew compileJava` 与 `./gradlew test` 通过
- 游戏验证：
  - 装备/卸下不留账本残留，不出现负冷却
  - 护盾修饰符清洁，刷新遵守 `MAX_ABSORPTION`
  - DoT 类型化完整，反应联动按设计生效且不刷屏
 - 热键/UI/网络触发走同一入口（`ActiveSkillRegistry.trigger`）

---

## 数值与冷却统一公式（按道系）
- 目标：为各“x道”提供一致的“增益随道系提升”和“冷却随道系或冷却属性变化”的可配置计算方式，避免魔法数散落。

- 参数来源（优先级从高到低）：
  - 快照字段：通过 `ActivationHookRegistry` 注册的 `ResourceFieldSnapshotEffect`（在技能触发帧固定）
  - 状态/道痕：如 `BingXueDaohenOps`、各 `...StateKeys`、或 `OrganStateOps`
  - 器官评分：相关 organScores（如 `bing_xue_dao` 读取“冰雪道”贡献）

- 增益缩放统一公式：
  - 基础形式：`scaled = base * (1 + coeff * dao)`
  - 取值钳制：`scaled = clamp(scaled, min, max)`（min/max 由 `tuning` 配置；默认不超过 3.0x，且不低于 0）
  - 建议命名：`calculator/CommonScalingOps.scaleByDao(base, dao, coeff, min, max)`

- 冷却计算统一公式：
  - 加法冷却缩减（不可叠爆）：`cooldown = base * (1 - cdr)`，其中 `cdr = clamp(sum(cdrSources), 0, cdrCap)`；常见 `cdrCap` 取 0.4–0.5（由 `tuning` 决定）
  - 道系对冷却影响（可选）：`cdr += daoCdrCoeff * dao`，同样受 `cdrCap` 钳制
  - 冷却下限：`cooldown = max(cooldown, minCooldown)`；`minCooldown` 推荐 ≥ 0.2s
  - 建议命名：`calculator/CommonScalingOps.cooldown(base, cdr, minCooldown, cdrCap)` 与 `withDaoCdr(base, dao, daoCoeff, extraCdr, minCooldown, cdrCap)`

- MultiCooldown 使用规范：
  - 仅通过 `MultiCooldown.setIfReady(owner, key, durationTicks)` 设置；查询用 `getRemaining(owner, key, nowTick)`；移除用 `clearFor(owner, key)`
  - 禁止在行为或 runtime 中保存自定义时间戳；跨区块/重登后应保持一致

- 落地示例（伪代码）：
  - 伤害/范围缩放：
    - `double dmg = scaleByDao(tuning.baseDamage(), snapshot.dao(), tuning.daoCoeff(), tuning.minDmg(), tuning.maxDmg());`
  - 冷却：
    - `double cd = withDaoCdr(tuning.baseCd(), snapshot.dao(), tuning.daoCdrCoeff(), snapshot.extraCdr(), tuning.minCd(), tuning.cdrCap());`
    - `MultiCooldown.setIfReady(cc, ABILITY_ID, ticks(cd));`

- 测试要点：
  - 单调性：dao 增大 ⇒ 增益非减、冷却非增（在阈值前）
  - 钳制：当 dao 或 cdr 过大时命中上限；当 base 很小命中下限
  - 组合：多来源 cdr 相加后仍不越过 `cdrCap`
  - 可重复性：相同快照下，多次触发结果一致

---

## 风险与回滚
- 风险
  - 遗留 `ledger.remove` 或 `LinkageChannel.adjust` 导致账本漂移
  - 冷却重复来源导致负时间戳
  - DoT 未携带 `typeId` 引发运行期异常
- 预防
  - 每个桶完成后执行编译+测试并做一次定向手测（装备/卸下/重登/触发）
- 回滚
  - 以桶为单位提交；若回归出现，记录复现与修复计划并快速回滚上一稳定点

---

## 里程碑
- M1（雷/食）：
  - 统一助手替换完成，DoT 全类型化；关键 calculator/ops 覆盖单测
- M2（其余家族）：
  - FakePlayer/Soul 行为接线完成；残留域限流稳定
- M3（清理优化）：
  - 去除底层 API 残留；补齐反应规则与边界测试

---

## 附：实施提示与对照
- 参考目录：
  - `compat/guzhenren/item/bing_xue_dao/**`（清晰的 runtime/calculator/tuning 拆分与 DoT 接线）
  - `compat/guzhenren/item/combo/**`（复杂招式在 framework + calculator 中的解耦范式）
- 迁移时优先使用现成助手：`LedgerOps`、`MultiCooldown`、`NBTCharge`、`OrganStateOps`、`ResourceOps`
- 日志：INFO 以下默认静音；调试开关保持关闭，必要时临时开启并回收

---

最后更新：2025-10-31
