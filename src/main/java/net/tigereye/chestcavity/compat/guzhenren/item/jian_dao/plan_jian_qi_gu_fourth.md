# 剑气蛊 实现计划（Jian Qi Gu）

> 模块路径：`compat/guzhenren/item/jian_dao`  
> 适配规范：[`AGENTS.md`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/AGENTS.md:1)  
> 本文档遵循 `docs/plan.md` 多阶段方案模板，聚焦“剑气蛊”主动技「一斩开天」、被动「气断山河」，以及非玩家生物（Mob）逻辑与剑光 Fx 的设计与落地。  
> 命名统一使用 JianQiGu，不在类名中强调“4转”，4转限制通过 tuning/条件判断表达。

---

## 1. 能力概要与设计目标

### 1.1 主动：「一斩开天」

- 强力直线剑气斩击（剑光），模拟高速剑气波。
- 特性：
  - 从施放者前方沿朝向直线前进，可命中多个实体与受控方块。
  - 对命中的目标造成“真实剑气伤害”，尽量绕过常规护甲减免，可通过 DamageType/Damage Buckets 方案实现。
  - 可破坏部分方块（硬度与白名单限制，防 grief）。
- 威能公式（逻辑入口）：
  - Damage = Base × (1 + 剑道道痕系数 + 流派经验系数)
  - 道痕、流派经验由 mechanics/快照体系提供，不在行为中硬编码。
- 衰减机制：
  - 每次命中实体或破坏方块视为一次“命中事件”。
  - 每次命中后威能按比例衰减（示例：15%）。
  - 威能低于阈值则斩击提前终止。
- 前置与消耗：
  - 4转4阶前置由 `JianQiGuTuning` + Behavior 前置检查控制。
  - 消耗指定量 BURST 真元、精力、念头（全部集中在 tuning）。

### 1.2 被动：「气断山河」

- 对象：持有剑气蛊的实体（玩家和特定非玩家生物）使用「一斩开天」时生效。
- 机制：
  - 每次「一斩开天」的有效命中（实体/受控方块）增加“断势层数”。
  - 每满 3 层：
    - 为下一次「一斩开天」提供 25% 威能加成（乘区）。
    - 同时减少一次威能衰减（或等价衰减豁免）。
  - 若 10 秒未释放「一斩开天」，断势层数清空。
- 存储：
  - 使用 `OrganState`，根键 `"JianQiGu"`，字段常量化。

### 1.3 非玩家生物逻辑（重点补充）

为支持剑气蛊在非玩家实体身上的表现，增加如下规范：

- 适用前提：
  - 该生物拥有 JianQiGu 器官（通过现有 organ 系统判定）。
- OnHit 触发主动的几率机制：
  - 当 Mob 持有 JianQiGu 并进行近战攻击命中目标时：
    - 按 `JianQiGuTuning.NPC_ACTIVE_CHANCE`（如 10%-20%）判定是否触发「一斩开天」。
    - 仅在无冷却状态下可触发（见下一条）。
- 被动触发后的冷却限制：
  - 一旦通过 OnHit 成功触发 JianQiGu 主动（包括自动施放的剑气斩）：
    - 在 Mob 的 `OrganState("JianQiGu")` 中记录一个 NPC 专用冷却时间戳：
      - `KEY_NPC_TRIGGER_COOLDOWN_UNTIL`
    - 将其设置为当前时间 + 1 分钟（1200 tick）。
    - 后续 OnHit 在冷却未结束前，即使通过随机判定也不再触发自动主动技。
- 逻辑目标：
  - 保证非玩家生物的剑气蛊行为具有偶发爆发感：
    - “平时普通攻击 + 偶尔放出一记剑气斩”。
  - 避免高攻速怪物连锁刷屏，控制强度与性能。

详见第 3.5 小节的实现细则。

---

## 2. 目录结构与代码分层设计

遵循 [`AGENTS.md`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/AGENTS.md:5)：

- behavior/organ
  - `JianQiGuOrganBehavior.java`
    - 主动技触发（玩家/非玩家）
    - 被动「气断山河」断势逻辑
    - 非玩家 OnHit 随机触发逻辑与 1 分钟冷却
- calculator
  - `JianQiGuCalc.java`
    - 威能计算（含道痕/流派）
    - 衰减逻辑
    - 断势层数加成与衰减豁免计算
- tuning
  - `JianQiGuTuning.java`
    - 数值常量：基础伤害、范围、衰减率、冷却、资源消耗
    - 断势过期时间
    - 非玩家触发概率与冷却时长
