# 飞剑模块重构｜迁移说明

## 概述

本文档帮助您从旧版本飞剑系统迁移到重构后的 1.0.0-RC1 版本。

**重构版本**: 1.0.0-RC1
**发布日期**: 2025-11-06
**兼容性**: 向后兼容，旧存档可直接加载

---

## 主要变更

### 1. 功能开关系统（Phase 0-1）

**变更说明**：
- 引入功能开关，默认关闭高级功能
- 精简默认轨迹和意图，提升性能

**影响**：
- 默认配置下，只启用核心轨迹（Orbit、PredictiveLine、CurvedIntercept）
- 默认配置下，每个 AIMode 只有 ≤2 条核心意图
- 高级轨迹、扩展意图、Swarm、TUI、Gecko 需手动启用

**迁移步骤**：

如果您之前使用了高级轨迹或扩展意图，需要在配置中启用：

```java
// 位置：FlyingSwordTuning.java
public static boolean ENABLE_ADVANCED_TRAJECTORIES = true;  // 启用高级轨迹
public static boolean ENABLE_EXTRA_INTENTS = true;          // 启用扩展意图
public static boolean ENABLE_SWARM = false;                 // 启用集群系统（可选）
public static boolean ENABLE_TUI = false;                   // 启用指挥中心 UI（可选）
public static boolean ENABLE_GEO_OVERRIDE_PROFILE = false;  // 启用 Gecko 模型覆盖（可选）
```

**默认关闭的高级轨迹**（共 13 个）：
- Boomerang、Corkscrew、BezierS、Serpentine、VortexOrbit
- Sawtooth、PetalScan、WallGlide、ShadowStep、DomainEdgePatrol
- Ricochet、HelixPair、PierceGate

**默认关闭的扩展意图**：
- ORBIT 模式：SweepSearchIntent
- GUARD 模式：DecoyIntent、KitingIntent
- HUNT 模式：FocusFireIntent、BreakerIntent、SuppressIntent、ShepherdIntent、SweepIntent、KitingIntent、DecoyIntent、PivotIntent、SweepSearchIntent

### 2. 系统分层重构（Phase 2）

**变更说明**：
- 代码按职责分层到 `systems/` 目录
- 实体类职责精简，复杂逻辑移交给系统

**影响**：
- 如果您的扩展模组直接调用了实体类的内部方法，可能需要调整
- 建议使用事件系统扩展功能，而非直接调用内部方法

**迁移步骤**：

检查您的代码是否直接调用了以下内部方法：

```java
// 旧代码（可能失效）
sword.tickMovement();  // 已移至 MovementSystem
sword.tickCombat();    // 已移至 CombatSystem
sword.tickUpkeep();    // 已移至 UpkeepSystem

// 新代码（推荐）
// 通过事件系统扩展功能
FlyingSwordEventRegistry.subscribe(OnTickContext.class, ctx -> {
    // 您的自定义逻辑
});
```

### 3. 事件模型扩展（Phase 3）

**变更说明**：
- 新增多个事件上下文，支持更细粒度的扩展
- 事件系统完善，支持取消和参数修改

**新增事件**：
- `ModeChangeContext` - AI 模式切换
- `TargetAcquiredContext` - 目标获取
- `TargetLostContext` - 目标丢失
- `UpkeepCheckContext` - 维持消耗检查
- `PostHitContext` - 命中后（只读）
- `BlockBreakAttemptContext` - 破块尝试
- `ExperienceGainContext` - 经验获取
- `LevelUpContext` - 升级

**迁移步骤**：

如果您之前通过其他方式监听这些事件，现在可以使用官方事件：

```java
// 示例：监听升级事件
FlyingSwordEventRegistry.subscribe(LevelUpContext.class, ctx -> {
    int oldLevel = ctx.oldLevel;
    int newLevel = ctx.newLevel;
    FlyingSwordEntity sword = ctx.sword;

    // 您的自定义逻辑（如升级特效、奖励等）
});
```

### 4. 冷却与资源统一（Phase 4）

**变更说明**：
- 冷却系统统一到 `MultiCooldown`（owner 附件）
- 移除实体级冷却字段
- 资源消耗统一到 `UpkeepOps`

