# 飞剑模块重构执行计划

## 目标概述

将 flyingsword 系统从当前的"全功能集成"状态重构为"核心功能 + 可选扩展"的模块化架构，实现：
1. **可维护性提升**：通过功能开关和分层设计降低复杂度
2. **可扩展性增强**：事件驱动模型支持外部系统无侵入式接入
3. **性能优化**：精简不必要的复杂轨迹和意图计算
4. **代码质量**：补充文档和测试，统一冷却与资源管理

## 当前架构分析

### 优点
- ✅ 已有基本分层（ai/, motion/, ops/, integration/）
- ✅ Intent/Trajectory 解耦良好
- ✅ Calculator 使用纯函数设计
- ✅ 事件系统框架已存在（events/FlyingSwordEventRegistry）

### 问题
- ❌ **无功能开关**：所有复杂功能（16种轨迹、16个意图、Swarm、Gecko）默认启用
- ❌ **Intent 过载**：HUNT 模式有 11 个意图，GUARD 有 4 个
- ❌ **旧式 Goal 系统未移除**：ForceHuntTargetGoal、SwordGoalOps 仍存在
- ❌ **实体逻辑过重**：FlyingSwordEntity 包含大量 tick/hurt/interact 逻辑
- ❌ **冷却管理分散**：实体级字段 + owner 附件不一致
- ❌ **缺乏文档和测试**：系统复杂度高但缺少测试覆盖

---

## Phase 0｜接线与开关 🔌

**目标**：添加配置开关，为后续裁剪做准备

### 任务清单

#### 0.1 在 FlyingSwordTuning.java 添加布尔开关
```java
// 在 FlyingSwordTuning.java 添加：
/** 启用高级轨迹（保留：Orbit, PredictiveLine, CurvedIntercept） */
public static final boolean ENABLE_ADVANCED_TRAJECTORIES = false;

/** 启用额外意图（每个模式最多保留 2 条基础意图） */
public static final boolean ENABLE_EXTRA_INTENTS = false;

/** 启用青莲剑群系统（QingLianSwordSwarm） */
public static final boolean ENABLE_SWARM = false;

/** 启用剑引蛊 TUI（SwordCommandTUI） */
public static final boolean ENABLE_TUI = false;

/** 启用 Gecko 模型覆盖与视觉档案 */
public static final boolean ENABLE_GEO_OVERRIDE_PROFILE = false;
```

#### 0.2 确保事件系统初始化
- 在模组主类初始化路径中调用 `FlyingSwordEventInit.init()`
- 验证默认钩子已注册（查看 FlyingSwordEventInit.java:22）

#### 0.3 控制 Gecko/视觉档加载器注册
- 在 ChestCavity 主类中，将 `SwordVisualProfileLoader`、`SwordModelOverrideLoader` 的注册用 `ENABLE_GEO_OVERRIDE_PROFILE` 包裹

**验证**：`./gradlew compileJava` 通过

---

## Phase 1｜裁剪（先停用后清理）✂️

**目标**：通过开关精简到核心功能，高级功能默认关闭

### 任务清单

#### 1.1 精简 Trajectories.java 轨迹注册
**保留基础轨迹**（默认启用）：
- ✅ `Orbit`：环绕
- ✅ `PredictiveLine`：预测直线
- ✅ `CurvedIntercept`（可选）：曲线拦截

**高级轨迹**（受 `ENABLE_ADVANCED_TRAJECTORIES` 控制）：
- Boomerang, Corkscrew, BezierS, Serpentine
- VortexOrbit, Sawtooth, PetalScan, WallGlide
- ShadowStep, DomainEdgePatrol, Ricochet, HelixPair, PierceGate

**实现方式**：
```java
static {
    // 基础轨迹（总是注册）
    register(TrajectoryType.Orbit, ...);
    register(TrajectoryType.PredictiveLine, ...);

    // 高级轨迹（条件注册）
    if (FlyingSwordTuning.ENABLE_ADVANCED_TRAJECTORIES) {
        register(TrajectoryType.Boomerang, ...);
        register(TrajectoryType.Corkscrew, ...);
        // ...其他高级轨迹
    }
}
```