- fx
  - `JianQiGuFx.java`
    - 剑光特效与断势提示
  - 文档说明：[`JianQiGuFx.md`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/fx/JianQiGuFx.md:1)
- entity（可选）
  - `JianQiGuSlashProjectile.java`
    - 若不复用现有 SwordSlashProjectile，则提供专用剑气投射物

4转条件通过 `JianQiGuTuning` + Behavior 内校验，不写入类名。

---

## 3. 技术方案分解

### 3.1 主动技触发与注册

- SkillId 建议：`guzhenren:jian_qi_yi_zhan_kai_tian`
- OrganId 建议：`guzhenren:jian_qi_gu`
- 在 [`JiandaoOrganRegistry`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/JiandaoOrganRegistry.java:19) 中增加 JianQiGu 的 `OrganIntegrationSpec`：
  - 玩家：
    - `addSlowTickListener(JianQiGuOrganBehavior.INSTANCE)`（检查断势过期等）
    - `ensureAttached(JianQiGuOrganBehavior.INSTANCE::ensureAttached)`
  - 非玩家：
    - 至少需要能在 OnHit 回调中识别 JianQiGu 并调用行为逻辑（详见 3.5）。
- 在 `OrganActivationListeners` 注册：
  - `OrganActivationListeners.register(ABILITY_ID, JianQiGuOrganBehavior.INSTANCE::activateAbility);`
- 客户端热键：
  - 在 [`JiandaoClientAbilities.onClientSetup`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/JiandaoClientAbilities.java:11) 中使用字面量 ResourceLocation 加入 SkillId。

### 3.2 威能与衰减计算（JianQiGuCalc）

- `computeInitialDamage(snapshot, tuning, duanshiTriggers)`：
  - 读取道痕/流派经验，套用 Damage 公式。
  - 根据 `duanshiTriggers`（断势触发次数）附加 `1 + 0.25 * triggers`。
- `computeDamageAfterHit(initialDamage, hitIndex, decayRate, decayGrace)`：
  - `hitIndex <= decayGrace` 时不衰减。
  - 否则按 `(1 - decayRate)^(hitIndex - decayGrace)` 计算。
- `shouldTerminate(damage, initialDamage, minRatio)`：
  - 用于确定剑气是否提前结束。

### 3.3 剑光 Projectile 与 Fx（高性价比方案）

详见 [`JianQiGuFx.md`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/fx/JianQiGuFx.md:1)。

要点：

- 使用扁平光刃 Quad，绑定在 Projectile Renderer：
  - Quad 长度/宽度/亮度根据 `visualPower`（威能归一化）动态缩放。
- `JianQiGuSlashProjectile` 字段：
  - `initialPower`
  - `currentPower`
  - `visualPower`（同步到客户端）
- Renderer（如 `JianQiGuSlashRenderer`）：
  - 根据方向渲染扁平剑光条。
  - 使用发光纹理 + 轻量粒子。
  - 高威能时剑光更长更亮，随命中衰减视觉弱化。

### 3.4 被动「气断山河」断势机制（玩家 & 非玩家通用）

- OrganState 根键：`"JianQiGu"`。
- 字段：
  - `KEY_DUANSHI_STACKS`：当前断势层数。
  - `KEY_LAST_CAST_TICK`：最近一次施放「一斩开天」的 tick。
- 逻辑：
  - 每次一斩开天有效命中：`STACKS++`。
  - 触发数：`triggers = STACKS / 3`。
  - 下一次一斩开天：
    - 应用额外伤害和衰减豁免。
    - 建议在使用后清空 STACKS（简单清晰）。
  - 若当前 tick - LAST_CAST_TICK ≥ 10 秒（200 tick）：
    - 清空 STACKS。

### 3.5 非玩家生物 OnHit 触发逻辑（新增重点）

非玩家生物使用 JianQiGu 时的专用规则：

1. 适用范围：
   - 实体为 `LivingEntity` 且非玩家。
   - 具有 JianQiGu 器官（通过 organ 系统检测）。

2. OnHit 自动触发流程（在 `JianQiGuOrganBehavior` 中实现）：

   - 在全局 OnHit 入口（已有 `addOnHitListener` 或兼容 Hook）中：
     - 若攻击者为玩家：
       - 不走此逻辑（玩家手动施放即可）。
     - 若攻击者为非玩家：
       - 检查其是否拥有 JianQiGu。
       - 调用 `tryTriggerNpcActiveOnHit(attacker, target, now)` 尝试自动触发。

