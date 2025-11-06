# Phase 7｜最终清理与发布

## 阶段目标
- 删除未引用代码；完成评审与发布候选版本。
- 降低日志级别；清理调试代码。
- 确认无实体级冷却残留、无直接 ResourceOps 旁路、无旧 Goal/遗留路径。
- 标注 Legacy 路径为兼容选项。
- 产出 RC 构件并更新文档。

## 实施日期
2025-11-06

## 任务列表

### 7.1 代码清理

#### 7.1.1 删除未引用实现与临时调试代码
**位置**: 全代码库扫描
- [ ] 删除未引用的高级轨迹实现（需配合功能开关检查）
- [ ] 删除未引用的意图实现（需配合功能开关检查）
- [ ] 删除临时调试代码（System.out.println、调试注释等）
- [ ] 清理未使用的导入（import）

#### 7.1.2 降低日志级别
**位置**: 全代码库扫描
- [ ] 将 INFO 以上日志降低到 DEBUG 级别（非关键路径）
- [ ] 保留关键操作的 INFO 日志（召唤、召回、模式切换等）
- [ ] 错误和警告日志保持不变

嵌入指引（命令与定位）
- 搜索飞剑模块中的 INFO 日志：
  - `rg -n "LOGGER.info\(|System\.out\.println\(" src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword`
  - 重点文件（建议降级为 DEBUG，关键里程碑除外）：
    - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/combat/FlyingSwordCombat.java:64`
    - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/combat/FlyingSwordCombat.java:114`
    - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/combat/FlyingSwordCombat.java:141`
    - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/combat/FlyingSwordCombat.java:206`
- 规则建议：
  - DEBUG：命中调试、数值打印、周期性状态；
  - INFO：召唤/召回、升级成功、重要模式切换；
  - WARN/ERROR：异常与失败分支。

