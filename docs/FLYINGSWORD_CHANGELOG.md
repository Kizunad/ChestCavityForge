# 飞剑模块重构 CHANGELOG

## [1.0.0-RC1] - 2025-11-06

### Phase 0 - 接线与开关 ✅

**新增功能**：
- 添加事件系统初始化：`FlyingSwordEventInit.init()`
- 添加功能开关配置（位于 `FlyingSwordTuning`）：
  - `ENABLE_ADVANCED_TRAJECTORIES` - 高级轨迹开关（默认 false）
  - `ENABLE_EXTRA_INTENTS` - 扩展意图开关（默认 false）
  - `ENABLE_SWARM` - 集群系统开关（默认 false）
  - `ENABLE_TUI` - 指挥中心 UI 开关（默认 false）
  - `ENABLE_GEO_OVERRIDE_PROFILE` - Gecko 模型覆盖开关（默认 false）

**架构变更**：
- 建立事件驱动架构基础
- 默认钩子注册机制

### Phase 1 - 裁剪（先停用后清理）✅

**功能裁剪**：
- 轨迹系统精简：默认仅启用核心轨迹（Orbit、PredictiveLine、CurvedIntercept）
- 意图系统精简：每个 AIMode 精简到 ≤2 条核心意图
- 高级轨迹（13 个）移至功能开关控制：
  - Boomerang、Corkscrew、BezierS、Serpentine、VortexOrbit
  - Sawtooth、PetalScan、WallGlide、ShadowStep、DomainEdgePatrol
  - Ricochet、HelixPair、PierceGate
- 扩展意图移至功能开关控制

**移除内容**：
- 旧式 Goal 追击路径（`ForceHuntTargetGoal`、`SwordGoalOps`）
- 未使用的旧代码路径

**配置变更**：
- 指挥中心与高级战术由 `ENABLE_TUI` 控制
- Gecko 模型覆盖/视觉档渲染由 `ENABLE_GEO_OVERRIDE_PROFILE` 控制
- Swarm 系统由 `ENABLE_SWARM` 控制

### Phase 2 - 分层重构 ✅

**架构重构**：
- 建立 `systems/` 目录，按职责分层：
  - `MovementSystem` - 运动系统
  - `CombatSystem` - 战斗系统
  - `DefenseSystem` - 防御系统
  - `BlockBreakSystem` - 破块系统
  - `TargetingSystem` - 目标系统
  - `ProgressionSystem` - 经验成长系统
  - `UpkeepSystem` - 维持消耗系统
  - `LifecycleSystem` - 生命周期系统

**代码优化**：
- 实体类（`FlyingSwordEntity`）职责精简：仅保留上下文组装与事件触发
- 复杂逻辑移交给对应系统
- 系统设计为无状态静态类，便于测试

**文档更新**：
- 添加 `systems/README.md` 说明系统职责

### Phase 3 - 事件模型扩展 ✅

**新增事件上下文**：
- `ModeChangeContext` - AI 模式切换事件
- `TargetAcquiredContext` - 目标获取事件
- `TargetLostContext` - 目标丢失事件
- `UpkeepCheckContext` - 维持消耗检查事件
- `PostHitContext` - 命中后事件（只读）
- `BlockBreakAttemptContext` - 破块尝试事件
- `ExperienceGainContext` - 经验获取事件
- `LevelUpContext` - 升级事件

**事件系统增强**：
- `FlyingSwordEventRegistry` 实现对应 fire 方法
- 保持短路语义一致性
- 前置事件可取消/改参，后置事件只读

**集成点**：
- 系统入口正确触发事件
- 事件钩子可扩展和定制行为

### Phase 4 - 冷却与资源一致性 ✅

**冷却系统统一**：
- 移除实体级冷却字段
- 统一使用 `MultiCooldown`（owner 附件）管理冷却
- 通过 `FlyingSwordCooldownOps` 操作冷却
- 冷却 key 规范：`cc:flying_sword/<uuid>/<domain>`
  - 攻击冷却：`cc:flying_sword/<uuid>/attack`
  - 破块冷却：`cc:flying_sword/<uuid>/block_break`

**资源消耗统一**：
- `UpkeepSystem` 集中调用 `UpkeepOps` 管理资源消耗
- 触发 `OnUpkeepCheck` 事件，允许外部模块修改消耗量
- 维持失败策略配置化：
  - `RECALL` - 召回到物品栏（默认）
  - `STALL` - 停滞不动
  - `SLOW` - 减速移动

**配置新增**：
- `FlyingSwordTuning.UPKEEP_FAILURE_STRATEGY` - 维持失败策略
- `FlyingSwordTuning.UPKEEP_FAILURE_SLOW_FACTOR` - 减速倍率
- `FlyingSwordTuning.UPKEEP_FAILURE_SOUND_INTERVAL` - 音效间隔

### Phase 5 - 客户端与网络优化 ✅

**渲染优化**：
- 默认渲染路径：`FlyingSwordRenderer` + 默认 FX
- Gecko/Override/Profile 仅在开关启用时加载和注册
- 减少不必要的渲染计算

**网络优化**：
- 事件副作用通过实体同步实现
- 减少自定义网络消息
- 优化同步频率

**性能优化**：
- 默认配置下降低运行开销
- 高级功能通过开关按需启用

