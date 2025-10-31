# 土道（tu_dao）重构指南（门面优先）

保持与其他“道”一致的重构落位：
- 分层：behavior / calculator / runtime / fx / messages / tuning；
- 注册：enum + static；被动：`PassiveBus`/Organ；快照：`ActivationHookRegistry`；
- 依赖：`MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；
- 测试：calculator 单元测试。

TODO：数值/算法下沉与 FX/提示归口、combo 拆分（若有）。
