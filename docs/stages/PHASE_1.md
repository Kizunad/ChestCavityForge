# Phase 1｜裁剪（先停用后清理）

## 阶段目标
- 通过开关精简轨迹/意图；停用旧式 Goal；TUI/Gecko/Swarm 默认关闭。

## 任务列表
- `Trajectories` 条件注册：仅 Orbit/PredictiveLine（可选 CurvedIntercept）。
- `IntentPlanner` 每模式≤2意图；其余 behind `ENABLE_EXTRA_INTENTS`。
- 移除 `ForceHuntTargetGoal` 与 `SwordGoalOps`（或先停用）。
- `SwordCommandCenter` 精简；`CommandTactic` 收敛；TUI 受开关控制。
- Gecko/覆盖/视觉档 loader 受开关控制。
- Swarm 行为 behind `ENABLE_SWARM`；QingLianDomain 提供降级路径。

## 依赖关系
- 依赖 Phase 0 的开关与初始化。

## 验收标准
- 编译通过；默认开关=关闭下基础功能（ORBIT/GUARD/HUNT）可用。
- 未见引用的实现标记为删除候选。

## 风险与回退
- 中；如影响体验，将开关临时打开恢复原行为。

