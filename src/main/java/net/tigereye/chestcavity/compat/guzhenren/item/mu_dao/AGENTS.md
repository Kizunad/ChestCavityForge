# 木道（mu_dao）重构指南（门面优先）

统一分层与门面，数值/逻辑出栈到 tuning/calculator，FX/提示归口。

## 分层/注册
- behavior 注册与参数；calculator 逻辑；runtime 副作用；fx/messages 表现；tuning 数值。
- 主动 enum + static；被动 `PassiveBus`/Organ；参数快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；`*Ops` 门面。

## 测试
- 只测 calculator；`./gradlew test`。

## TODO
- [ ] tuning 化；
- [ ] 计算下沉；
- [ ] FX/提示归口；
- [ ] combo（若有）拆分。