#### 1.2 精简 IntentPlanner.java 意图列表
**当前状态**：
- ORBIT: 3 intents (PatrolIntent, HoldIntent, SweepSearchIntent)
- GUARD: 4 intents
- HUNT: 11 intents ❌ **过多！**
- HOVER: 2 intents
- RECALL: 1 intent

**目标配置**：
```java
// 基础意图（默认启用）
case ORBIT -> {
    list.add(new HoldIntent());
    list.add(new PatrolIntent());
}
case GUARD -> {
    list.add(new GuardIntent());
    list.add(new InterceptIntent());
}
case HUNT -> {
    list.add(new FocusFireIntent());
    list.add(new DuelIntent());
    if (FlyingSwordTuning.ENABLE_EXTRA_INTENTS) {
        list.add(new AssassinIntent());
        list.add(new BreakerIntent());
        list.add(new SuppressIntent());
        // ...其他意图
    }
}
```

#### 1.3 移除旧式 Goal 追击路径
**删除文件**：
- `ai/goal/ForceHuntTargetGoal.java`
- `util/behavior/SwordGoalOps.java`（如果存在）

**原因**：已被 Intent 系统完全替代

#### 1.4 精简 SwordCommandCenter.java
**精简策略**：
- 核心指令保留：目标标记、战术切换、编队控制
- TUI 相关代码受 `ENABLE_TUI` 控制
- `CommandTactic` 精简为 2-3 个基础战术（FOCUS_FIRE, HOLD_POSITION, DEFENSIVE）

#### 1.5 Swarm 系统设为可选
**条件编译**：
- `ai/swarm/QingLianSwordSwarm.java` 相关调度受 `ENABLE_SWARM` 控制
- 为 `domain/impl/qinglian/QingLianDomain.java` 提供降级路径：
  ```java
  if (FlyingSwordTuning.ENABLE_SWARM) {
      // 启用集群行为
  } else {
      // 降级为普通 GUARD 模式
  }
  ```

#### 1.6 RepairOps 和维护命令设为 dev-only
- `ops/RepairOps.java` 的公开方法添加 `@Deprecated` 或 dev-only 注释
- 命令集中的维护子命令受开发模式开关控制

**验证**：
- 所有开关默认关闭时，`./gradlew compileJava` 和 `./gradlew build` 通过
- 手动测试基础功能（Orbit, Guard, Hunt）

---

## Phase 2｜分层重构 🏗️

**目标**：从 "domain 概念" 迁移到 `core + systems` 架构

### 任务清单

#### 2.1 建立 core/ 包结构
**新建目录结构**：
```
flyingsword/
├── core/
│   ├── entity/
│   │   └── FlyingSwordEntity.java       (移动自根目录)
│   ├── types/
│   │   ├── FlyingSwordAttributes.java   (移动自根目录)
│   │   ├── FlyingSwordType.java         (移动自根目录)
│   │   └── FlyingSwordTypePresets.java  (移动自根目录)
│   ├── controller/
│   │   ├── FlyingSwordController.java   (移动自根目录)
│   │   └── FlyingSwordSpawner.java      (移动自根目录)
│   └── storage/
│       └── FlyingSwordStorage.java      (移动自根目录)
```

**迁移步骤**：
1. 创建 core/ 子包
2. 移动文件并更新 package 声明
3. 全局更新 import 语句

#### 2.2 建立 systems/ 目录
**新建职责系统**：
```
flyingsword/
├── systems/
│   ├── README.md                        (系统职责说明)
│   ├── movement/
│   │   ├── MovementSystem.java          (从 Entity.tick 提取)
│   │   └── SteeringExecutor.java
│   ├── combat/
│   │   ├── CombatSystem.java            (整合 FlyingSwordCombat)
│   │   └── DamageCalculator.java        (委托给 FlyingSwordCalculator)
│   ├── defense/
│   │   └── DefenseSystem.java           (处理 Entity.hurt 逻辑)
│   ├── blockbreak/
│   │   └── BlockBreakSystem.java        (整合 BlockBreakOps)
│   ├── targeting/
│   │   └── TargetingSystem.java         (目标验证与切换)
│   ├── progression/
│   │   └── ExperienceSystem.java        (经验与升级)
│   └── lifecycle/
│       ├── UpkeepSystem.java            (整合 UpkeepOps)
│       └── RecallSystem.java            (召回逻辑)
```

