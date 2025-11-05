# Phase 4｜冷却与资源一致性

## 阶段目标
- 冷却集中 `MultiCooldown`；资源消耗统一 `ResourceOps`；失败策略可配。

## 任务列表
- 将实体内攻击/破块等冷却迁至 `MultiCooldown`（owner 附件）。
- UpkeepSystem：唯一入口扣减与失败策略（停滞/减速/召回）。
- 在 `FlyingSwordTuning` 暴露策略配置。

## 依赖关系
- 依赖 Phase 2/3。

## 验收标准
- 无负冷却/残留；资源扣减与策略生效且可观测。

## 风险与回退
- 中；临时保留旧字段镜像，便于回退与对比。

