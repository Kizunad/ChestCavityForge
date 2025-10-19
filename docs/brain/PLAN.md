# Soul Brain 规划（沿用现有实现）

本文档描述在现有 Brain 框架（BrainController/Brain/HierarchicalBrain/SubBrain/ActionScheduler）基础上，分阶段完善“行动大脑”的目标、结构、模式切换、仲裁、时间管理、命令与 UI、集成与验证方案。

## 目标与原则
- 选择/启动分层：子大脑只做“选择/启动”，动作自身按 ActionScheduler 调度。
- 并发有序：允许并发但受仲裁与预算约束；最小驻留与迟滞消除抖动。
- 统一时间管理：MultiCooldown + DelayedTaskScheduler；禁止手写 tick 计数器。
- 集成一致：遵循 DoT/Reaction、ResourceOps/LedgerOps/AbsorptionHelper 规范；导航可选 Baritone，失败回退。

## 结构
- BrainController（统一入口，SoulRuntimeHandler）：在 onTickEnd 读取 BrainMode，构造 BrainContext，调度子大脑。
- Brain 接口 + 子大脑库：现有 Combat/Idle/LLM；新增 Survival/Exploration（后续接线）。
- BrainContext：封装 level/soul/owner/ActionStateManager/意图快照。

## 模式与切换
- 模式：AUTO/COMBAT/SURVIVAL/EXPLORATION/IDLE（现有 AUTO/COMBAT/IDLE/LLM）。
- 规则：AUTO 依据订单/威胁推断；迟滞 + 最小驻留避免抖动。
- 清理：切换时停止不兼容排他动作；保留维持类并行动作（HEAL）。

## 感知与记忆
- 感知：一次 tick 汇总（目标、距离、药效、血量/护盾、危险）。
- 记忆：Blackboard + TTL，不持久化（重登自动重建）。

## 行为仲裁
- 排他：force_fight/guard/explore_path；并发：heal/buff/loot/signal。
- 评分：WeightedUtilityScorer + 迟滞；BudgetPolicy 限制每 tick 状态变更/规划数。

## 时间与冷却
- MultiCooldown：键空间 `brain.*`、`action.*`；统一 clamp。
- ActionScheduler：统一 TickOps/DelayedTaskScheduler；禁止 while 与手写计时。

## 命令与 UI
- 指令：/soul brain get|set|prefs；LLM/Combat 意图桥接。
- UI：ModernUI 展示模式、主/并行动作、关键冷却与资源。

## 集成
- DoT/Reaction：DoTManager.schedulePerSecond + typeId（DoTTypes），ReactionRegistry.preApplyDoT。
- 资源/链接/护盾：ResourceOps/LedgerOps/AbsorptionHelper。
- 导航：Baritone 计划 + 回退，重规划限速与阈值。

## 里程碑
- M1：稳定化 AUTO/COMBAT/IDLE（评分器+迟滞+驻留；HEAL 阈值与冷却；基础日志）。
- M2：Survival（规避/撤退；安全窗口；与 HEAL 并发）。
- M3：Exploration（巡逻/拾取；战斗切换）。
- M4：命令与 UI（get/set/prefs + 面板）。
- M5：持久化与回归（模式/偏好；压力/性能）。

## 本次（T0）落地
- 新增骨架（纯新增，不接线）：
  - scoring/ScoreInputs.java、WeightedUtilityScorer.java
  - policy/ResidencePolicy.java、HysteresisPolicy.java、BudgetPolicy.java
  - arbitration/Arbitrator.java
  - model/BrainPrefs.java、Blackboard.java
- 验收：编译/测试通过，无对现有逻辑的影响。

