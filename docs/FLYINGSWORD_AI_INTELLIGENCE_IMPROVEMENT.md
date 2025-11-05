# 飞剑AI智能表现改进方案

> **核心理念**：用简单、可预测的逻辑创造"看起来聪明"的行为，而不是堆砌复杂代码

## 问题诊断 🔍

### 当前系统的"假复杂"

**代码复杂度**：
- 16 个 Intent 类（许多只有 6-20 行逻辑）
- 17 种轨迹类型
- 575 行的 TargetFinder（8 个专用方法）
- HUNT 模式还在用旧的 HuntBehavior（未完成迁移）

**实际效果**：
- ❌ **不聪明**：优先级公式是硬编码的魔法数字（为什么 Duel 是 18 但 Shepherd 是 11？）
- ❌ **性能差**：每帧重复搜索相同目标（AssassinIntent 找最低血量，FocusFireIntent 也找最低血量）
- ❌ **难调试**：没有决策日志，无法理解为什么飞剑选择某个行为
- ❌ **不协作**：AIContext 只有 4 个字段，Intent 无法共享信息

### "聪明"应该是什么样的？

**玩家眼中的"智能飞剑"**：
1. **情境感知**：被围攻时散开，追击时集中
2. **预判敌人**：拦截移动目标，优先威胁单位
3. **协作配合**：多把剑分工明确（有的缠斗，有的突袭）
4. **流畅过渡**：行为切换自然，不会"抽风式"转向
5. **记忆与学习**：不会反复攻击无法伤害的目标

**关键洞察**：这些表现不需要复杂逻辑，需要的是**正确的抽象层**！

---

## 改进方案 🚀

### 方案 A：效用AI系统（Utility AI）⭐ 推荐

**原理**：用数学曲线（响应曲线）评估每个行为的"效用值"，选择最高的

**优势**：
- 📉 代码量：16 个 Intent 类 → 5-7 个效用函数 + 配置文件
- 🎯 可调试：所有权重在配置文件中，一目了然
- 🔄 灵活性：运行时可调整，支持热重载
- 🧠 "聪明"表现：多因素权衡自然产生复杂决策

#### 实现示例

**1. 定义效用评估器（UtilityEvaluator）**

```java
// flyingsword/ai/utility/UtilityEvaluator.java
public record UtilityEvaluator(
    String name,
    UtilityFunction function,
    double weight
) {
    public double evaluate(UtilityContext ctx) {
        return function.calculate(ctx) * weight;
    }
}
```

**2. 预定义响应曲线（UtilityFunction）**

```java
// flyingsword/ai/utility/UtilityFunction.java
public interface UtilityFunction {
    double calculate(UtilityContext ctx);

    // 线性曲线：越近越高
    static UtilityFunction linearInverse(String key, double max) {
        return ctx -> Math.min(max / ctx.get(key), 1.0);
    }

    // S型曲线：平滑过渡
    static UtilityFunction sigmoid(String key, double midpoint, double steepness) {
        return ctx -> 1.0 / (1.0 + Math.exp(-steepness * (ctx.get(key) - midpoint)));
    }

    // 阈值曲线：达到条件后激活
    static UtilityFunction threshold(String key, double threshold, double outputHigh, double outputLow) {
        return ctx -> ctx.get(key) >= threshold ? outputHigh : outputLow;
    }

    // 反比例曲线：距离越远效用越低
    static UtilityFunction inverseDistance(String key, double scale) {
        return ctx -> scale / Math.max(ctx.get(key), 0.1);
    }
}
```

**3. 配置文件驱动（JSON）**

