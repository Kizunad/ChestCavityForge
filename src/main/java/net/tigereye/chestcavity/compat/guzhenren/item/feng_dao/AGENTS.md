# 风道（feng_dao）重构指南（门面优先）

目标：对齐全局分层，零冗余，calculator 可测。

## 分层/注册
- behavior→注册与参数收集；runtime→副作用；calculator→纯逻辑；tuning→数值；fx/messages→表现与提示。
- 主动 enum + static 注册；被动走 `PassiveBus`/Organ 监听；快照走 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；对外仅 `*Ops`。

## 测试
- 优先补 calculator 单元测试；`./gradlew test`。

## TODO
- [ ] 数值迁出至 tuning；
- [ ] 计算迁出至 calculator；
- [ ] FX/提示集中；
- [ ] 若有 combo：独立四件套并从 item 桥接。
