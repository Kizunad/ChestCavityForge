# 光道（guang_dao）重构指南（门面优先）

遵循统一分层与注册规范，保持零冗余与可测试性。

## 分层/注册
- behavior（注册/参数）→ calculator（纯逻辑）→ runtime（副作用）→ fx/messages（表现）→ tuning（数值）。
- 主动 enum + static；被动 `PassiveBus`/Organ 监听；快照 `ActivationHookRegistry`。

## 依赖
- 统一使用 `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；对外 `*Ops` 门面。

## 测试
- calculator 单元测试，避免依赖 Minecraft 类。

## TODO
- [ ] 常量迁出 tuning；
- [ ] 逻辑迁入 calculator；
- [ ] FX/提示归口；
- [ ] 有 combo 则分离至 combo 目录。