```json5
// data/chestcavity/flyingsword/ai/utility_profiles.json
{
  "hunt_mode": {
    "actions": [
      {
        "name": "assassinate_weak",
        "trajectory": "PredictiveLine",
        "evaluators": [
          {"function": "inverse_health", "weight": 12.0},  // 优先低血量
          {"function": "inverse_distance", "weight": 3.0},  // 越近越好
          {"function": "has_mark", "weight": 5.0}           // 已标记目标加分
        ]
      },
      {
        "name": "duel_strong",
        "trajectory": "Corkscrew",
        "evaluators": [
          {"function": "health_ratio", "weight": 8.0},      // 优先高血量
          {"function": "is_elite", "weight": 10.0},         // 精英单位
          {"function": "threat_level", "weight": 6.0}       // 威胁等级
        ]
      },
      {
        "name": "intercept_mobile",
        "trajectory": "CurvedIntercept",
        "evaluators": [
          {"function": "velocity_magnitude", "weight": 5.0}, // 移动速度高
          {"function": "inverse_distance", "weight": 2.0},
          {"function": "has_ranged_attack", "weight": 4.0}   // 远程单位
        ]
      }
    ]
  }
}
```

**4. 效用规划器（UtilityPlanner）**

```java
// flyingsword/ai/utility/UtilityPlanner.java
public final class UtilityPlanner {

    private static final Map<AIMode, List<UtilityAction>> PROFILES = new HashMap<>();

    public static Optional<IntentResult> pickBest(UtilityContext ctx) {
        AIMode mode = ctx.getSword().getAIMode();
        List<UtilityAction> actions = PROFILES.get(mode);

        return actions.stream()
            .map(action -> {
                // 计算总效用值
                double utility = action.getEvaluators().stream()
                    .mapToDouble(eval -> eval.evaluate(ctx))
                    .sum();

                return new ScoredAction(action, utility);
            })
            .max(Comparator.comparingDouble(ScoredAction::utility))
            .map(scored -> IntentResult.builder()
                .trajectory(scored.action().getTrajectoryType())
                .priority(scored.utility())
                .target(ctx.getBestTarget())
                .build());
    }

    record ScoredAction(UtilityAction action, double utility) {}
}
```

**效果对比**：

| 维度 | 当前 Intent 系统 | 效用AI系统 |
|------|----------------|-----------|
| **代码量** | 16 个 Java 类 (800+ 行) | 1 个规划器 + 配置文件 (200 行代码 + 100 行 JSON) |
| **可调试性** | 优先级公式散落在代码中 | 所有权重集中在配置文件 |
| **灵活性** | 修改需要重新编译 | 热重载配置文件 |
| **决策透明度** | 无法看到决策过程 | 每个因素的贡献可视化 |
| **"聪明"程度** | 单一因素主导 | 多因素自然权衡 |

---

### 方案 B：黑板系统（Blackboard System）+ 缓存

**问题**：当前 AIContext 只有 4 个字段，每个 Intent 都要重复搜索目标

**解决**：建立"黑板"（共享信息池），一次搜索多次使用

#### 实现示例

```java
// flyingsword/ai/blackboard/SwordBlackboard.java
public class SwordBlackboard {
    private final FlyingSwordEntity sword;
    private final Map<String, Object> data = new HashMap<>();
    private long lastUpdateTick = -1;

    // 懒加载：首次访问时才计算
    public List<LivingEntity> getNearbyHostiles() {
        return computeIfAbsent("nearby_hostiles", () ->
            TargetFinder.findAllHostilesInRange(sword, 32.0)
        );
    }

    public Optional<LivingEntity> getLowestHealthTarget() {
        return computeIfAbsent("lowest_health", () ->
            getNearbyHostiles().stream()
                .min(Comparator.comparingDouble(LivingEntity::getHealth))
        );
    }

    public Optional<LivingEntity> getHighestThreatTarget() {
        return computeIfAbsent("highest_threat", () ->
            TargetFinder.findHighThreatMelee(sword.level(), sword.getOwner(), 32.0)
        );
    }

    public Vec3 getClusterCenter() {
        return computeIfAbsent("cluster_center", () ->
            TargetFinder.estimateHostileClusterCenter(sword.level(), sword.position(), 32.0)
        );
    }

    // 每帧开始时清空缓存
    public void tickReset(long currentTick) {
        if (lastUpdateTick != currentTick) {
            data.clear();
            lastUpdateTick = currentTick;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T computeIfAbsent(String key, Supplier<T> supplier) {
        return (T) data.computeIfAbsent(key, k -> supplier.get());
    }
}
```

