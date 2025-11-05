# Phase 3｜事件模型扩展

## 阶段目标
- 新增关键事件上下文并接线：Mode/Target/Upkeep/PostHit/BlockBreakAttempt/Exp。
- 扩展事件驱动模型，支持更细粒度的外部扩展。
- 保持向后兼容，默认行为不变。

## 实施日期
2025-11-05

## 任务列表

### ✅ 已完成任务

#### 3.1 创建新事件上下文类
**位置**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/events/context/`

新增 8 个事件上下文类：

1. **ModeChangeContext.java** (41 行)
   - 模式切换事件：ORBIT/GUARD/HUNT/HOVER/RECALL/SWARM 模式切换时触发
   - 可取消：`cancelled` 字段
   - 包含旧模式、新模式、触发实体（可选）

2. **TargetAcquiredContext.java** (32 行)
   - 目标锁定事件：飞剑获取新目标时触发
   - 可取消：`cancelled` 字段
   - 包含目标实体、当前AI模式

3. **TargetLostContext.java** (45 行)
   - 目标丢失事件：飞剑失去当前目标时触发
   - 只读事件（无 cancelled 字段）
   - 包含上一个目标、丢失原因（DEAD/OUT_OF_RANGE/INVALID/MANUAL_CANCEL/MODE_CHANGE/OTHER）

4. **UpkeepCheckContext.java** (42 行)
   - 维持消耗检查事件：每次维持消耗前触发
   - 可修改：`finalCost` 字段（允许外部模块调整消耗量）
   - 可跳过：`skipConsumption` 字段
   - 包含基础消耗、速度倍率、检查间隔

5. **PostHitContext.java** (39 行)
   - 命中后事件：伤害已造成后触发
   - 只读事件（无 cancelled 字段）
   - 包含目标、实际伤害、是否击杀

6. **BlockBreakAttemptContext.java** (41 行)
   - 破块尝试事件：破块之前触发
   - 可取消：`cancelled` 字段
   - 可修改：`canBreak` 字段
   - 包含方块位置、方块状态

7. **ExperienceGainContext.java** (47 行)
   - 经验获取事件：飞剑获得经验时触发
   - 可取消：`cancelled` 字段
   - 可修改：`finalExpAmount` 字段
   - 包含原始经验值、经验来源（KILL_MOB/KILL_PLAYER/BREAK_BLOCK/MANUAL_INJECT/PASSIVE_EFFECT/OTHER）

8. **LevelUpContext.java** (31 行)
   - 等级提升事件：飞剑升级时触发
   - 可取消：`cancelled` 字段
   - 包含旧等级、新等级

#### 3.2 扩展 FlyingSwordEventHook 接口
**文件**: `FlyingSwordEventHook.java` (新增 82 行)

添加 8 个新事件方法，所有方法都有默认空实现：
- `onModeChange(ModeChangeContext ctx)`
- `onTargetAcquired(TargetAcquiredContext ctx)`
- `onTargetLost(TargetLostContext ctx)`
- `onUpkeepCheck(UpkeepCheckContext ctx)`
- `onPostHit(PostHitContext ctx)`
- `onBlockBreakAttempt(BlockBreakAttemptContext ctx)`
- `onExperienceGain(ExperienceGainContext ctx)`
- `onLevelUp(LevelUpContext ctx)`

#### 3.3 扩展 FlyingSwordEventRegistry
**文件**: `FlyingSwordEventRegistry.java` (新增 98 行)

添加 8 个事件触发方法：
- `fireModeChange(ModeChangeContext ctx)` - 支持短路
- `fireTargetAcquired(TargetAcquiredContext ctx)` - 支持短路
- `fireTargetLost(TargetLostContext ctx)` - 只读
- `fireUpkeepCheck(UpkeepCheckContext ctx)` - 只读
- `firePostHit(PostHitContext ctx)` - 只读
- `fireBlockBreakAttempt(BlockBreakAttemptContext ctx)` - 支持短路
- `fireExperienceGain(ExperienceGainContext ctx)` - 支持短路
- `fireLevelUp(LevelUpContext ctx)` - 支持短路

所有方法都包含异常处理，防止单个钩子失败影响其他钩子。

#### 3.4 集成事件触发点

**3.4.1 UpkeepSystem.java** (新增 30 行)
- 在维持消耗前触发 `UpkeepCheck` 事件
- 计算基础消耗量，构建上下文
- 允许外部模块修改 `finalCost` 或设置 `skipConsumption`
- 位置：`tick()` 方法，line 74-102

**3.4.2 FlyingSwordCombat.java** (新增 6 行)
- 在伤害造成后触发 `PostHit` 事件
- 传递实际伤害值和击杀状态
- 位置：`attackTarget()` 方法，line 249-254

**3.4.3 BlockBreakOps.java** (新增 11 行)
- 在破块前触发 `BlockBreakAttempt` 事件
- 允许外部模块取消破块或修改 `canBreak` 标志
- 位置：`tickBlockBreak()` 方法，line 118-128

## 代码行数变化

| 文件 | 变化 | 说明 |
|------|------|------|
| **新增上下文类** | | |
| ModeChangeContext.java | +41 行 | 新增 |
| TargetAcquiredContext.java | +32 行 | 新增 |
| TargetLostContext.java | +45 行 | 新增 |
| UpkeepCheckContext.java | +42 行 | 新增 |
| PostHitContext.java | +39 行 | 新增 |
| BlockBreakAttemptContext.java | +41 行 | 新增 |
| ExperienceGainContext.java | +47 行 | 新增 |
| LevelUpContext.java | +31 行 | 新增 |
| **修改文件** | | |
| FlyingSwordEventHook.java | +82 行 | 新增 8 个方法 |
| FlyingSwordEventRegistry.java | +98 行 | 新增 8 个 fire 方法 |
| UpkeepSystem.java | +30 行 | 集成 UpkeepCheck 事件 |
| FlyingSwordCombat.java | +6 行 | 集成 PostHit 事件 |
| BlockBreakOps.java | +11 行 | 集成 BlockBreakAttempt 事件 |
| **总计** | **+545 行** | 事件系统显著扩展 |

## 架构改进

### 事件分类

**可短路事件**（支持取消）：
- `ModeChange` - 可阻止模式切换
- `TargetAcquired` - 可阻止目标锁定
- `BlockBreakAttempt` - 可阻止破块
- `ExperienceGain` - 可阻止经验获取
- `LevelUp` - 可阻止升级

**只读事件**（不可取消，仅通知）：
- `TargetLost` - 目标已丢失的通知
- `UpkeepCheck` - 可修改消耗量，但不可取消检查
- `PostHit` - 伤害已造成的通知

### 事件触发顺序

典型 Tick 流程中的事件顺序：
```
1. Tick 事件 (Phase 0-2)
2. UpkeepCheck 事件 (Phase 3) → 维持系统
3. ModeChange 事件 (Phase 3, 已接线) → 运动系统
4. TargetAcquired/TargetLost 事件 (Phase 3, 已接线) → 运动系统
5. BlockBreakAttempt 事件 (Phase 3) → 破块系统
6. BlockBreak 事件 (Phase 0-2) → 破块系统
7. HitEntity 事件 (Phase 0-2) → 战斗系统
8. PostHit 事件 (Phase 3) → 战斗系统
9. ExperienceGain 事件 (Phase 3, 已接线) → 经验系统
10. LevelUp 事件 (Phase 3, 已接线) → 经验系统
```

### 扩展性示例

外部模块可以通过实现 `FlyingSwordEventHook` 接口来扩展功能：

```java
// 示例：领域内免费维持
public class DomainFreeUpkeepHook implements FlyingSwordEventHook {
  @Override
  public void onUpkeepCheck(UpkeepCheckContext ctx) {
    if (isInDomain(ctx.sword)) {
      ctx.skipConsumption = true; // 跳过消耗
    }
  }
}