#### 2.3 重构 FlyingSwordEntity
**目标**：实体仅保留：
- SynchedEntityData 定义
- 上下文组装（owner, target, attributes）
- 事件触发入口

**逻辑移交**：
```java
// 旧代码：
public void tick() {
    // 100+ 行复杂逻辑
}

// 新代码：
public void tick() {
    super.tick();

    // 触发事件
    var ctx = new TickContext(this, level(), ...);
    FlyingSwordEventRegistry.fireOnTick(ctx);

    // 委托给系统
    if (!level().isClientSide) {
        MovementSystem.tick(this);
        CombatSystem.tick(this);
        UpkeepSystem.tick(this);
    }
}
```

#### 2.4 编写 systems/README.md
**内容包括**：
- 每个系统的职责边界
- 系统间的调用顺序
- 扩展点（事件钩子）

**验证**：
- 所有现有功能测试通过
- 代码审查确认逻辑等价性

---

## Phase 3｜事件模型扩展 📡

**目标**：补充缺失的事件上下文，支持更细粒度的扩展

### 任务清单

#### 3.1 新增事件上下文
**在 events/context/ 添加**：
```java
// ModeChangeContext.java
public record ModeChangeContext(
    FlyingSwordEntity sword,
    AIMode oldMode,
    AIMode newMode,
    @Nullable LivingEntity trigger
) {}

// TargetAcquiredContext.java
public record TargetAcquiredContext(
    FlyingSwordEntity sword,
    LivingEntity target,
    AIMode mode
) {}

// TargetLostContext.java
public record TargetLostContext(
    FlyingSwordEntity sword,
    @Nullable LivingEntity lastTarget,
    LostReason reason
) {}

// UpkeepCheckContext.java
public record UpkeepCheckContext(
    FlyingSwordEntity sword,
    double baseCost,
    double speedMultiplier,
    int tickInterval
) {}

// PostHitContext.java
public record PostHitContext(
    FlyingSwordEntity sword,
    LivingEntity target,
    float damageDealt,
    boolean wasKilled
) {}

// BlockBreakAttemptContext.java
public record BlockBreakAttemptContext(
    FlyingSwordEntity sword,
    BlockPos pos,
    BlockState state,
    boolean canBreak
) {}

// ExperienceGainContext.java
public record ExperienceGainContext(
    FlyingSwordEntity sword,
    int expAmount,
    GainSource source
) {}

// LevelUpContext.java
public record LevelUpContext(
    FlyingSwordEntity sword,
    int oldLevel,
    int newLevel
) {}
```

#### 3.2 在 FlyingSwordEventRegistry 实现 fire 方法
```java
public static void fireOnModeChange(ModeChangeContext ctx) {
    for (var hook : HOOKS) {
        hook.onModeChange(ctx);
    }
}

public static void fireOnTargetAcquired(TargetAcquiredContext ctx) {
    for (var hook : HOOKS) {
        hook.onTargetAcquired(ctx);
    }
}
// ...其他事件
```

#### 3.3 在系统入口触发事件
**示例**：
```java
// MovementSystem.java
public static void tick(FlyingSwordEntity sword) {
    var oldMode = sword.getAIMode();
    // ...模式逻辑
    var newMode = computeNewMode();
    if (newMode != oldMode) {
        var ctx = new ModeChangeContext(sword, oldMode, newMode, null);
        FlyingSwordEventRegistry.fireOnModeChange(ctx);
    }
}
```

**验证**：
- 注册测试钩子，验证事件正确触发
- 确保短路语义（某些事件可被取消）

---

## Phase 4｜冷却与资源一致性 ⏱️

**目标**：统一冷却管理到 MultiCooldown 附件，规范资源检查

### 任务清单

