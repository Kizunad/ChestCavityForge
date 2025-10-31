# 金道（jin_dao）重构指南（门面优先）

对齐分层与注册规范，保持 calculator 可测、tuning 可配，FX/提示归口。

## 分层/注册
- behavior（注册/参数）→ calculator（纯逻辑）→ runtime（副作用）→ fx/messages（表现）→ tuning（数值）。
- 主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；门面 `*Ops`。

## 测试
- 纯函数单测；`./gradlew test`。

## TODO
- [ ] 数值/ID 迁至 tuning；
- [ ] 计算迁至 calculator；
- [ ] FX/提示集中；
- [ ] combo 路径独立（若有）。
