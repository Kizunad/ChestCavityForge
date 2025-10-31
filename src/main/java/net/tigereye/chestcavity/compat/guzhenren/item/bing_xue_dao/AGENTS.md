# 冰雪道（bing_xue_dao）分层与开发指引

本指引用于规范冰雪道模块的代码结构、职责划分与迁移校验，确保“可测试、可维护、低耦合”。本目录的规范同样可作为其它道系迁移模板复用。

---

## 目录结构（必须遵守）

- `behavior/active/` 主动技能触发入口（仅注册与转发）
  - 例：`BingJiGuActive.java`、`ShuangXiGuActive.java`
  - 注册模式：`enum` 单例 + `static { OrganActivationListeners.register(...) }`
- `behavior/passive/` 被动挂钩（从被动总线回调到对应 Organ 行为）
  - 例：`BingJiGuPassive.java`、`ShuangXiGuPassive.java`、`QingReGuPassive.java`、`BingBuGuPassive.java`
- `behavior/organ/` Organ 行为主类（监听/组装/调度层，禁止重逻辑）
  - 例：`BingJiGuOrganBehavior.java`、`ShuangXiGuOrganBehavior.java`、`QingReGuOrganBehavior.java`、`BingBuGuOrganBehavior.java`
- `calculator/` 纯逻辑计算（必须可单测）
  - 例：`BurstParamOps.java`（冰爆参数）、`BreathParamOps.java`（霜息参数）、`CooldownOps.java`、`ConeFilter.java`、`AoEFalloff.java`、`ChanceOps.java`
- `runtime/` 运行时执行（目标搜集、效果应用、残留域、脚本流等）
  - 例：`IceBurstRuntime.java`、`FrostBreathRuntime.java`、`ColdEffectOps.java`
- `tuning/` 参数常量与配置访问器
  - 例：`BingJiTuning.java`、`ShuangXiTuning.java`
- `fx/` 轻粒子与音效封装
  - 例：`BingJiFx.java`、`ShuangXiFx.java`
- `messages/` 玩家提示与提示节流
  - 例：`BingXueMessages.java`、`FailNotifier.java`
- `state/` 状态键与 MultiCooldown 槽（仅键名与读写帮助）
  - 例：`BingJiStateKeys.java`

---

## 分层职责（该做/不该做）

- behavior/active
  - 该做：按 ID 注册触发器；将激活转发给 `behavior/organ/*OrganBehavior.activateAbility`。
  - 不该做：任何重逻辑、冷却/资源扣除、状态持久化。

- behavior/passive
  - 该做：从被动回调（Tick/受击/命中）找到对应 Organ 并转发到 `behavior/organ/*OrganBehavior`。
  - 不该做：业务计算/效果实现/资源变更。

- behavior/organ
  - 该做：
    - 监听链路（慢 tick/受击/入伤/地面/移除）
    - 读取快照（`ActivationHookRegistry` 已注册 `ResourceFieldSnapshotEffect`）
    - 组装参数并调用 `calculator/*`、`runtime/*`、`fx/*`、`messages/*`、`state/*`
  - 不该做：
    - 复杂数学计算（放到 `calculator/`）
    - 目标遍历/DoT/残留域/脚本流（放到 `runtime/`）
    - 直接写 NBT/属性表（统一由 `MultiCooldown/AbsorptionHelper/OrganStateOps` 等助手或 `state/*` 管）

- calculator
  - 该做：只写纯函数；无 Minecraft 依赖；必须有单测。
  - 不该做：修改实体、读写世界、播放 FX。

- runtime
  - 该做：围绕实体/世界的“执行”，如搜集目标、应用效果、入队残留域、触发 Flow 脚本。
  - 不该做：读取配置或做复杂数学（放到 `tuning/` 与 `calculator/`）。

- messages
  - 该做：提供文案键与失败提示的节流（`FailNotifier` 80 tick 节流）。
  - 不该做：硬编码业务逻辑或直接修改状态。

- state
  - 该做：提供键名常量与 `MultiCooldown.Entry` 便捷访问器。
  - 不该做：在此实现调度或业务逻辑。

---

## 必遵规范（结合项目总规范）

- DoT Reaction
  - 仅使用带 `typeId` 的 `DoTEngine.schedulePerSecond(...)` 重载；`typeId==null` 将抛异常。
  - DoT 类型统一使用 `net.tigereye.chestcavity.util.DoTTypes` 中声明的常量。
  - 反应引擎入口：`util/reaction/ReactionRegistry.preApplyDoT(...)` 可取消当次伤害；新增反应用 `ReactionRegistry.register(...)`。