**影响**：
- **重要**：如果您的扩展模组直接访问了实体的冷却字段，将无法编译
- 旧的冷却数据会自动迁移到新系统

**迁移步骤**：

更新您的冷却访问代码：

```java
// 旧代码（已移除）
int cooldown = sword.attackCooldown;  // 编译错误
sword.attackCooldown = 10;            // 编译错误

// 新代码
int cooldown = FlyingSwordCooldownOps.getAttackCooldown(sword);
FlyingSwordCooldownOps.setAttackCooldown(sword, 10);

// 检查是否就绪
if (FlyingSwordCooldownOps.isAttackReady(sword)) {
    // 执行攻击
}
```

**维持失败策略**：

新增配置选项，允许自定义维持不足时的行为：

```java
// FlyingSwordTuning.java
public static UpkeepFailureStrategy UPKEEP_FAILURE_STRATEGY = UpkeepFailureStrategy.RECALL;

// 可选策略：
// - RECALL: 召回到物品栏（默认，兼容旧版）
// - STALL: 停滞不动
// - SLOW: 减速移动
```

### 5. 客户端与网络优化（Phase 5）

**变更说明**：
- 渲染优化，默认路径性能提升
- Gecko/Override/Profile 仅在开关启用时加载

**影响**：
- 如果您使用了 Gecko 模型覆盖或视觉档案，需要启用开关
- 默认渲染性能更好

**迁移步骤**：

如果您使用了 Gecko 模型或视觉档案：

```java
// FlyingSwordTuning.java
public static boolean ENABLE_GEO_OVERRIDE_PROFILE = true;
```

### 6. 测试与文档（Phase 6）

**变更说明**：
- 新增单元测试（11 个测试类）
- 完善文档系统
- 测试覆盖率 ≥ 70%

**影响**：
- 代码质量提升
- 文档更完善

**迁移步骤**：

如果您需要扩展功能，建议参考以下文档：

- `docs/FLYINGSWORD_STANDARDS.md` - 规范文档
- `docs/FLYINGSWORD_MANUAL_TEST_CHECKLIST.md` - 测试清单
- `docs/FLYINGSWORD_IMPLEMENTATION_PLAN.md` - 实施计划

---

## 兼容性保证

### 存档兼容性

**保证**：旧存档可直接加载，无需额外转换。

**自动迁移**：
- 旧的实体级冷却数据会自动迁移到 `MultiCooldown`
- 旧的 NBT 数据会自动转换为新格式

**验证方法**：
1. 备份您的存档
2. 加载存档
3. 检查飞剑是否正常工作
4. 执行手动测试清单（`docs/FLYINGSWORD_MANUAL_TEST_CHECKLIST.md`）

### API 兼容性

**保证**：公开 API 保持向后兼容。

**变更说明**：
- 内部实现重构，但公开接口保持稳定
- 旧的 API 调用仍然有效（除非标记为 `@Deprecated`）

**弃用 API**：

以下 API 已弃用，建议迁移：

```java
// 已弃用：直接访问实体冷却字段
@Deprecated
public int attackCooldown;  // 使用 FlyingSwordCooldownOps 替代

// 已弃用：旧式 Goal 路径
@Deprecated
public class ForceHuntTargetGoal { ... }  // 已移除，使用 Intent 系统

// 已弃用：旧式行为工具
@Deprecated
public class SwordGoalOps { ... }  // 已移除，使用 SteeringOps
```

### 配置兼容性

**保证**：旧的配置文件可继续使用。

**新增配置**：
- 所有新配置都有合理的默认值
- 不影响现有配置

---

## 迁移检查清单

使用以下清单确保迁移成功：

### 编译检查

- [ ] 代码编译通过（`./gradlew compileJava`）
- [ ] 测试编译通过（`./gradlew compileTestJava`）
- [ ] 无弃用警告（或已计划处理）

### 功能检查