**性能提升**：
- 当前：每个 Intent 单独搜索 → **8 次 AABB 查询 / 帧**
- 黑板：首次搜索后缓存 → **1 次 AABB 查询 / 帧**
- 性能提升：**~8x**

---

### 方案 C：行为树简化（Behavior Tree Lite）

**原理**：用树状结构组织决策逻辑，比 Intent 列表更直观

#### 实现示例

```java
// flyingsword/ai/btree/BehaviorNode.java
public interface BehaviorNode {
    enum Status { SUCCESS, FAILURE, RUNNING }
    Status tick(SwordBlackboard ctx);
}

// 选择节点：从左到右尝试，直到成功
class SelectorNode implements BehaviorNode {
    private final List<BehaviorNode> children;

    public Status tick(SwordBlackboard ctx) {
        for (var child : children) {
            Status status = child.tick(ctx);
            if (status != Status.FAILURE) return status;
        }
        return Status.FAILURE;
    }
}

// 序列节点：必须全部成功
class SequenceNode implements BehaviorNode {
    private final List<BehaviorNode> children;

    public Status tick(SwordBlackboard ctx) {
        for (var child : children) {
            Status status = child.tick(ctx);
            if (status != Status.SUCCESS) return status;
        }
        return Status.SUCCESS;
    }
}

// 条件节点
class ConditionNode implements BehaviorNode {
    private final Predicate<SwordBlackboard> condition;

    public Status tick(SwordBlackboard ctx) {
        return condition.test(ctx) ? Status.SUCCESS : Status.FAILURE;
    }
}

// 行动节点
class ActionNode implements BehaviorNode {
    private final Consumer<SwordBlackboard> action;

    public Status tick(SwordBlackboard ctx) {
        action.accept(ctx);
        return Status.SUCCESS;
    }
}
```

**HUNT 模式行为树示例**：

```java
// flyingsword/ai/btree/HuntBehaviorTree.java
BehaviorNode huntTree = new SelectorNode(List.of(
    // 1. 如果有标记目标 → 猎杀
    new SequenceNode(List.of(
        new ConditionNode(ctx -> ctx.getMarkedTarget().isPresent()),
        new ActionNode(ctx -> attackTarget(ctx, ctx.getMarkedTarget().get(), "PredictiveLine"))
    )),

    // 2. 如果有低血量敌人 → 刺杀
    new SequenceNode(List.of(
        new ConditionNode(ctx -> {
            var target = ctx.getLowestHealthTarget();
            return target.isPresent() && target.get().getHealth() < target.get().getMaxHealth() * 0.3;
        }),
        new ActionNode(ctx -> attackTarget(ctx, ctx.getLowestHealthTarget().get(), "PredictiveLine"))
    )),

    // 3. 如果有高威胁敌人 → 缠斗
    new SequenceNode(List.of(
        new ConditionNode(ctx -> ctx.getHighestThreatTarget().isPresent()),
        new ActionNode(ctx -> attackTarget(ctx, ctx.getHighestThreatTarget().get(), "Corkscrew"))
    )),

    // 4. 默认：巡逻搜索
    new ActionNode(ctx -> patrolSearch(ctx))
));
```

**优势**：
- 📊 可视化：可以画成决策树图
- 🎮 直观：从上到下阅读，像游戏脚本
- 🔧 易调试：每个节点可以打印状态
- 📦 复用性：子树可以在不同模式间共享

---

### 方案 D：表现层增强（视觉智能）🎨

**核心思想**：通过动画和特效让AI"看起来"更聪明，而不改变决策逻辑

#### 4.1 预判动画

