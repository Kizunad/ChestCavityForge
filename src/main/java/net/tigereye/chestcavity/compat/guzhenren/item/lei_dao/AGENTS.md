# 雷道（lei_dao）重构指南（门面优先）

统一分层、门面、测试与 FX 归口，避免冗余。

## 分层/注册
- behavior 注册/参数；calculator 逻辑；runtime 副作用；fx/messages 表现；tuning 数值。
- 主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`。

## 测试
- 只测 calculator；`./gradlew test`。

## TODO
- [ ] tuning 化常量；
- [ ] 下沉计算；
- [ ] FX/提示归口；
- [ ] combo（若有）独立四件套。
