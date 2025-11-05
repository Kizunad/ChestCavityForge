# Phase 3｜事件模型扩展

## 阶段目标
- 新增关键事件上下文并接线：Mode/Target/Upkeep/PostHit/BlockBreakAttempt/Exp。

## 任务列表
- 在 `events/context/` 添加新上下文定义。
- 在 `FlyingSwordEventRegistry` 添加 fireXxx 方法，遵循短路语义。
- 在 systems/ 与 Controller 入口触发相应事件。

## 依赖关系
- 依赖 Phase 2 的系统抽取。

## 验收标准
- 注册测试钩子验证事件触发；可取消的事件能短路默认流程。

## 风险与回退
- 中；保持默认钩子行为不变，新增事件默认无副作用。

