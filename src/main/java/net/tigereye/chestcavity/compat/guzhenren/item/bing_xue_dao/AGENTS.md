# 冰雪道（bing_xue_dao）重构指南（门面优先）

## 目标与范围
- 与其它“道”保持一致的分层：behavior/{active, passive, organ} + calculator/ + runtime/ + fx/ + messages/ + tuning/ + state/
- 业务调用只经由 `*Ops` 门面；计算与数值统一在 `compat/common/{organ|tuning}`（若存在共用逻辑）。
- 组合技（若有）放入 `compat/guzhenren/item/combo/<family>/<dao>/...`，与 item 彻底分离（零冗余）。

## 分层规范
- behavior 层：仅做事件/注册与参数获取，禁止写重逻辑。
- calculator 层：纯逻辑与参数计算，可单元测试。
- runtime 层：执行副作用（位移/召唤/伤害/属性）、读写状态、冷却、网络同步。
- fx/messages 层：统一音效/粒子与玩家提示文案。
- tuning 层：全部“数值常量/ID/键名”。

## 注册与总线
- 主动技能：enum 单例 + static 注册到 `OrganActivationListeners`；如需参数快照，先在 `ActivationHookRegistry` 注册。
- 被动：用 `PassiveBus`（家族级）或 Organ 监听（`Organ*Listener`）统一入口，禁止分散事件绑定。

## 依赖与助手
- 冷却：`MultiCooldown`；资源：`ResourceOps`；护盾：`AbsorptionHelper`；账本：`LedgerOps`。
- 统一通过 `*Ops` 门面暴露能力，避免直接引用 Calculator。

## 测试
- 仅测 calculator 层的纯函数（JUnit5），示例见 `src/test/java/...`。
- 运行：`./gradlew test`。

## TODO（建议）
- [ ] 盘点仍在 behavior/runtime 中的硬编码常量，迁至 tuning。
- [ ] 将计算迁入 calculator，并补足单测。
- [ ] FX/提示迁入 fx/messages，避免内联。
- [ ] 若存在 combo，建立 combo 目录四件套（calculator/tuning/fx/behavior），并从 item 桥接触发。