### Phase 6 - 文档与测试 ✅

**单元测试新增**（11 个测试类）：
1. `FlyingSwordStorageTest` - NBT 序列化测试
2. `ItemAffinityUtilTest` - 物品亲和度计算
3. `ItemDurabilityUtilTest` - 耐久工具
4. `KinematicsOpsTest` - 运动学基础
5. `FlyingSwordCalculatorTest` - 核心计算器（伤害、维持、经验、耐久）
6. `UpkeepOpsTest` - 维持消耗逻辑
7. `KinematicsOpsEdgeCaseTest` - 边界条件与回归测试
8. `KinematicsSnapshotMathTest` - 快照数学测试
9. `SteeringCommandTest` - 命令链式配置
10. `SteeringOpsTest` - 转向操作测试（使用 Mockito）
11. `CombatSystemTest` - 战斗系统集成测试

**测试覆盖率**：
- 目标：整体 ≥ 70%，核心计算器 ≥ 80%
- 边界条件充分覆盖
- 使用 JUnit 5 和 Mockito

**文档新增**：
- `docs/FLYINGSWORD_STANDARDS.md` - 规范文档（含测试规范）
- `docs/FLYINGSWORD_MANUAL_TEST_CHECKLIST.md` - 手动测试清单
- `docs/stages/PHASE_0.md` ~ `PHASE_8.md` - 阶段文档
- `systems/README.md` - 系统职责说明

**文档更新**：
- `docs/FLYINGSWORD_IMPLEMENTATION_PLAN.md` - 实施计划
- `docs/FLYINGSWORD_MASTER_PLAN.md` - 总体计划

### Phase 7 - 最终清理与发布 ✅

**代码清理**：
- 移除未引用实现和临时调试代码
- 降低日志级别：INFO 日志降至 DEBUG（非关键路径）
- 清理 TODO/FIXME 注释（保留说明性注释）
- 移除未使用的导入

**一致性验证**：
- ✅ 无实体级冷却残留
- ✅ 无直接 ResourceOps 旁路
- ✅ 无旧 Goal/遗留路径
- ✅ 功能开关正确控制未使用功能

**资源清理**：
- 轨迹资源：13 个高级轨迹由开关控制
- 意图资源：扩展意图由开关控制
- 默认配置下无冗余加载

**文档新增**：
- `docs/FLYINGSWORD_CHANGELOG.md` - 本文档
- `docs/FLYINGSWORD_MIGRATION.md` - 迁移说明
- `docs/stages/PHASE_7.md` - Phase 7 详细文档

**构建验证**：
- 编译通过：`./gradlew compileJava`
- 测试通过：`./gradlew test`
- 测试覆盖率达标

---

## [Planned] 未来版本

### Phase 8 - 姿态统一 📋

**计划功能**：
- 引入 `OrientationOps` 统一渲染姿态计算
- 使用四元数和正交基替代欧拉角
- 修复"途中剑头朝上"等渲染问题
- 当前欧拉渲染路径标记为 Legacy（保留兼容）

**配置选项**：
- `USE_BASIS_ORIENTATION` - 启用新姿态系统（默认 true）
- `ORIENTATION_MODE` - 姿态模式（BASIS | LEGACY_EULER）
- `UP_MODE` - 上向量模式（WORLD_Y | OWNER_UP）

---

## 迁移指南

### 从旧版本升级

如果您从旧版本飞剑系统升级，请参考 `docs/FLYINGSWORD_MIGRATION.md`。

### 功能开关配置

默认情况下，所有高级功能关闭。如需启用：

```java
// 在 FlyingSwordTuning 中设置
ENABLE_ADVANCED_TRAJECTORIES = true;  // 启用高级轨迹
ENABLE_EXTRA_INTENTS = true;          // 启用扩展意图
ENABLE_SWARM = true;                  // 启用集群系统
ENABLE_TUI = true;                    // 启用指挥中心 UI
ENABLE_GEO_OVERRIDE_PROFILE = true;   // 启用 Gecko 模型覆盖
```

---

## 已知问题

### 渲染相关
- **途中剑头朝上**：当前使用欧拉角渲染，在某些角度可能出现朝向不自然。Phase 8 将引入 OrientationOps 修复此问题。

### 性能相关
- **大量飞剑**：10+ 飞剑同时存在时可能影响性能。建议使用默认配置（关闭高级功能）。

### 测试覆盖
- **系统测试**：部分系统测试（如 CombatSystem）由于依赖 Minecraft 环境，测试覆盖有限。建议执行手动测试清单。

---

## 贡献者

- 飞剑模块重构项目组

---

## 参考文档

- [Master Plan](FLYINGSWORD_MASTER_PLAN.md) - 总体计划
- [Migration Guide](FLYINGSWORD_MIGRATION.md) - 迁移说明
- [Standards](FLYINGSWORD_STANDARDS.md) - 规范文档
- [Manual Test Checklist](FLYINGSWORD_MANUAL_TEST_CHECKLIST.md) - 手动测试清单
- [Implementation Plan](FLYINGSWORD_IMPLEMENTATION_PLAN.md) - 实施计划

---

**版本**: 1.0.0-RC1
**发布日期**: 2025-11-06
**状态**: Release Candidate
