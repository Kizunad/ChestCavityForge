# FxEngine 时间线系统 · 技术框架（KISS/轻量）

## 设计原则

- KISS：最小必要接口与实现，避免过度抽象。
- 轻量：复用现有 TickEngineHub/TickOps；无新外部依赖。
- 易测：纯逻辑部分（合并/预算/状态机）可单测。

## 模块与职责

- FxTrack（接口）
  - 字段：id、ttlTicks、tickInterval、ownerId、waveId、anchorSupplier（位置/实体/Wave采样器）
  - 回调：onStart(level)、onTick(level, nowTick)、onStop(level, reason)
- FxRegistry（注册中心）
  - API：register(id, factory)、unregister(id)、play(id, context)
  - factory: (FxContext) -> FxTrackSpec（根据上下文构建规格）
  - context: {level, ownerId?, waveId?, position?, customParams}
  - 作用：在“定义处”和“调用处”解耦，实现“先注册、后处处可调用”的便捷模式。
- FxTrackSpec（Builder/数据）
  - 构造 Track 所需参数与回调，含 mergeKey、gating、策略配置。
- FxTimelineEngine（引擎）
  - TickEngineHub 注册，维护活跃 Track；处理预算、合并、门控、超时与停止。
- FxScheduler（门面）
  - 对外 schedule/cancel/update/find；执行配置读取与预算校验。

## 配置（默认关闭）

- fxEngine.enabled = false（总开关）
- fxEngine.budget.enabled = false（预算开关）
- fxEngine.budget.perLevelCap = 256
- fxEngine.budget.perOwnerCap = 16
- fxEngine.merge.defaultStrategy = extendTtl|drop|replace（默认 extendTtl）
- fxEngine.tick.defaultInterval = 1

说明：本配置项仅为方案设计命名，落地时挂入现有配置体系（如 CCConfig）。

## 与现有系统集成

- Tick 调度：复用 TickEngineHub（优先级建议 DOT+15），保证在 ShockfieldManager 之后运行（便于采样半径/振幅）。
- 一次性延时：保留 TickOps/DelayedTaskScheduler 用于一次性任务。
- ShockfieldFxService：保持回调不变；实现方在回调中调用 FxScheduler 提交时间线。
  - 建议：常用 FX 方案通过 FxRegistry.register("shockfield:ring", factory) 预注册；在 onWaveCreate/onSubwaveCreate/onHit 使用 FxRegistry.play(id, ctx) 复用。

## 数据结构与算法要点

- 活跃表：
  - per-level 活跃 Map：trackId -> Track
  - per-owner 索引：ownerId -> Set<trackId>
  - per-mergeKey 索引：mergeKey -> trackId（用于合并/延长TTL）
- 门控：
  - 玩家半径检测（取最近玩家距离阈值）；区块 isLoaded 检查；Owner 存活快速校验
- 状态机：
  - CREATED → STARTED（首 tick 前 onStart）→ RUNNING（每 tick 或间隔 onTick）→ STOPPING → STOPPED

## 性能与稳健性

- 预算与限流：严格执行上限；合并优先；日志降噪。
- 失败隔离：onTick/onStart/onStop 异常捕获；不中断其他 Track。
- 统计：活跃数、合并次数、丢弃次数、暂停次数（按需）。

## 交付物（核心接口草图）

- FxTrack / FxTrackSpec / FxTimelineEngine / FxScheduler（不在此文档放代码）。
