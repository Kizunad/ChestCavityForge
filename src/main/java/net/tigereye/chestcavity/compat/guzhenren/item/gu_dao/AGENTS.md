# 骨道（gu_dao）重构指南（门面优先）

对齐全局规范，避免逻辑/数值分散与重复。

## 分层/注册
- behavior 注册与轻逻辑；calculator 纯计算；runtime 副作用；fx/messages 表现/提示；tuning 数值。
- 主动 enum + static；被动 `PassiveBus`/Organ 监听；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；外部仅 `*Ops` 门面。

## 测试
- 仅测 calculator；`./gradlew test`。

## TODO
- [ ] 将硬编码常量迁至 tuning；
- [ ] 将算法迁至 calculator；
- [ ] FX/提示归口；
- [ ] 若有 combo：独立四件套并由 item 桥接触发。
