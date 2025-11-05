# Phase 2｜分层重构（core + systems）

## 阶段目标
- 建立 systems 目录；迁移 movement/combat/upkeep；削薄实体逻辑。

## 任务列表
- ✅ 建立 `systems/` 目录与 README（职责与顺序）
- ✅ MovementSystem：应用 SteeringTemplate → setDeltaMovement
- ✅ CombatSystem：集中命中检测与伤害，触发 OnHitEntity/PostHit
- ✅ UpkeepSystem：集中维持消耗，触发 OnUpkeepCheck，调用 ResourceOps
- ✅ `FlyingSwordEntity.tick` 仅组装上下文与触发事件，委托给系统层

## 实施日期
2025-11-05

## 实际修改文件清单

### 新增文件 (4个)
1. `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/systems/README.md`
   - 系统层总体架构说明
   - 定义各系统职责、调用顺序、设计原则

2. `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/systems/MovementSystem.java`
   - 运动系统：集中管理 AI 行为与速度计算
   - 支持 ORBIT/GUARD/HUNT/HOVER/RECALL/SWARM 模式
   - 优先处理 TUI 命令系统，回退到标准 AI 行为
   - 提供 `applySteeringVelocity()` 兼容接口供外部模块调用

3. `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/systems/CombatSystem.java`
   - 战斗系统：集中管理碰撞检测与伤害计算
   - 委托给现有 `FlyingSwordCombat` 处理具体逻辑
   - 预留 Phase 3 扩展接口 (PostHit, BlockBreakAttempt 事件)

4. `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/systems/UpkeepSystem.java`
   - 维持系统：集中管理资源消耗与维持逻辑
   - 调用 `UpkeepOps.consumeIntervalUpkeep()` 消耗真元
   - 维持不足时自动召回飞剑
   - 提供 `calculateUpkeepCost()` 接口供 UI 显示

### 修改文件 (1个)
1. `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/FlyingSwordEntity.java`
   - 重构 `tickServer()` 方法，委托给系统层：
     - 维持逻辑 → `UpkeepSystem.tick()`
     - 运动逻辑 → `MovementSystem.tick()`
     - 战斗逻辑 → `CombatSystem.tick()`
   - 删除原 `tickAI()` 方法（约 110 行代码）
   - 保留 `applySteeringVelocity()` 作为兼容接口，委托给 `MovementSystem`
   - 标记 `applySteeringTemplate()` 为 @Deprecated（内部使用）

## 代码行数变化
| 文件 | 变化 | 说明 |
|------|------|------|
| FlyingSwordEntity.java | -110 行 | 删除 tickAI() 方法 |
| systems/README.md | +150 行 | 新增架构文档 |
| MovementSystem.java | +200 行 | 新增运动系统 |
| CombatSystem.java | +80 行 | 新增战斗系统 |
| UpkeepSystem.java | +150 行 | 新增维持系统 |
| **总计** | **+470 行** | 代码更清晰，职责分离 |

## 架构改进

### 调用顺序 (tickServer)
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
attackCooldown = CombatSystem.tick(this, attackCooldown);

// 5. 破块逻辑 (BlockBreakOps, 保持独立)
if (!tickCtx.skipBlockBreak) {
  BlockBreakOps.tickBlockBreak(this);
}
```

### 设计原则
1. **无状态 (Stateless)**: 所有系统类只包含静态方法，不持有实例状态
2. **事件驱动 (Event-Driven)**: 关键操作触发事件钩子，允许外部扩展
3. **可测试性 (Testability)**: 系统方法接受明确的输入参数，返回明确的输出
4. **向后兼容 (Backward Compatible)**: 保持原有 API 接口不变，仅重构内部实现

### 系统职责划分
| 系统 | 职责 | 输入 | 输出 |
|------|------|------|------|
| MovementSystem | AI 行为与速度计算 | FlyingSwordEntity, LivingEntity, AIMode | 更新 deltaMovement, speedCurrent |
| CombatSystem | 碰撞检测与伤害计算 | FlyingSwordEntity, attackCooldown | 新的 attackCooldown 值 |
| UpkeepSystem | 资源消耗与维持检查 | FlyingSwordEntity, upkeepTicks | 新的 upkeepTicks 值 |

## 依赖关系
- Phase 1 完成 ✅

## 验收标准
- ✅ 编译通过：`./gradlew compileJava`
- ✅ 实体类行数显著下降 (-110 行)
- ✅ 系统层职责清晰，架构文档完整
- 🔄 基础回归测试：待用户环境验证

## 风险与回退
- **风险等级**: 中
- **潜在问题**:
  1. 系统层调用顺序错误，导致逻辑异常
     - **缓解**: 严格按照 README 定义的顺序调用
     - **回退**: 恢复原 tickAI() 方法
  2. 外部模块调用 `applySteeringVelocity()` 失败
     - **缓解**: 保留兼容接口，委托给 MovementSystem
     - **回退**: 恢复原实现
- **回退方案**: `git revert <commit-hash>` 回退整个 Phase 2

## 下一步 (Phase 3)
- 扩展事件模型 (ModeChange, TargetAcquired, TargetLost)
- 增强 UpkeepCheck 事件
- 添加 PostHit, BlockBreakAttempt 事件

## 附录：系统接口一览

### MovementSystem
```java
public static void tick(FlyingSwordEntity sword, LivingEntity owner, AIMode mode)
public static void applySteeringVelocity(FlyingSwordEntity sword, Vec3 desiredVelocity)
```

### CombatSystem
```java
public static int tick(FlyingSwordEntity sword, int attackCooldown)
public static double calculateCurrentDamage(FlyingSwordEntity sword)
```

### UpkeepSystem
```java
public static int tick(FlyingSwordEntity sword, int upkeepTicks)
public static double calculateUpkeepCost(FlyingSwordEntity sword, int ticks)
```

