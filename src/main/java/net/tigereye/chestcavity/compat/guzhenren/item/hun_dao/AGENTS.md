# 魂道（hun_dao）重构指南（门面优先）

遵循统一分层，抽离计算与数值，保持可测试与可维护。

## 分层/注册
- behavior→注册/参数；calculator→计算；runtime→副作用；fx/messages→表现；tuning→常量。
- 主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；统一 `*Ops` 门面。

## 测试
- 仅测 calculator；运行 `./gradlew test`。

## TODO
- [ ] 常量迁出；
- [ ] 计算迁出；
- [ ] FX/提示归口；
- [ ] combo（若有）分离至 combo 目录。
