# Phase 2｜分层重构（core + systems）

## 阶段目标
- 建立 systems 目录；迁移 movement/combat/upkeep；削薄实体逻辑。

## 任务列表
- 建立 `systems/` 目录与 README（职责与顺序）。
- MovementSystem：应用 SteeringTemplate → setDeltaMovement。
- CombatSystem：集中命中检测与伤害，触发 OnHitEntity/PostHit。
- UpkeepSystem：集中维持消耗，触发 OnUpkeepCheck，调用 ResourceOps。
- `FlyingSwordEntity.tick/hurt/mobInteract` 仅组装上下文与触发事件。

## 依赖关系
- Phase 1 完成。

## 验收标准
- 编译与基础回归通过；实体类行数显著下降。

## 风险与回退
- 中高；逐块迁移，每块迁移后做回归测试与备份。

