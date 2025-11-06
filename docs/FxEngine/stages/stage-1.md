# 阶段一（Stage 1）· 引擎骨架

## 阶段目标

- 实现最小可用的 FX 时间线：FxTrack/FxTimelineEngine/FxScheduler；TickEngineHub 注册。

## 具体任务

- 定义 FxTrack 接口与 StopReason。
- 实现 FxTimelineEngine：
  - 维护活跃 Track；
  - 每 tick 处理 onStart/onTick/TTL 停止；
  - 异常隔离。
- 实现 FxScheduler：schedule/cancel；最小合并（仅同 id 防重）。

## 依赖关系

- 依赖 TickEngineHub（已存在）；无外部依赖。

## 验收标准

- 可提交一个简单 Track 并按 tickInterval 运行至 TTL；停止回调触发一次。
- 无控制台刷屏；异常不影响其他逻辑。

