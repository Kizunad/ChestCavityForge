# FxEngine 时间线系统 · 实施文档

## 概述

- 依据需求与技术框架，分阶段实现：引擎骨架 → Builder/合并/预算 → 工具与示例 → 清理与测试。

## 实施步骤

- 阶段一：引擎骨架
  - 实现 FxTrack/FxTimelineEngine/FxScheduler 最小子集；注册到 TickEngineHub。
  - 提供最小门面：schedule/cancel；支持 TTL 与 tickInterval。

- 阶段二：Builder 与合并/预算
  - 加 FxTrackSpec 与 mergeKey ；实现 perLevel/perOwner caps（配置默认关闭）。
  - 合并策略：extendTtl|drop|replace（默认 extendTtl）。

- 阶段三：工具与示例
  - 提供 Interpolators/Shapes/Samplers 实用函数；
  - 编写示例：Shockfield “环纹扩张”与“次级扩散” FX（仅文档/伪代码层面）。

- 阶段四：清理与测试
  - 边界与异常处理；统计与 Debug 开关；
  - 单元测试（纯逻辑）与手册测试用例。

## 预算控制（实现要点）

- 配置项（默认关闭）：
  - fxEngine.budget.enabled=false
  - fxEngine.budget.perLevelCap=256
  - fxEngine.budget.perOwnerCap=16
- 执行顺序：校验配置 → 统计当前活跃量 → 超限 → 合并/丢弃/替换。

## 风险与缓解

- 风险：FX 过量导致卡顿 → 预算与合并；tickInterval；gating。
- 风险：异常中断主循环 → try/catch 隔离；限时回调。
- 风险：文档与实现偏差 → 每阶段评审与对照测试用例。