#### 4.1 迁移实体级冷却字段到 MultiCooldown
**当前问题**：
- FlyingSwordEntity 可能有 `attackCooldown` 等字段
- Owner 附件的 MultiCooldown 未被充分利用

**统一 key 规范**：
```
cc:flying_sword/<sword_uuid>/attack
cc:flying_sword/<sword_uuid>/block_break
cc:flying_sword/<sword_uuid>/ability
```

**迁移步骤**：
1. 在 CombatSystem 中使用 `MultiCooldown.getOrCreate(owner)` 获取冷却管理器
2. 替换所有 `sword.attackCooldown` 为 `cooldown.get("cc:flying_sword/..." + sword.getUUID() + "/attack")`
3. 删除实体级冷却字段

#### 4.2 集中调用 UpkeepOps
**在 UpkeepSystem.tick() 中**：
```java
public static void tick(FlyingSwordEntity sword) {
    if (sword.tickCount % FlyingSwordTuning.UPKEEP_CHECK_INTERVAL == 0) {
        var ctx = new UpkeepCheckContext(sword, ...);
        FlyingSwordEventRegistry.fireOnUpkeepCheck(ctx);

        double cost = FlyingSwordCalculator.calculateUpkeep(ctx);
        boolean success = UpkeepOps.consumeIntervalUpkeep(sword.getOwner(), cost);

        if (!success) {
            handleUpkeepFailure(sword);
        }
    }
}
```

#### 4.3 在 FlyingSwordTuning 配置失败策略
```java
public enum UpkeepFailureStrategy {
    STALL,      // 停滞不动
    SLOW,       // 减速移动
    RECALL      // 强制召回
}

public static final UpkeepFailureStrategy UPKEEP_FAILURE_STRATEGY = UpkeepFailureStrategy.RECALL;
```

**验证**：
- 测试冷却在 owner 切换后的持久性
- 测试 upkeep 失败时的降级行为

---

## Phase 5｜客户端与网络 🎨

**目标**：优化渲染路径，减少不必要的客户端负载

### 任务清单

#### 5.1 默认渲染路径精简
**保留**：
- `FlyingSwordRenderer`（基础渲染器）
- 默认粒子效果（`DefaultFlyingSwordEntityFX`）

**条件加载**（受 `ENABLE_GEO_OVERRIDE_PROFILE` 控制）：
- Gecko 渲染器（`SwordModelObjectRenderer`）
- 模型覆盖系统（`SwordModelOverrideRegistry`）
- 视觉档案系统（`SwordVisualProfileRegistry`）

#### 5.2 检查网络消息效率
- 审计所有 `synchedEntityData` 更新频率
- 确保 Intent 副作用通过实体同步实现（避免额外数据包）
- 复用现有载荷（如在 `AI_MODE` 变化时触发客户端效果，而非单独发包）

**验证**：
- 使用调试工具监控网络流量
- 多玩家环境下测试同步延迟

---

## Phase 6｜文档与测试 📚

**目标**：补充系统文档和单元测试

### 任务清单

#### 6.1 编写系统级文档
**新建/更新文档**：
1. **flyingsword/AGENTS.md**（或更新现有）：
   - 事件模型使用指南
   - 系统职责划分
   - 冷却与资源约定
   - 扩展接口示例

2. **ai/AGENTS.md**：
   - 精简后的意图/轨迹集合
   - 开关使用方法
   - 自定义 Intent/Trajectory 教程

3. **systems/README.md**：
   - 每个系统的输入/输出
   - 系统间依赖关系图
   - 调试检查点

#### 6.2 添加单元测试
**测试覆盖目标**：
- ✅ `FlyingSwordCalculator` 的所有计算方法（已有部分测试）
- ✅ `UpkeepOps` 的资源扣减逻辑
- ✅ `SteeringOps` 的速度约束计算
- ✅ `ItemAffinityUtil` 的继承属性计算（已有测试）

**测试文件位置**：
```
src/test/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/
├── calculator/FlyingSwordCalculatorTest.java
├── integration/resource/UpkeepOpsTest.java
├── motion/SteeringOpsTest.java
└── systems/
    ├── CombatSystemTest.java
    └── UpkeepSystemTest.java
```