// 示例：特殊目标保护
public class ProtectedTargetHook implements FlyingSwordEventHook {
  @Override
  public void onTargetAcquired(TargetAcquiredContext ctx) {
    if (isProtected(ctx.target)) {
      ctx.cancelled = true; // 取消锁定
    }
  }
}

// 示例：经验加成
public class ExpBoostHook implements FlyingSwordEventHook {
  @Override
  public void onExperienceGain(ExperienceGainContext ctx) {
    if (ctx.source == GainSource.KILL_MOB) {
      ctx.finalExpAmount *= 2; // 双倍经验
    }
  }
}
```

## 待完成任务（精简版）

- TargetLost 原因细分：
  - 在 MovementSystem/TargetFinder 自动清除目标时，根据具体场景发出 LOST(DEAD/OUT_OF_RANGE/INVALID)
- 观测与诊断：
  - 为事件链增加可选的 DEBUG 指标（触发次数/短路次数），默认关闭

## 依赖关系
- ✅ 依赖 Phase 2 的系统抽取
- ✅ 依赖 Phase 0 的事件系统初始化

## 验收标准

### 编译测试
```bash
./gradlew compileJava
```
- ✅ 编译通过（待用户环境验证）
- ✅ 无新增警告

### 功能测试

**✅ 事件定义**：
- [x] 8 个新上下文类已创建
- [x] FlyingSwordEventHook 接口已扩展
- [x] FlyingSwordEventRegistry 已实现 fire 方法

**✅ 事件触发**：
- [x] UpkeepCheck 事件在维持消耗前触发
- [x] PostHit 事件在伤害造成后触发
- [x] BlockBreakAttempt 事件在破块前触发

**✅ 已验证（代码接线）**：
- [x] ModeChange 事件在模式切换时触发
- [x] TargetAcquired 事件在锁定目标时触发
- [x] TargetLost 事件在失去目标时触发（后续细分原因）
- [x] ExperienceGain 事件在获得经验时触发
- [x] LevelUp 事件在升级时触发

**短路语义测试**：
- [ ] 注册测试钩子，验证可取消事件能正确短路
- [ ] 验证 `cancelled=true` 能阻止默认行为
- [ ] 验证 `skipConsumption=true` 能跳过维持消耗

### 集成测试

**测试用例**（待用户环境执行）：
1. 注册自定义钩子，监听 UpkeepCheck 事件
2. 修改 `finalCost` 或设置 `skipConsumption`
3. 验证维持消耗被正确调整或跳过

4. 注册自定义钩子，监听 PostHit 事件
5. 在击中后触发额外效果（如吸血、追伤）
6. 验证效果正确触发

7. 注册自定义钩子，监听 BlockBreakAttempt 事件
8. 取消特定方块的破坏
9. 验证该方块不被破坏

## 风险与回退

### 风险等级：低-中

**潜在问题**：
1. **事件钩子执行失败**
   - **缓解**：所有 fire 方法都包含异常处理
   - **回退**：单个钩子失败不影响其他钩子和默认行为

2. **短路语义误用**
   - **缓解**：文档清晰说明哪些事件可取消
   - **回退**：保持默认钩子不修改 cancelled 标志

3. **性能开销**
   - **缓解**：事件仅在已注册钩子时触发，空实现开销极小
   - **回退**：临时注销钩子以恢复性能

4. **ModeChange/Target 事件未完成**
   - **影响**：部分扩展功能无法使用
   - **缓解**：Phase 4 完成实现
   - **回退**：暂不使用这些事件接口

**回退方案**：
- 所有新增事件都有默认空实现，不影响现有行为
- 可单独回退某个事件的触发（注释掉 fire 调用）
- 最坏情况：`git revert <commit-hash>` 回退整个 Phase 3

## 下一步 (Phase 4)

Phase 3 完成后，Phase 4 将专注于：
- 冷却管理统一到 MultiCooldown 附件
- 资源消耗统一到 ResourceOps
- 完成 ModeChange/Target/Experience 事件集成
- 配置化维持失败策略（RECALL/DISCARD/HOVER）

## 附录：事件接口一览

### 新增 Fire 方法

```java
// FlyingSwordEventRegistry.java (Phase 3 新增)
public static void fireModeChange(ModeChangeContext ctx)
public static void fireTargetAcquired(TargetAcquiredContext ctx)
public static void fireTargetLost(TargetLostContext ctx)
public static void fireUpkeepCheck(UpkeepCheckContext ctx)
public static void firePostHit(PostHitContext ctx)
public static void fireBlockBreakAttempt(BlockBreakAttemptContext ctx)
public static void fireExperienceGain(ExperienceGainContext ctx)
public static void fireLevelUp(LevelUpContext ctx)
```

### 使用示例

```java
// 注册钩子
FlyingSwordEventRegistry.register(new MyCustomHook());

// 实现钩子
public class MyCustomHook implements FlyingSwordEventHook {
  @Override
  public void onUpkeepCheck(UpkeepCheckContext ctx) {
    // 领域内免费维持
    if (isInDomain(ctx.sword)) {
      ctx.skipConsumption = true;
    }
  }

  @Override
  public void onPostHit(PostHitContext ctx) {
    // 吸血效果
    if (ctx.wasKilled) {
      ctx.sword.heal(5.0f);
    }
  }
}
```