- Attack Ability 注册
  - 行为类使用 `enum` 单例在 `static { ... }` 中调用 `OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)`。
  - 客户端热键仅将字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`，不要强引类常量避免早期 classloading。

- 助手优先
  - 资源/冷却/护盾统一复用：`ResourceOps`、`MultiCooldown`、`AbsorptionHelper`、`LedgerOps`。
  - 禁止直接调用 `LinkageManager` / `LinkageChannel.adjust` / `ledger.remove`。

- 快照参数
  - 冰雪道技能已在 `ActivationHookRegistry` 注册 `ResourceFieldSnapshotEffect` 与 `ComputedBingXueDaohenEffect`；行为层读取技能快照，不做即时拉取。

---

## 迁移步骤（模板）

1) 归档文件到新分层
   - Active → `behavior/active/`；Passive → `behavior/passive/`；Organ 行为 → `behavior/organ/`
2) 抽离计算器
   - 将半径/伤害/锥形/概率/冷却等纯数学下沉到 `calculator/` 并补单测
3) 抽离运行时
   - 将目标遍历、效果应用、残留域与脚本流下沉到 `runtime/`
4) 抽离 FX/消息/状态/调优
   - 粒子音效 → `fx/`；文案与节流 → `messages/`；状态键 → `state/`；配置访问 → `tuning/`
5) 行为类瘦身
   - 仅组装参数 + 调用分层；不再直接操作 NBT 或属性
6) 校验
   - 见“核对清单”，并运行编译/测试

---

## 核对清单（完成必查）

- 目录与注册
  - [ ] Active 为 `enum` 单例并在 `static {}` 中注册到 `OrganActivationListeners`
  - [ ] Passive 只做转发，不含业务逻辑
  - [ ] Organ 行为只组装/转发，重逻辑不在行为类

- 计算与运行
  - [ ] 所有计算均在 `calculator/*`，且有相应单测
  - [ ] 运行时逻辑在 `runtime/*`，含目标筛选、效果应用、残留域与 Flow
  - [ ] 粒子与音效仅经 `fx/*` 调用

- 资源与冷却
  - [ ] 统一通过 `ResourceOps` 消耗/调整资源
  - [ ] 冷却统一 `MultiCooldown`（若走玩家内置冷却，同步 FailNotifier 低噪提示）

- 状态与吸收
  - [ ] 键名与 Entry 通过 `state/*` 访问（如 `BingJiStateKeys`）
  - [ ] 护盾统一通过 `AbsorptionHelper`，卸下时清理修饰符

- DoT 与反应
  - [ ] DoT 均使用 `DoTTypes` 常量并带 `typeId`
  - [ ] 需要霜痕/免疫等标签时通过 `ReactionTagOps.add/clear`

- 快照与参数
  - [ ] 读取技能快照（道痕/流派经验）而非实时查询
  - [ ] 参数计算（如 `BurstParamOps/BreathParamOps`）与运行时（如 `IceBurstRuntime/FrostBreathRuntime`）对齐

- 文案与节流
  - [ ] 提示文本在 `messages/BingXueMessages`，提示频率使用 `FailNotifier` 节流

---

## 行为规则（冰雪道当前期望）

- 冰肌蛊（BingJiGu）
  - 主动“冰爆”：具备玉骨 + 消耗 1 个 `*muscle`；对范围敌人造成伤害与高额减速；爆心生成霜雾残留域；发送提示
  - 被动：消耗真元/加精力/小治愈；周期刷新吸收；具备玉骨时清理流血

- 霜息蛊（ShuangXiGu）
  - 主动“霜息”锥形命中目标；挂冷效果与霜痕；按概率施加霜蚀 DoT；粒子与音效克制
  - 与冰肌蛊一致的失败提示：冷却中 / 真元不足

- 清热蛊（QingReGu）
  - 成功供能后短时获得“霜免疫”并清理火系标记；首次授予轻提示

- 冰布蛊（BingBuGu）
  - 给予饱和/再生并短时“霜免疫”；轻提示与雪花粒子

---

## 不要做什么（常见误区）

- 不要在 `enum` 之外的构造函数中做注册或重逻辑（防止早期 classloading 崩溃）
- 不要直接使用 `LinkageManager/LinkageChannel/ledger.remove`，统一走 `LedgerOps/ResourceOps`
- 不要从行为类直接操作 NBT（使用 `MultiCooldown/OrganStateOps/state/*`）
- 不要在行为类内写数学/目标遍历/FX/残留域（下沉到 `calculator/`、`runtime/`、`fx/`）

---

## 测试与验证

- 单元测试（纯逻辑）
  - 位置：`src/test/java/net/tigereye/chestcavity/compat/guzhenren/item/bing_xue_dao/calculator/`
  - 运行：`./gradlew test` 或 `./scripts/run-tests.sh`

- 编译与手测
  - `./gradlew compileJava`
  - 客户端手测：`./gradlew runClient`（装备/卸下/触发/护盾刷新/失败提示）

---

## 示例参照

- Organ 行为（仅组装）：
  - `behavior/organ/BingJiGuOrganBehavior.java`
  - `behavior/organ/ShuangXiGuOrganBehavior.java`
- 计算器：
  - `calculator/BurstParamOps.java`、`calculator/BreathParamOps.java`、`calculator/ConeFilter.java`
- 运行时：
  - `runtime/IceBurstRuntime.java`、`runtime/FrostBreathRuntime.java`
- 提示与粒子：
  - `messages/FailNotifier.java`、`fx/BingJiFx.java`、`fx/ShuangXiFx.java`

> 若新增 Organ/技能，请在提交前用“核对清单”自检一遍，确保与冰雪道既有分层一致。

