# 力道（li_dao）重构指南（门面优先）

遵循统一分层与门面，保证零冗余和可测试。

## 分层/注册
- behavior→calculator→runtime→fx/messages→tuning；主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`，门面 `*Ops`。

## 测试
- calculator 单测；`./gradlew test`。

## TODO
- [ ] 常量→tuning；
- [ ] 逻辑→calculator；
- [ ] FX/提示→归口；
- [ ] combo（若有）→独立四件套。
