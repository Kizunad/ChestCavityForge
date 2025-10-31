# 剑道（jian_dao）重构指南（门面优先）

参照统一模板，避免跨层引用与数值散落。

## 分层/注册
- behavior 注册；calculator 计算；runtime 副作用；fx/messages 表现；tuning 常量。
- 主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；统一 `*Ops` 门面暴露。

## 测试
- calculator 层单测；`./gradlew test`。

## TODO
- [ ] 迁出数值到 tuning；
- [ ] 迁出算法到 calculator；
- [ ] FX/提示集中到 fx/messages；
- [ ] 有 combo 则拆分目录并从 item 桥接。
