# 食道（shi_dao）重构指南（门面优先）

对齐统一模板：
- 分层：behavior / calculator / runtime / fx / messages / tuning；
- 注册：enum + static；被动：`PassiveBus`/Organ；快照：`ActivationHookRegistry`；
- 依赖：`MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；
- 测试：calculator 层（JUnit5）。

TODO：常量→tuning；逻辑→calculator；FX/提示→归口；combo→独立四件套。
