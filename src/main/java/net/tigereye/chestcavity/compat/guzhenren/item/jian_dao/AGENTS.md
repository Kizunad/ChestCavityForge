# 剑道（jian_dao）模块约定与格式规范

保证代码整洁、职责单一、可测试。请严格遵循以下分层与命名规则。

## 目录分层（必须）
- behavior/organ：器官行为实现（枚举单例）。
  - 例如：`behavior/organ/JianXinGuOrganBehavior.java`、`JianYingGuOrganBehavior.java`、`JianYuGuOrganBehavior.java`
- behavior/passive：被动事件订阅与桥接。
  - 例如：`behavior/passive/JianXinGuPassiveEvents.java`
- behavior/active：主动技能分发/桥接（轻量入口，转调 organ/skills）。
- behavior/skills：跨器官可复用的技能实现（若只服务单器官，留在 organ）。
- calculator：纯函数计算（可单测，不直接依赖 Minecraft 复杂类型）。
  - 例如：`calculator/JianXinGuCalc.java`、`calculator/JianXinGuPassiveCalc.java`
- tuning：调参常量（无逻辑）。
  - 例如：`tuning/JianXinGuTuning.java`、`tuning/JianXinGuPassiveTuning.java`、`tuning/JianYuGuTuning.java`
- fx：轻量表现/提示（UI toast、粒子/音效入口；默认克制）。
  - 例如：`fx/JianXinGuFx.java`
- 其他子域：`client/`、`entity/`、`events/`、`tuning/`（飞剑等子模块自成体系时再分层）。

## 注册与引用规范（必须）
- 器官行为类用 enum 单例；在 static 块中通过 `OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)` 注册主动技。
- 禁止在构造函数做注册或执行重逻辑（避免客户端早期 classloading 崩溃）。
- 客户端热键：仅以字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`；禁止在客户端引用行为类常量。
- 服务端激活：网络包 -> `OrganActivationListeners.activate(id, cc)` -> 行为 `activateAbility`；默认静默失败（不打日志）。
- 参数快照：涉及玩家状态动态参数的技能，需在 `ActivationHookRegistry` 注册 `ResourceFieldSnapshotEffect`，保证激活时参数可用。

## 状态、冷却与资源（必须）
- 状态存储：统一使用 `OrganState`，每个器官定义固定根键（如 `"JianXinGu"`），字段名集中定义并复用。
- 冷却：统一 `MultiCooldown`；禁止私造时间戳。需要提示使用 `ActiveSkillRegistry.scheduleReadyToast` 或对应 Fx 辅助。
- 资源与账本：统一 `ResourceOps` / `LedgerOps`；禁止直接 `LinkageChannel.adjust` 或手动 `ledger.remove`。
- 护盾：统一 `AbsorptionHelper` 与相关上限恢复工具。

## 命名与常量（必须）
- 类名：`<OrganName>OrganBehavior`、`<OrganName>PassiveEvents`、`<OrganName>Calc`、`<OrganName>Tuning`、`<OrganName>Fx`。
- 资源标识：`MOD_ID = "guzhenren"`；物品与技能 `ResourceLocation` 用 `fromNamespaceAndPath(MOD_ID, "...")` 构造。
- 常量入 tuning；behavior 内仅引用，不得埋硬编码数值。
- 事件键/字段键统一常量化，保持与 calculator 层注释一致。

## 依赖约束（必须）
- 优先使用现有助手：`LedgerOps`、`ResourceOps`、`MultiCooldown`、`OrganStateOps`、`AbsorptionHelper`。
- 反应/DoT：遵循全局 Reaction 规范；仅使用带 `typeId` 的 `DoTEngine.schedulePerSecond` 重载。

## 测试规范（强烈建议）
- 仅对 calculator 层与纯逻辑代码编写单测（JUnit 5 + Mockito 已配置）。
- Minecraft 实体/世界等核心类不可 mock；抽离为纯函数后测试。
- 运行：`./gradlew test` 或 `./scripts/run-tests.sh`。

## 迁移与提交前检查
- 新增行为：优先落位 organ/passive/skills，再补充 calculator/tuning/fx；避免 cross-package 互相引用。
- 搜索自检：`rg "behavior\\.jianxin|LinkageChannel\.adjust|ledger\.remove|FMLJavaModLoadingContext"`，确保未违反规范。
- ActiveSkillRegistry 的 `sourceHint` 使用新 organ 路径：`compat/guzhenren/item/jian_dao/behavior/organ/...`。

## 示例
- 冷却提示：`JianXinGuFx.scheduleCooldownToast(player, ABILITY_ID, readyAt, now)`。
- 资源读取：`ResourceOps.openHandle(player).ifPresent(h -> h.adjustJingli(+v, true));`
- 域等级：`JianXinGuCalc.computeDomainLevel(player)`（或传入快照参数的重载）。