#### 6.3 验证编译与测试
```bash
./gradlew compileJava
./gradlew test
```

---

## Phase 7｜最终清理 🧹

**目标**：删除未使用代码，收尾优化

### 任务清单

#### 7.1 删除未引用的轨迹实现
**条件**：所有开关默认关闭且编译通过后
- 删除未被引用的高级轨迹实现文件
- 删除对应的 `TrajectoryType` 枚举项（如果未被外部引用）
- 删除 trajectory/templates/ 中的未使用模板

#### 7.2 删除旧式 Goal 路径
- `ai/goal/ForceHuntTargetGoal.java`（已在 Phase 1.3 标记）
- `util/behavior/SwordGoalOps.java`（如果存在）

#### 7.3 评估 TUI/Swarm 独立模块化
**如果长期默认关闭**：
- 考虑将 `ai/command/SwordCommandTUI.java` 迁移至独立可选模块
- 考虑将 `ai/swarm/` 迁移至示例插件
- 核心保留最小接口（`SwordCommandCenter` 保留基础 API）

#### 7.4 最终代码审查
- ✅ 所有 TODO 注释已清理或转为 Issue
- ✅ 无未使用的 import
- ✅ 所有公开 API 有 Javadoc
- ✅ 符合项目代码风格

**验证**：
```bash
./gradlew build
./gradlew test
# 游戏内全功能手动测试
```

---

## 风险评估与回退策略

### 高风险项
1. **Phase 2.3 实体重构**：可能引入行为不一致
   - **缓解**：每个系统迁移后立即测试
   - **回退**：保留旧代码作为注释，验证后再删除

2. **Phase 4.1 冷却迁移**：可能影响战斗节奏
   - **缓解**：先在测试环境验证数值一致性
   - **回退**：保留原冷却字段作为备用

### 中风险项
1. **Phase 1 功能裁剪**：可能影响现有玩家体验
   - **缓解**：开关提供配置文件覆盖（config/flyingsword.toml）
   - **回退**：临时将所有开关设为 true

### 测试检查点
- [ ] Phase 0 完成：编译通过
- [ ] Phase 1 完成：基础功能手动测试（Orbit/Guard/Hunt）
- [ ] Phase 2 完成：集成测试套件通过
- [ ] Phase 3-4 完成：事件触发测试 + 冷却持久性测试
- [ ] Phase 5 完成：多人测试（网络同步）
- [ ] Phase 6 完成：单元测试覆盖率 >70%
- [ ] Phase 7 完成：全功能回归测试

---

## 时间估算

| 阶段 | 预计工作量 | 依赖 |
|------|-----------|------|
| Phase 0 | 2-3 小时 | 无 |
| Phase 1 | 4-6 小时 | Phase 0 |
| Phase 2 | 8-12 小时 | Phase 1 |
| Phase 3 | 4-6 小时 | Phase 2 |
| Phase 4 | 4-6 小时 | Phase 3 |
| Phase 5 | 3-4 小时 | Phase 2 |
| Phase 6 | 6-8 小时 | Phase 2-5 |
| Phase 7 | 2-3 小时 | 所有 |
| **总计** | **33-48 小时** | - |

**建议执行节奏**：
- 每个 Phase 独立完成并提交
- Phase 2 可与 Phase 3-5 并行（不同子系统）
- Phase 6 贯穿整个过程（文档即时更新，测试在 Phase 2 后集中补充）

---

## 成功标准

### 技术指标
- ✅ 编译通过，无警告
- ✅ 单元测试覆盖率 ≥ 70%
- ✅ 所有开关默认关闭时，包大小减少 ≥ 20%
- ✅ 多人测试无网络同步问题

### 可维护性指标
- ✅ FlyingSwordEntity 类行数减少 ≥ 50%
- ✅ 每个 AIMode 的 Intent 数量 ≤ 2（不含开关启用）
- ✅ 系统职责文档完整（systems/README.md）

### 功能指标
- ✅ 基础功能（Orbit/Guard/Hunt）行为不变
- ✅ 外部系统（domain/rift）集成不受影响
- ✅ 事件钩子支持运行时注册
