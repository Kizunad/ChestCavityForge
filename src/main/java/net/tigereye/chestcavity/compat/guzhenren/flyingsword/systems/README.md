# Flying Sword Systems (飞剑系统层)

## 概述 (Overview)

本目录包含飞剑的核心系统模块，遵循**单一职责原则**和**事件驱动架构**。

每个系统负责一个明确的职责领域，实体类仅作为数据载体和事件触发点。

## 系统职责划分 (System Responsibilities)

### 1. MovementSystem (运动系统)
**文件**: `MovementSystem.java`

**职责**:
- 应用转向模板 (SteeringTemplate) 计算速度
- 调用 `setDeltaMovement()` 更新实体运动
- 处理速度平滑与插值
- 触发运动相关事件 (OnMoveStart, OnMoveEnd)

**输入**:
- AIContext: AI 上下文
- IntentResult: 意图结果 (包含轨迹类型)
- FlyingSwordEntity: 飞剑实体

**输出**:
- 更新飞剑实体的 `deltaMovement` (速度向量)
- 更新 `speedCurrent` 数据同步器

---

### 2. CombatSystem (战斗系统)
**文件**: `CombatSystem.java`

**职责**:
- 集中管理碰撞检测
- 计算伤害 (速度² 公式)
- 触发战斗事件 (OnHitEntity, PostHit)
- 管理攻击冷却

**输入**:
- FlyingSwordEntity: 飞剑实体

**输出**:
- 对目标造成伤害
- 触发粒子/音效

**冷却管理（Phase 4 以后）**:
- 攻击冷却统一由主人附件 `MultiCooldown` 管理，Key 规范为 `cc:flying_sword/<uuid>/attack`；
- 通过 `FlyingSwordCooldownOps` 读写与递减冷却；
- `CombatSystem.tick(...)` 不再接收/返回冷却值。

**替代模块**:
- 原 `FlyingSwordCombat.tickCollisionAttack()` 逻辑整合到此处

---

### 3. UpkeepSystem (维持系统)
**文件**: `UpkeepSystem.java`

**职责**:
- 检查维持消耗间隔
- 调用 ResourceOps 消耗真元
- 触发 OnUpkeepCheck 事件
- 处理维持不足的回调 (召回/消散)

**输入**:
- FlyingSwordEntity: 飞剑实体
- 当前 upkeepTicks 计数

**输出**:
- 消耗玩家真元
- 返回新的 upkeepTicks 值
- 若维持不足，触发召回

**替代模块**:
- 原 `FlyingSwordEntity.tickServer()` 中的维持逻辑

---

## 系统调用顺序 (Execution Order)

在 `FlyingSwordEntity.tickServer()` 中，系统按以下顺序调用：

```java
// 1. 触发 Tick 事件钩子
TickContext tickCtx = ...;
FlyingSwordEventRegistry.fireTick(tickCtx);

// 2. 维持系统 (UpkeepSystem)
if (!tickCtx.skipUpkeep) {
  upkeepTicks = UpkeepSystem.tick(this, upkeepTicks);
  if (isRemoved()) return; // 维持不足，已召回
}

// 3. 运动系统 (MovementSystem)
if (!tickCtx.skipAI) {
  MovementSystem.tick(this, owner, getAIMode());
}

// 4. 战斗系统 (CombatSystem)
CombatSystem.tick(this); // 冷却递减与设置由 FlyingSwordCooldownOps + MultiCooldown 处理

// 5. 破块逻辑 (BlockBreakOps, 保持独立)
if (!tickCtx.skipBlockBreak) {
  BlockBreakOps.tickBlockBreak(this);
}
```

---

## 设计原则 (Design Principles)

### 1. 无状态 (Stateless)
所有系统类只包含静态方法，不持有实例状态。状态全部存储在实体或上下文对象中。

### 2. 可测试性 (Testability)
系统方法接受明确的输入参数，返回明确的输出，便于单元测试。

### 3. 事件驱动 (Event-Driven)
关键操作触发事件钩子，允许外部模块订阅和扩展行为。

### 4. 向后兼容 (Backward Compatible)
保持原有 API 接口不变，仅重构内部实现。

---

## 迁移路径 (Migration Path)

### Phase 2 (当前阶段)
- ✅ 创建 systems/ 目录与 README
- 🔄 实现 MovementSystem
- 🔄 实现 CombatSystem
- 🔄 实现 UpkeepSystem
- 🔄 重构 FlyingSwordEntity.tickServer() 使用新系统

### Phase 3 (下一阶段)
- 扩展事件模型 (ModeChange, TargetAcquired, TargetLost)
- 增强 UpkeepCheck 事件
- 添加 PostHit, BlockBreakAttempt 事件

### Phase 4 (后续阶段)
- 统一冷却管理 (MultiCooldown)
- 统一资源操作 (ResourceOps 增强)
- 失败策略可配置

---

## 依赖关系 (Dependencies)

```
FlyingSwordEntity
    ↓
Systems Layer (无状态)
    ├─ MovementSystem → SteeringOps, Trajectories
    ├─ CombatSystem → FlyingSwordCalculator, EventRegistry
    └─ UpkeepSystem → ResourceOps, EventRegistry
```

---

## 注意事项 (Notes)

1. **性能优化**: 所有系统方法设计为低开销，避免不必要的对象分配
2. **线程安全**: 系统在服务端单线程调用，无需同步
3. **错误处理**: 系统内部使用 try-catch 防御，不向上抛异常
4. **日志规范**: 使用 LOGGER.debug/warn，避免 info 级别污染

---

## 相关文档 (Related Docs)

- 总体规划: `docs/FLYINGSWORD_MASTER_PLAN.md`
- Phase 2 详细任务: `docs/stages/PHASE_2.md`
- 事件系统: `events/README.md`
- 轨迹系统: `ai/trajectory/README.md`