```java
// 飞剑锁定目标时：
// 1. 先"悬停"0.2秒（盯着目标）
// 2. 剑尖发光粒子聚集
// 3. 然后爆发冲刺

public void onTargetAcquired(LivingEntity target) {
    // 视觉暂停
    sword.setDeltaMovement(Vec3.ZERO);

    // 对准目标
    Vec3 lookDir = target.position().subtract(sword.position()).normalize();
    sword.setYRot((float) Math.toDegrees(Math.atan2(lookDir.z, lookDir.x)));

    // 粒子效果（聚气）
    spawnChargeParticles(sword, 4); // 0.2秒 = 4 ticks

    // 延迟冲刺
    sword.level().scheduleTick(sword, () -> {
        sword.setDeltaMovement(lookDir.scale(maxSpeed));
        spawnDashParticles(sword);
    }, 4);
}
```

**效果**：玩家会觉得"飞剑在瞄准"，实际上只是延迟了 0.2 秒

#### 4.2 协作光效

```java
// 多把剑攻击同一目标时显示连线
public void renderCooperationLinks(PoseStack poseStack, MultiBufferSource bufferSource) {
    List<FlyingSwordEntity> allySwords = getNearbyAllySwords(32.0);
    LivingEntity myTarget = this.getTarget();

    if (myTarget == null) return;

    for (var ally : allySwords) {
        if (ally.getTarget() == myTarget) {
            // 绘制剑→目标的能量连线
            renderEnergyBeam(poseStack, bufferSource,
                this.position(), myTarget.position(),
                0x40FF4040, 2.0f); // 半透明红色
        }
    }
}
```

**效果**：玩家看到光束会觉得"飞剑在协同攻击"

#### 4.3 情境音效

```java
// 根据状态播放不同音效
public void playContextualSound() {
    AIMode mode = getAIMode();
    float speed = (float) getDeltaMovement().length();

    if (mode == AIMode.HUNT && speed > 0.8) {
        // 高速追击：尖锐破空声
        playSound(CCGuSounds.SWORD_DASH, 1.0f, 1.5f);
    } else if (mode == AIMode.GUARD) {
        // 防守：低频嗡鸣
        playSound(CCGuSounds.SWORD_ORBIT, 0.6f, 0.8f);
    } else if (isNearOwner(3.0)) {
        // 靠近主人：柔和剑鸣
        playSound(CCGuSounds.SWORD_IDLE, 0.4f, 1.0f);
    }
}
```

#### 4.4 动态轨迹渲染

```java
// 渲染飞剑的"意图轨迹"（预测路径）
public void renderIntentPath(PoseStack poseStack, MultiBufferSource bufferSource) {
    if (this.currentIntent == null) return;

    // 计算未来 1 秒的轨迹点
    List<Vec3> pathPoints = new ArrayList<>();
    Vec3 pos = position();
    Vec3 vel = getDeltaMovement();

    for (int i = 0; i < 20; i++) { // 20 ticks = 1 秒
        pos = pos.add(vel);
        pathPoints.add(pos);

        // 简单物理模拟（可选）
        vel = trajectory.computeDesiredVelocity(pos, currentIntent);
    }

    // 渲染虚线路径
    for (int i = 0; i < pathPoints.size() - 1; i++) {
        if (i % 2 == 0) { // 虚线效果
            renderLine(poseStack, bufferSource,
                pathPoints.get(i), pathPoints.get(i + 1),
                0x8080FF80, 1.0f); // 半透明绿色
        }
    }
}
```

**效果**：玩家看到飞剑的"思考轨迹"，感觉很"有谋略"

---

### 方案 E：记忆与适应系统

**问题**：飞剑会反复攻击无敌目标（如屏障保护的敌人）

**解决**：短期记忆 + 失败惩罚

