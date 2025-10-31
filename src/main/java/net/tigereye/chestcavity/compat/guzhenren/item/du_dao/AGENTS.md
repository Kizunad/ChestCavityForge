# 毒道（du_dao）重构指南（门面优先）

参照 bing_xue_dao 的分层与流程执行，目标：零冗余、可测算。

## 分层规范与注册
- 同步使用 behavior/{active, passive, organ} + calculator + runtime + fx + messages + tuning。
- 主动 enum + static 注册；被动统一走 `PassiveBus` 或 Organ 监听；参数快照通过 `ActivationHookRegistry`。

## 依赖与助手
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`。
- 对外仅暴露 `*Ops` 门面。

## 测试
- 只测 calculator 纯函数；`./gradlew test`。

## TODO
- [ ] 迁出数值到 tuning；
- [ ] 迁出计算到 calculator；
- [ ] FX/提示归口 fx/messages；
- [ ] 若有 combo：独立目录四件套 + 从 item 桥接。
