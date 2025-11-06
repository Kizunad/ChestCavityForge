# 阶段二（Stage 2）· Builder 与预算/合并

## 阶段目标

- 引入 FxTrackSpec Builder；完成预算控制（per-level/per-owner）与合并策略。

## 具体任务

- FxTrackSpec：mergeKey、tickInterval、gating、策略字段。
- 预算控制：读取配置（默认关闭）；统计活跃量并执行策略。
- 合并策略：extendTtl|drop|replace（默认 extendTtl）。

## 依赖关系

- Stage 1 完成。

## 验收标准

- 在预算开启后，上限被严格执行；合并按配置生效。
- 单元测试覆盖：合并与预算路径；异常路径。