- [ ] 飞剑召唤正常
- [ ] AI 模式切换正常（ORBIT/GUARD/HUNT/RECALL）
- [ ] 战斗系统正常（攻击、伤害、经验）
- [ ] 资源消耗正常（维持扣除真元）
- [ ] 冷却系统正常（攻击冷却、破块冷却）

### 扩展功能检查

如果您启用了高级功能：

- [ ] 高级轨迹正常工作（需 `ENABLE_ADVANCED_TRAJECTORIES = true`）
- [ ] 扩展意图正常工作（需 `ENABLE_EXTRA_INTENTS = true`）
- [ ] Swarm 系统正常工作（需 `ENABLE_SWARM = true`）
- [ ] 指挥中心 UI 正常显示（需 `ENABLE_TUI = true`）
- [ ] Gecko 模型正常渲染（需 `ENABLE_GEO_OVERRIDE_PROFILE = true`）

### 性能检查

- [ ] 单个飞剑运行流畅
- [ ] 多个飞剑（5+）无明显卡顿
- [ ] 服务器 TPS 稳定
- [ ] 客户端 FPS 稳定

### 数据检查

- [ ] 旧存档加载成功
- [ ] 飞剑属性正确（等级、经验、耐久）
- [ ] 冷却数据正确迁移
- [ ] 无数据丢失

---

## 回退方案

如果迁移遇到问题，可以回退到旧版本：

### 回退步骤

1. **备份当前存档**（已使用新版本修改的存档）
2. **恢复旧版本代码**：
   ```bash
   git checkout <old-version-tag>
   ```
3. **恢复旧存档备份**（迁移前的备份）
4. **编译旧版本**：
   ```bash
   ./gradlew clean build
   ```

### 注意事项

- 使用新版本修改过的存档可能无法在旧版本中加载
- 回退前务必备份存档

---

## 常见问题（FAQ）

### Q1: 为什么默认关闭了高级轨迹？

**A**: 为了提升性能和降低维护成本。默认配置下，飞剑系统运行更流畅。如需使用高级轨迹，可在配置中启用。

### Q2: 如何启用 Gecko 模型覆盖？

**A**: 在 `FlyingSwordTuning` 中设置：
```java
public static boolean ENABLE_GEO_OVERRIDE_PROFILE = true;
```

### Q3: 旧的冷却数据会丢失吗？

**A**: 不会。旧的实体级冷却数据会自动迁移到新的 `MultiCooldown` 系统。

### Q4: 如何扩展飞剑功能？

**A**: 推荐使用事件系统：
```java
FlyingSwordEventRegistry.subscribe(OnHitEntityContext.class, ctx -> {
    // 您的自定义逻辑
});
```

### Q5: 测试覆盖率要求多少？

**A**: 整体 ≥ 70%，核心计算器 ≥ 80%。建议运行 `./gradlew test jacocoTestReport` 检查覆盖率。

### Q6: Phase 8 什么时候发布？

**A**: Phase 8（姿态统一）计划在 1.1.0 版本中发布。届时将修复"途中剑头朝上"等渲染问题。

### Q7: 如何报告问题？

**A**: 请在项目仓库提交 Issue，并附上：
- 详细问题描述
- 复现步骤
- 日志文件
- Minecraft 版本和模组列表

---

## 获取帮助

如果在迁移过程中遇到问题：

1. **查阅文档**：
   - [CHANGELOG](FLYINGSWORD_CHANGELOG.md) - 变更日志
   - [STANDARDS](FLYINGSWORD_STANDARDS.md) - 规范文档
   - [MANUAL_TEST_CHECKLIST](FLYINGSWORD_MANUAL_TEST_CHECKLIST.md) - 测试清单

2. **检查日志**：
   - 查看 `logs/latest.log` 中的错误信息
   - 启用 DEBUG 日志获取更多信息

3. **提交 Issue**：
   - 在项目仓库提交详细的问题报告
   - 附上日志、配置和复现步骤

---

## 致谢

感谢您使用飞剑模块重构版本！我们致力于提供更好的性能、更清晰的架构和更完善的文档。

如有任何问题或建议，欢迎反馈。

---

**文档版本**: 1.0.0-RC1
**更新日期**: 2025-11-06
**维护者**: 飞剑模块重构项目组