#### 7.1.3 清理 TODO/FIXME 注释
**位置**:
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/combat/FlyingSwordCombat.java`
- [ ] 检查并解决或移除所有 TODO/FIXME/XXX/HACK 注释

### 7.2 一致性验证

#### 7.2.1 确认无实体级冷却残留
**验证点**:
- [ ] 检查 `FlyingSwordEntity` 是否还有实体级冷却字段
- [ ] 确认所有冷却都通过 `MultiCooldown` (owner 附件) 管理
- [ ] 验证冷却 key 规范: `cc:flying_sword/<uuid>/attack` 等

#### 7.2.2 确认无直接 ResourceOps 旁路
**验证点**:
- [ ] 检查是否有绕过 `UpkeepOps` 的资源消耗代码
- [ ] 确认所有资源消耗都通过 `integration/resource/UpkeepOps` 进行
- [ ] 验证失败策略配置正确（RECALL/STALL/SLOW）

#### 7.2.3 确认无旧 Goal/遗留路径
**验证点**:
- [ ] 确认 `ForceHuntTargetGoal` 已删除或不再使用
- [ ] 确认 `SwordGoalOps` 已删除或不再使用
- [ ] 检查是否有其他旧式 AI Goal 残留

### 7.3 资源与数据清理

#### 7.3.1 清点未引用的轨迹资源
**位置**: `ai/trajectory/`
- [ ] 列出所有轨迹实现
- [ ] 标记未使用的轨迹（默认配置下）
- [ ] 确保未使用的轨迹被功能开关控制
- [ ] 考虑移除完全未引用的轨迹

候选删除清单（默认关闭 ENABLE_ADVANCED_TRAJECTORIES=false）
- 保留（始终注册）
  - `Orbit`：src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/trajectory/impl/OrbitTrajectory.java
  - `PredictiveLine`：src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/trajectory/impl/PredictiveLineTrajectory.java
  - `CurvedIntercept`：src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/trajectory/impl/CurvedInterceptTrajectory.java
- 候选删除（默认不注册）
  - `Boomerang`：ai/trajectory/impl/BoomerangTrajectory.java
  - `Corkscrew`：ai/trajectory/impl/CorkscrewTrajectory.java
  - `BezierS`：ai/trajectory/impl/BezierSTrajectory.java
  - `Serpentine`：ai/trajectory/impl/SerpentineTrajectory.java
  - `VortexOrbit`：ai/trajectory/impl/VortexOrbitTrajectory.java
  - `Sawtooth`：ai/trajectory/impl/SawtoothTrajectory.java
  - `PetalScan`：ai/trajectory/impl/PetalScanTrajectory.java
  - `WallGlide`：ai/trajectory/impl/WallGlideTrajectory.java
  - `ShadowStep`：ai/trajectory/impl/ShadowStepTrajectory.java
  - `DomainEdgePatrol`：ai/trajectory/impl/DomainEdgePatrolTrajectory.java
  - `Ricochet`：ai/trajectory/impl/RicochetTrajectory.java
  - `HelixPair`：ai/trajectory/impl/HelixPairTrajectory.java
  - `PierceGate`：ai/trajectory/impl/PierceGateTrajectory.java
注册守卫：`Trajectories.java:38`（受 `ENABLE_ADVANCED_TRAJECTORIES` 控制）

嵌入指引（命令与定位）
- 列出轨迹与注册：
  - `rg -n "class .*Trajectory|register\(TrajectoryType" src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/trajectory`
  - 对照 `Trajectories.java` 与 `FlyingSwordTuning.ENABLE_ADVANCED_TRAJECTORIES` 的实际使用
- 未引用判定：
  - `rg -n "new \w+Trajectory\(|TrajectoryType\.\w+" src/main/java | rg -v "Trajectories\.java"`

#### 7.3.2 清点未引用的意图资源
**位置**: `ai/intent/`
- [ ] 列出所有意图实现
- [ ] 标记未使用的意图（默认配置下）
- [ ] 确保未使用的意图被功能开关控制
- [ ] 考虑移除完全未引用的意图

候选删除清单（默认关闭 ENABLE_EXTRA_INTENTS=false）
- 保留（默认启用）
  - ORBIT：`HoldIntent`、`PatrolIntent`
  - GUARD：`GuardIntent`、`InterceptIntent`
  - HUNT：`AssassinIntent`、`DuelIntent`
  - RECALL：`RecallIntent`
- 候选删除（默认不启用）
  - `SweepSearchIntent`、`DecoyIntent`、`KitingIntent`、`FocusFireIntent`、`BreakerIntent`、`SuppressIntent`、`ShepherdIntent`、`SweepIntent`、`PivotIntent`
规划守卫：`IntentPlanner.java:31,40,50`（受 `ENABLE_EXTRA_INTENTS` 控制）

嵌入指引（命令与定位）
- 列出意图类与规划：
  - `rg -n "class .*Intent\b|new .*Intent\(" src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai`
  - 对照 `intent/planner/IntentPlanner.java` 与 `FlyingSwordTuning.ENABLE_EXTRA_INTENTS`
- 未引用判定：
  - `rg -n "new .*Intent\(" src/main/java | rg -v "IntentPlanner\.java"`

#### 7.3.3 清点未引用的模型覆盖资源
**位置**: `client/` 和资源文件
- [ ] 检查 Gecko 模型资源
- [ ] 检查视觉档案资源
- [ ] 确保默认配置下无冗余加载

清单与保留原则（ENABLE_GEO_OVERRIDE_PROFILE=false，默认不加载）
- Loader/Registry（保留，后续会使用到 Gecko）
  - 覆盖：`client/override/SwordModelOverrideRegistry.java`、`client/override/SwordModelOverrideLoader.java`
  - 视觉档：`client/profile/SwordVisualProfileRegistry.java`、`client/profile/SwordVisualProfileLoader.java`
- 资源（示例，默认不加载）
  - `assets/guzhenren/sword_models/qinglian.json`
  - `assets/guzhenren/sword_visuals/qinglian.json`（`enabled=false`）
- 客户端注册守卫：`ChestCavity.java:229`（受 `ENABLE_GEO_OVERRIDE_PROFILE` 控制）
- 结论：Gecko/覆盖/视觉档需保留（会使用到）；仅确保默认配置下不加载、无性能负担。

软删除建议（不破坏可选功能）
- 仅保留注册守卫与开关，默认构建不注册/不加载；
- 删除候选：限于“默认关闭且确认未来不再支持”的轨迹/意图；Gecko 相关一律保留；
- 删除前在 `CHANGELOG.md` 与 `docs/FLYINGSWORD_MIGRATION.md` 标注兼容性说明与过渡期。

资源逐项摘要（扫描结果，用于二次确认）
- `src/main/resources/assets/guzhenren/sword_models/qinglian.json`
  - key: `qinglian`
  - renderer: `item`
  - enabled: N/A（覆盖定义无 enabled 字段，由开关控制 Loader 注册）
  - align: `target`
  - pre_roll: `-45.0`
  - yaw_offset: `0`
  - pitch_offset: `0.0`
  - scale: `0.5`
  - display_item: `minecraft:diamond_sword`
  - model: null / textures: null / animation: null

- `src/main/resources/assets/guzhenren/sword_visuals/qinglian.json`
  - key: `qinglian`
  - enabled: `false`（默认不启用）
  - renderer: `item`
  - align: `target`
  - pre_roll: `-45.0`
  - yaw_offset: `-90.0`
  - pitch_offset: `0.0`
  - scale: `1.0`
  - glint: `inherit`
  - model: null / textures: [] / animation: null
  - match_model_keys: `["qinglian"]`

备注：由于 `ENABLE_GEO_OVERRIDE_PROFILE=false`，上述资源在默认构建中不会被加载；确认保留 Gecko/覆盖/视觉档为可选功能（将来会用到）。

嵌入指引（命令与定位）
- 仅在开关启用时应加载：
  - 检查 `ChestCavity.java` 的 `ENABLE_GEO_OVERRIDE_PROFILE` 守卫是否完整
  - `rg -n "SwordModelOverrideLoader|SwordVisualProfileLoader" src/main/java`
- 资源清单交叉：
  - `rg -n "profile|override|gecko" src/main/resources | rg -i "json|geo"`
  - 对照注册器：`client/override/*Registry`、`client/profile/*Registry`

### 7.4 API/文档标注

#### 7.4.1 标注 Legacy 路径（渲染欧拉顺序）为兼容选项
**位置**:
- `client/renderer/FlyingSwordRenderer.java`
- `docs/FLYINGSWORD_STANDARDS.md`

**任务**:
- [ ] 在渲染器中明确标注旧欧拉渲染路径为 Legacy
- [ ] 添加开关配置说明（如 `USE_BASIS_ORIENTATION`）
- [ ] 文档化回退选项和兼容性说明

嵌入指引（文档与代码注释）
- 渲染器注释：在 `client/FlyingSwordRenderer.java` 贴注释“欧拉 Y→Z 为 Legacy 路径”；
- 标注开关：在 `FlyingSwordModelTuning`（或 `FlyingSwordTuning`）新增/说明 `USE_BASIS_ORIENTATION`（默认 true，P8 落地）；
- 标注 Profile 字段：在 `SwordModelOverrideLoader`/`SwordVisualProfileLoader` 文档说明 `orientationMode/upMode`（P8）。

#### 7.4.2 同步 CHANGELOG、版本号与迁移说明
**位置**:
- `CHANGELOG.md` (如果存在)
- `docs/FLYINGSWORD_MIGRATION.md` (新建)

**任务**:
- [ ] 创建或更新 CHANGELOG，记录 Phase 0-7 的所有变更
- [ ] 更新版本号（建议：1.0.0-RC1）
- [ ] 编写迁移说明文档，指导用户升级

嵌入指引（文件与步骤）
- 版本号位置：`gradle.properties` 的 `mod_version=2.16.4`（示例），建议临时设为 `-RC1`；
- 新增文档：`docs/FLYINGSWORD_MIGRATION.md`，包含：
  - 冷却统一到 MultiCooldown 的迁移说明；
  - Upkeep 失败策略与默认行为；
  - 客户端降噪与开关；
  - 渲染 Legacy 回退开关；
- CHANGELOG 模板：
  - `## [2.17.0-RC1] - 2025-11-06`（示例）
  - Added/Changed/Fixed/Removed 小节简述改动。

### 7.5 构建与候选

#### 7.5.1 再跑一次编译/测试
```bash
./gradlew clean build test
```

**验证**:
- [ ] 编译通过（无错误、无警告）
- [ ] 所有测试通过
- [ ] 测试覆盖率 ≥ 70%

#### 7.5.2 产出 RC 构件
```bash
./gradlew build
```

**任务**:
- [ ] 生成发布候选构件（RC build）
- [ ] 可选：添加小型运行自检脚本
- [ ] 标记构件版本为 RC1

#### 7.5.3 手动回归测试
**参考**: `docs/FLYINGSWORD_MANUAL_TEST_CHECKLIST.md`

**关键测试点**:
- [ ] 召唤与召回
- [ ] AI 模式切换
- [ ] 战斗系统
- [ ] 资源消耗
- [ ] 多人同步
- [ ] 性能稳定性

### 7.6 文档更新

#### 7.6.1 更新实施计划文档
**位置**: `docs/FLYINGSWORD_IMPLEMENTATION_PLAN.md`
- [ ] 添加 Phase 7 实施总结
- [ ] 更新所有阶段状态标记

#### 7.6.2 更新 Master Plan
**位置**: `docs/FLYINGSWORD_MASTER_PLAN.md`
- [ ] 标记 Phase 7 为已完成
- [ ] 更新里程碑状态

## 代码行数预估

| 任务类型 | 预估变化 |
|---------|----------|
| 删除未引用代码 | -200~500 行 |
| 日志级别调整 | ±50 行 |
| TODO 清理 | -10~30 行 |
| 文档新增/更新 | +300~500 行 |
| **净变化** | **-100~+200 行** |

## 依赖关系
- ✅ 依赖 Phase 0-1 的功能开关
- ✅ 依赖 Phase 2 的系统抽取
- ✅ 依赖 Phase 3 的事件系统
- ✅ 依赖 Phase 4 的冷却与资源统一
- ✅ 依赖 Phase 5 的客户端优化
- ✅ 依赖 Phase 6 的文档与测试

## 验收标准

### 编译测试
```bash
./gradlew compileJava
./gradlew compileTestJava
```
- [ ] 编译通过
- [ ] 无新增警告
- [ ] 无未使用的导入

### 单元测试
```bash
./gradlew test
```
- [ ] 所有测试通过
- [ ] 测试覆盖率 ≥ 70%
- [ ] 无测试失败

### 代码质量
- [ ] 无 TODO/FIXME/XXX/HACK 注释（或已标注处理计划）
- [ ] 无调试代码（System.out.println 等）
- [ ] 日志级别合理（INFO 仅用于关键操作）
- [ ] 公开 API 注释齐全

### 一致性检查
- [ ] 无实体级冷却残留
- [ ] 无直接 ResourceOps 旁路
- [ ] 无旧 Goal/遗留路径
- [ ] 功能开关控制未使用功能

### 文档验证
- [ ] CHANGELOG 完整记录变更
- [ ] 迁移说明清晰
- [ ] Legacy 路径标注明确
- [ ] 所有文档链接可用

### 手动测试
- [ ] 核心功能正常（参考 MANUAL_TEST_CHECKLIST.md）
- [ ] 多人环境测试通过
- [ ] 性能无回退
- [ ] 默认配置下功能稳定

## 风险与回退

### 风险等级：低-中

**潜在问题**:

1. **删除代码导致意外破坏**
   - **影响**：删除了仍在使用的代码
   - **缓解**：使用 IDE 查找引用，谨慎删除
   - **回退**：保留删除列表，撤销提交

2. **日志级别调整过度**
   - **影响**：重要信息丢失，调试困难
   - **缓解**：保留关键操作的 INFO 日志
   - **回退**：恢复部分日志级别

3. **Legacy 路径标注不清**
   - **影响**：用户不知如何回退到旧行为
   - **缓解**：清晰文档化配置选项
   - **回退**：补充文档说明

4. **RC 构件问题**
   - **影响**：发布版本不稳定
   - **缓解**：充分手动测试
   - **回退**：发布 RC2 修复问题

**回退方案**:
- 所有删除操作记录在清单中，可逐条恢复
- 保留 git 分支，可整体回退
- 发布前充分测试，确保稳定性

## 不能做什么（红线约束）
- 不得删除任何被功能开关控制的代码（除非开关已废弃）
- 不得移除公开 API（保持向后兼容）
- 不得降低测试覆盖率
- 不得跳过手动回归测试
- 不得在 RC 版本引入新功能（仅清理和修复）

## 要做什么（详细清单）

### 代码清理
1. [ ] 扫描所有 TODO/FIXME/XXX/HACK 注释并处理
2. [ ] 查找所有 System.out.println 并移除或转为日志
3. [ ] 运行 IDE 优化导入功能，移除未使用的导入
4. [ ] 检查所有 INFO 日志，降低非关键日志到 DEBUG
5. [ ] 搜索并删除注释掉的旧代码块

### 一致性验证
1. [ ] 在 FlyingSwordEntity 中搜索"cooldown"字段
2. [ ] 在 FlyingSwordEntity 中搜索直接的资源访问代码
3. [ ] 搜索 ForceHuntTargetGoal 和 SwordGoalOps 引用
4. [ ] 验证 MultiCooldown 使用规范性

### 资源清理
1. [ ] 列出 ai/trajectory/ 下所有类
2. [ ] 列出 ai/intent/ 下所有类
3. [ ] 检查每个类的引用情况
4. [ ] 标记未使用或仅在开关控制下使用的类

### 文档工作
1. [ ] 创建 FLYINGSWORD_MIGRATION.md
2. [ ] 创建或更新 CHANGELOG.md
3. [ ] 在 FLYINGSWORD_STANDARDS.md 中标注 Legacy 路径
4. [ ] 更新 FLYINGSWORD_IMPLEMENTATION_PLAN.md
5. [ ] 更新 FLYINGSWORD_MASTER_PLAN.md
6. [ ] 完善 PHASE_7.md（本文档）

### 构建测试
1. [ ] 运行 `./gradlew clean`
2. [ ] 运行 `./gradlew build`
3. [ ] 运行 `./gradlew test`
4. [ ] 检查测试覆盖率报告
5. [ ] 执行手动回归测试

---

## Phase 7 实施进度

**开始时间**: 2025-11-06

**当前状态**: 🚧 进行中

### 已完成任务
- [ ] （待完成）

### 进行中任务
- [x] 文档规划（本文档编写）

### 待完成任务
- [ ] 代码清理
- [ ] 一致性验证
- [ ] 资源清理
- [ ] 文档工作
- [ ] 构建测试

---

**Phase 7 核心任务：最终清理与发布 - 进行中 🚧**
