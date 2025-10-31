# 奴道（nu_dao）重构指南（门面优先）

对齐统一重构规范，保证零冗余与可测试。

## 分层/注册
- behavior 注册；calculator 纯逻辑；runtime 副作用；fx/messages 表现；tuning 数值。
- enum + static 主动注册；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；统一 `*Ops` 门面。

## 测试
- calculator 层；`./gradlew test`。

## TODO
- [ ] tuning；
- [ ] calculator；
- [ ] fx/messages；
- [ ] combo 分离（若有）。