```java
// flyingsword/ai/memory/SwordMemory.java
public class SwordMemory {
    // 记录最近 N 次攻击失败的目标
    private final Map<UUID, FailureRecord> recentFailures = new HashMap<>();

    record FailureRecord(int failureCount, long lastAttempt) {}

    public void recordFailure(LivingEntity target) {
        UUID id = target.getUUID();
        FailureRecord old = recentFailures.get(id);

        if (old == null) {
            recentFailures.put(id, new FailureRecord(1, level.getGameTime()));
        } else {
            recentFailures.put(id, new FailureRecord(old.failureCount + 1, level.getGameTime()));
        }
    }

    public double getFailurePenalty(LivingEntity target) {
        FailureRecord record = recentFailures.get(target.getUUID());
        if (record == null) return 1.0;

        // 失败越多，优先级惩罚越大
        return Math.max(0.1, 1.0 - (record.failureCount * 0.2));
    }

    public void tick() {
        long now = level.getGameTime();
        // 清理 10 秒前的记录
        recentFailures.entrySet().removeIf(entry ->
            now - entry.getValue().lastAttempt > 200
        );
    }
}
```

**使用示例**：

```java
// 在效用评估中应用记忆惩罚
double utility = baseUtility * sword.getMemory().getFailurePenalty(target);
```

**效果**：
- 攻击无敌目标 3 次后 → 优先级降至 40%
- 10 秒后记忆消退 → 可以重新尝试

---

## 实施路线图 🗺️

### 阶段 1：黑板系统（1-2 天）⚡ 快速见效

**目标**：减少 80% 的重复计算

1. 创建 `SwordBlackboard` 类
2. 扩展 `AIContext` 包含黑板引用
3. 重构 `TargetFinder` 为黑板方法
4. 更新 Intent 评估使用黑板

**预期收益**：
- 性能提升 5-8x
- 代码行数减少 200+

### 阶段 2：效用AI系统（3-4 天）🎯 推荐

**目标**：将 16 个 Intent 类精简为配置驱动

1. 实现 `UtilityFunction` 工具类
2. 创建 `UtilityPlanner` 替代 `IntentPlanner`
3. 编写 JSON 配置文件
4. 添加调试日志（显示每个因素的贡献）

**预期收益**：
- 代码行数减少 600+
- 可调试性提升 10x
- 支持热重载配置

### 阶段 3：表现层增强（2-3 天）🎨 即时反馈

**目标**：让飞剑"看起来"更聪明

1. 添加目标锁定动画
2. 协作光效渲染
3. 情境音效系统
4. 意图轨迹可视化（调试模式）

**预期收益**：
- 玩家感知智能提升 3x
- 无需改动核心逻辑

### 阶段 4（可选）：行为树迁移（4-5 天）

**目标**：用行为树替代 Intent 系统

1. 实现基础行为树节点
2. 为每个 AIMode 编写行为树
3. 迁移现有行为到树节点
4. 添加可视化调试工具

**预期收益**：
- 决策流程可视化
- 更好的复用性

---

## 代码量对比 📊

| 组件 | 当前实现 | 效用AI方案 | 行为树方案 | 节省 |
|------|---------|-----------|-----------|------|
| Intent 类 | 16 × 50 行 = 800 行 | 0 行 | 0 行 | **-800** |
| TargetFinder | 575 行 | 150 行（黑板方法） | 150 行 | **-425** |
| Planner | 74 行 | 120 行（UtilityPlanner） | 200 行（BehaviorTree） | -/+ |
| 配置 | 0 行 | 100 行 JSON | 150 行 JSON | +100 |
| 工具类 | 0 行 | 80 行（UtilityFunction） | 100 行（TreeNodes） | +80 |
| **总计** | **1449 行** | **450 行** | **600 行** | **-1000** 或 **-850** |

**关键指标**：
- 代码复杂度降低：**~70%**
- 可维护性提升：配置文件 vs 硬编码
- 性能提升：**5-8x**（黑板缓存）

---

## 调试与可视化工具 🔧

### 决策日志输出

```java
// flyingsword/ai/debug/DecisionLogger.java
public class DecisionLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("cc.flyingsword.debug");

    public static void logUtilityDecision(FlyingSwordEntity sword, List<ScoredAction> scores) {
        if (!DEBUG) return;

        LOGGER.info("=== Sword {} Decision ===", sword.getId());
        scores.forEach(scored -> {
            LOGGER.info("  {} → utility: {:.2f}",
                scored.action().getName(),
                scored.utility());

            // 详细分解
            scored.action().getEvaluators().forEach(eval -> {
                double contribution = eval.evaluate(sword.getBlackboard());
                LOGGER.info("    - {}: {:.2f} (weight: {:.2f})",
                    eval.name(), contribution, eval.weight());
            });
        });
        LOGGER.info("  CHOSEN: {}", scores.get(0).action().getName());
    }
}
```

