# Hun Dao Phase 2.1 问题修复计划

## 制定时间
- 2025-11-17

## 背景
Phase 2 合并后暴露出多个阻塞问题：
1. `HunDaoSoulBeastBehavior` 仍在 `onSlowTick` 中执行魂魄泄露，和新加入的 `HunPoDrainScheduler` 重复扣减。
2. 行为层没有真正接入 `HunDaoRuntimeContext`，状态机与调度器无法被业务代码消费。
3. 新增的 `HunDaoSoulState` 仅注册为 Attachment，实际逻辑没有读写，持久化数据全部空置。

为保证 Phase 2 目标达成，需要在 Phase 2.1 中集中修复上述问题。

## 目标
- 移除重复魂魄泄露，确保调度器成为唯一的被动扣魂来源。
- 让行为层实质使用 `HunDaoRuntimeContext`/`HunDaoStateMachine`，实现真正的依赖倒置。
- 将魂焰与魂兽统计写入 `HunDaoSoulState`，保证持久化生效。

## 任务清单

### 任务 1：修复魂魄泄露重复扣减
- [ ] 审核所有慢速心跳逻辑，移除 `resourceOps.leakHunpoPerSecond()` 的重复调用。
- [ ] 在 `HunDaoSoulBeastBehavior` 中加入注释，说明泄露由 `HunPoDrainScheduler` 接管。
- [ ] 复测魂兽化状态，确认每秒仅扣除 `HunDaoTuning.SoulBeast.HUNPO_LEAK_PER_SEC`。

### 任务 2：接入 HunDaoRuntimeContext
- [ ] 在 `HunDaoSoulBeastBehavior` 中获取 `HunDaoRuntimeContext`，替换手工保存的 `HunDaoOpsAdapter.INSTANCE`。
- [ ] 所有资源/特效/通知调用改为 `context.getResourceOps()`、`context.getFxOps()`、`context.getNotificationOps()`。
- [ ] 使用 `context.getStateMachine()` 查询/修改魂兽状态，保证状态机发挥作用。

### 任务 3：启用 HunDaoSoulState 持久化
- [ ] 在攻击命中逻辑中，通过 `targetContext.getOrCreateSoulState()` 写入魂焰剩余 tick 与 DPS。
- [ ] 在魂兽化激活流程中，记录激活次数、持续时间等统计数据。
- [ ] 确保 `HunDaoSoulState` 的各项 setter 在业务流程中至少被一次调用。

## 验收标准
1. `HunDaoSoulBeastBehavior.onSlowTick` 不再调用 `leakHunpoPerSecond`，日志验证仅 `HunPoDrainScheduler` 扣魂。
2. 运行时上下文在行为层被实际使用（代码中有 `HunDaoRuntimeContext.get()`；`getStateMachine()`/`getResourceOps()` 调用可见）。
3. `HunDaoSoulState` 的字段被读写，`targetContext.getOrCreateSoulState()`、`setSoulFlameRemainingTicks()`、`incrementSoulBeastActivationCount()` 等方法有实际调用。

## 交付物
- `HunDaoSoulBeastBehavior.java` 重构补丁
- 验证日志/手动测试记录（附在 Phase2.1_Resolution）
- 更新 `Phase2_Report.md` 说明 Phase 2.1 修复内容