3. `tryTriggerNpcActiveOnHit` 伪逻辑说明：

   - 读取 OrganState:
     - `cooldownUntil = state.getLong(KEY_NPC_TRIGGER_COOLDOWN_UNTIL, 0L);`
   - 若 `now < cooldownUntil`：
     - 仍在冷却中，直接返回（不触发）。
   - 否则（冷却完成）：
     - 生成一个 [0,1) 随机数：
       - 若 `< JianQiGuTuning.NPC_ACTIVE_CHANCE`（例如 0.15F = 15%）：
         - 触发一次 JianQiGu 主动技「一斩开天」：
           - 使用与玩家相同的激活路径（但不检查玩家键位/客户端，仅服务器直呼）
           - 技能参数可沿用 tuning 中的数值，或配置 NPC 专用倍率。
         - 设置新的冷却：
           - `state.setLong(KEY_NPC_TRIGGER_COOLDOWN_UNTIL, now + JianQiGuTuning.NPC_NPC_ONHIT_COOLDOWN_TICKS);`
           - 建议 `NPC_NPC_ONHIT_COOLDOWN_TICKS = 1200`（1 分钟）。
       - 若随机判定失败：
         - 不触发，保持无冷却或可选择仅在触发成功时上冷却（推荐只在成功触发时进入冷却）。

4. 设计意图：

- 确保：
  - 非玩家剑修怪物在战斗中偶尔放出“一斩开天”作为高光时刻。
  - 不会因为高攻速反复判定导致技能滥用：
    - 触发成功后统一进入 1 分钟冷却。
- 保持玩家与 Mob 行为一致：
  - 使用相同的 Behavior/Calc/Fx 逻辑，仅触发源不同。

---

## 4. 资源消耗与冷却（玩家）

- 全部配置在 `JianQiGuTuning`：
  - `BASE_DAMAGE`
  - `MAX_RANGE`
  - `DECAY_RATE`
  - `MIN_DAMAGE_RATIO`
  - `BURST_ZHENYUAN_COST`
  - `JINGLI_COST`
  - `NIANTOU_COST`
  - `BASE_COOLDOWN_TICKS`
  - `STACK_EXPIRE_TICKS`（10 秒）
  - `MIN_REALM_REQUIREMENT` / `MIN_TIER_REQUIREMENT`
  - `NPC_ACTIVE_CHANCE`（非玩家触发概率）
  - `NPC_ONHIT_COOLDOWN_TICKS`（非玩家触发冷却，默认 1200 tick）
- 行为层：
  - 玩家主动激活时通过 `ResourceOps` 检查与扣费。
  - 使用 `MultiCooldown` 管理玩家技能冷却。

---

## 5. Fx 与用户反馈

- 参照 [`JianQiGuFx.md`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/fx/JianQiGuFx.md:1)：
  - 高威能时剑光更长/更亮。
  - 命中与衰减在视觉上逐渐缩短/变暗。
- 「气断山河」断势：
  - 达到触发阈值时，可添加轻量提示（玩家端即可；非玩家无需 UI）。

---

## 6. 测试与阶段计划（简要）

- 单测：
  - `JianQiGuCalc`：威能、衰减、断势加成等纯函数。
- 联调：
  - 玩家：
    - 主动技触发/消耗/冷却/断势/剑光 Fx。
  - 非玩家：
    - OnHit 随机触发是否生效。
    - 冷却是否正确锁 1 分钟。
    - 不会出现触发风暴。
- 实施阶段：
  - Stage 1：结构与注册（Behavior/Calc/Tuning/Fx stub）
  - Stage 2：玩家主动技 + 剑光 Projectile
  - Stage 3：断势被动
  - Stage 4：非玩家 OnHit 触发 + 1 分钟冷却
  - Stage 5：Fx 微调与安全限制（方块破坏）
  - Stage 6：测试与参数调优

---

## 7. 约束与注意事项

- 全部数值入 `JianQiGuTuning`，不在行为中硬编码。
- 统一使用 `OrganState` / `MultiCooldown` / `ResourceOps`，不直接操作 Ledger/Linkage。
- 非玩家逻辑与玩家逻辑共用同一 Behavior，避免复制粘贴分叉实现。
- Projectile 与 Renderer 遵循现有注册模式（`CCEntities` + `JiandaoClientRenderers`）。
- 核心逻辑仅在服务端运行，Fx 在客户端执行。
