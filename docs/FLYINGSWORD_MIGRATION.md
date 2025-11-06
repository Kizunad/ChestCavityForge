# 飞剑模块迁移说明（Migration Guide）

> 适用于从旧版飞剑实现迁移到 Phase 4–7 重构后的版本。

## 1. 冷却管理（Phase 4）
- 旧：实体内维护 `attackCooldown` 等字段，非持久化；
- 新：统一存储到主人附件 `MultiCooldown`，通过 `FlyingSwordCooldownOps` 读写；
- 影响：
  - 冷却自动随主人存档持久化；
  - 切换主人/重载区块后冷却不丢失；
  - 禁止直接操作 `LinkageChannel.adjust/ledger.remove`；使用 `LedgerOps/ResourceOps`。

## 2. 维持消耗与失败策略（Phase 4）
- 统一入口：`UpkeepSystem` 调用 `UpkeepOps` 扣减；
- 失败策略：`FlyingSwordTuning.UPKEEP_FAILURE_STRATEGY`（RECALL/STALL/SLOW，默认 RECALL）；
- 扩展：`OnUpkeepCheck` 事件可修改最终消耗或跳过本次消耗。

## 3. 事件模型扩展（Phase 3）
- 新增上下文：ModeChange/TargetAcquired/TargetLost/UpkeepCheck/PostHit/BlockBreakAttempt/ExperienceGain/LevelUp；
- 调用：由 `systems/` 各子系统在关键路径触发；
- 约定：前置事件可取消或改参，后置事件只读。

## 4. 客户端与网络（Phase 5）
- 默认降噪：关闭 Gecko/覆盖/TUI；
- 渲染器：`FlyingSwordRenderer` 仅按需要查询 Override/Profile；
- 指挥中心：TUI 关闭时提供最小文本反馈。

## 5. 文档与测试（Phase 6）
- 纯逻辑单元测试：Calculator/UpkeepOps/Kinematics 已覆盖；
- 集成测试（依赖 MC 实体）默认 @Disabled，建议在游戏内或集成环境验证。

## 6. 渲染姿态（Phase 7 标注，Phase 8 实施）
- 当前：欧拉 Y→Z 路径（Legacy），在某些半圆飞行出现“缓慢抬头”；
- 计划：引入 `OrientationOps`（正交基/四元数）统一姿态，提供 `orientationMode/upMode` 与 `USE_BASIS_ORIENTATION` 开关；
- 回退：保留 Legacy 作为兼容选项。

## 7. 迁移建议
- 避免直接依赖旧实体冷却字段；统一通过 `FlyingSwordCooldownOps` 访问；
- 资源消耗仅走 `UpkeepSystem/UpkeepOps`；
- 关注 `docs/stages/PHASE_8.md` 的姿态统一与兼容策略；
- 升级后执行 `./gradlew compileJava && ./gradlew test` 与手动回归（参考 `docs/MANUAL_TEST_CHECKLIST.md`）。

---

如有问题，请在 issue 中附上日志（DEBUG 级别）与重现步骤。谢谢！