**示例输出**：

```
=== Sword 12345 Decision ===
  assassinate_weak → utility: 18.5
    - inverse_health: 9.6 (weight: 12.0)
    - inverse_distance: 6.0 (weight: 3.0)
    - has_mark: 0.0 (weight: 5.0)
  duel_strong → utility: 14.2
    - health_ratio: 6.4 (weight: 8.0)
    - is_elite: 0.0 (weight: 10.0)
    - threat_level: 4.8 (weight: 6.0)
  intercept_mobile → utility: 8.3
    - velocity_magnitude: 2.5 (weight: 5.0)
    - inverse_distance: 6.0 (weight: 2.0)
    - has_ranged_attack: 0.0 (weight: 4.0)
  CHOSEN: assassinate_weak
```

### 游戏内可视化

```java
// 启用调试模式后，渲染决策信息
if (DEBUG_MODE) {
    // 在飞剑头顶显示当前意图
    renderFloatingText(sword.position().add(0, 1, 0),
        "Intent: " + currentAction.getName(),
        0xFFFFFF);

    // 渲染目标连线
    if (currentTarget != null) {
        renderLine(sword.position(), currentTarget.position(), 0xFF0000);
    }

    // 渲染黑板信息
    renderBlackboardInfo(sword.getBlackboard());
}
```

---

## 与重构计划的整合 🔗

**在 Phase 1 中整合**：
- 添加 `ENABLE_UTILITY_AI` 开关（默认关闭）
- 保留现有 Intent 系统作为回退

**在 Phase 2 中整合**：
- 将 UtilityPlanner 放入 `systems/decision/`
- 黑板系统放入 `systems/perception/`
- 记忆系统放入 `systems/memory/`

**在 Phase 6 中整合**：
- 补充效用AI使用文档
- 添加响应曲线调优指南
- 编写 JSON 配置示例

---

## 总结：为什么这些方案"聪明但简单"？

### 🎯 效用AI系统
- **简单**：数学曲线 + 配置文件
- **聪明**：多因素自然权衡产生复杂决策

### 🧠 黑板系统
- **简单**：懒加载缓存
- **聪明**：消除重复计算，提升响应速度

### 🎨 表现层增强
- **简单**：动画 + 粒子 + 音效
- **聪明**：玩家感知智能提升 3x

### 🌳 行为树（可选）
- **简单**：从上到下读决策树
- **聪明**：直观的优先级和回退逻辑

### 💾 记忆系统
- **简单**：失败计数 + 时间衰减
- **聪明**：避免重复错误

---

## 推荐实施顺序

1. **🚀 阶段 1：黑板系统**（快速见效，2天）
2. **🎨 阶段 3：表现层增强**（立即可见，3天）
3. **🎯 阶段 2：效用AI系统**（核心重构，4天）
4. **📝 文档与调试工具**（贯穿整个过程）

**总工作量**：9-12 天

**核心理念再强调**：
> 真正的智能不是堆砌 if-else，而是选择正确的抽象层。用数学曲线、缓存、动画特效这些简单工具，可以创造出远超复杂代码的"聪明"表现。

---

## 参考资源

1. **Utility AI Theory**: [GDC 2010 - Behavioral Mathematics for Game AI](https://www.gdcvault.com/play/1012410/Behavior-Math-for)
2. **Behavior Trees**: [Unreal Engine - Behavior Trees](https://docs.unrealengine.com/5.0/en-US/behavior-trees-in-unreal-engine/)
3. **Blackboard Systems**: [AI Game Programming Wisdom](http://www.aiwisdom.com/)
4. **Response Curves**: [Building Better AI through Utility Theory](https://www.gamedeveloper.com/programming/creating-a-utility-based-ai-using-response-curves)
