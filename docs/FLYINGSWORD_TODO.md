# 飞剑模块裁剪与重构 TODO

## Phase 0｜接线与开关
- [ ] 在模组初始化路径调用 `compat/guzhenren/flyingsword/events/FlyingSwordEventInit.init()`（确保默认钩子注册）。
- [ ] 在 `compat/guzhenren/flyingsword/tuning/FlyingSwordTuning.java` 中新增布尔开关：
  - `ENABLE_ADVANCED_TRAJECTORIES`
  - `ENABLE_EXTRA_INTENTS`
  - `ENABLE_SWARM`
  - `ENABLE_TUI`
  - `ENABLE_GEO_OVERRIDE_PROFILE`
- [ ] ChestCavity 主类注册的 `SwordVisualProfileLoader`、`SwordModelOverrideLoader` 受 `ENABLE_GEO_OVERRIDE_PROFILE` 控制。

## Phase 1｜裁剪（先停用后清理）
- [ ] 按开关在 `ai/trajectory/Trajectories.java` 中仅保留 Orbit、PredictiveLine（可选 CurvedIntercept）等基础轨迹注册，其他轨迹转为关闭状态。
- [ ] 在 `ai/intent/planner/IntentPlanner.java` 为每个 AIMode 精简到 ≤2 条意图，其余受 `ENABLE_EXTRA_INTENTS` 控制。
- [ ] 移除旧式 Goal 追击路径：`ai/goal/ForceHuntTargetGoal.java` 与 `util/behavior/SwordGoalOps.java`。
- [ ] 将 `ai/command/SwordCommandCenter.java` 精简为核心指令，TUI 与高级战术由 `ENABLE_TUI` 控制；`CommandTactic` 缩减为 2–3 个基础战术。
- [ ] Gecko 模型覆盖/视觉档渲染支路仅在 `ENABLE_GEO_OVERRIDE_PROFILE` 为 true 时启用。
- [ ] `ai/swarm/QingLianSwordSwarm.java`、`tuning/QingLianSwarmTuning.java` 及相关调度入口受 `ENABLE_SWARM` 控制，缺省关闭；为 `domain/impl/qinglian/QingLianDomain.java` 提供降级路径。
- [ ] 将 `ops/RepairOps.java` 和命令集中的维护子命令置为 dev-only 或受开关控制。

## Phase 2｜分层重构
- [ ] 将“domain”概念替换为 `core` 包结构（例如 `core/entity/FlyingSwordEntity.java`、`core/types/FlyingSwordAttributes.java`）。
- [ ] 建立 `systems/` 目录，按 movement/combat/defense/blockbreak/targeting/progression/lifecycle 拆分职责并编写 README/注释说明。
- [ ] 将实体内的复杂逻辑（tick/hurt/mobInteract）移交到对应系统或事件钩子，仅保留上下文组装与事件触发。

## Phase 3｜事件模型扩展
- [ ] 新增 `ModeChangeContext`、`TargetAcquiredContext`、`TargetLostContext`、`UpkeepCheckContext`、`PostHitContext`、`BlockBreakAttemptContext`、`ExperienceGainContext`、`LevelUpContext` 等上下文。
- [ ] 在 `FlyingSwordEventRegistry` 中实现对应 fire 方法并保持短路语义一致。
- [ ] 确保新增事件在系统或控制器入口正确触发。

## Phase 4｜冷却与资源一致性
- [ ] 将实体级冷却字段迁移到 `MultiCooldown`（owner 附件），规范 key：`cc:flying_sword/<uuid>/attack` 等。
- [ ] 在 `tickServer` 中集中调用 `integration/resource/UpkeepOps`，触发 `OnUpkeepCheck`，并在 `FlyingSwordTuning` 中配置失败策略（停滞/减速/召回）。

## Phase 5｜客户端与网络
- [ ] 默认渲染路径仅保留 `FlyingSwordRenderer` + 默认 FX；Gecko/Override/Profile 仅在开关启用时注册。
- [ ] 检查网络消息，确保事件副作用尽量通过实体同步实现，必要提示复用现有载荷。

### 渲染姿态统一（Orientation 计划项，允许 Override）✅ Phase 8 完成
- [x] 引入 `OrientationOps`（建议放置 `compat/guzhenren/flyingsword/client/orientation/`）统一"朝向→姿态"计算为四元数：
  - `orientationFromForwardUp(Vec3 forward, Vec3 up, float preRollDeg, float yawOffsetDeg, float pitchOffsetDeg, OrientationMode mode, UpMode upMode)`
  - 基于正交基/right-hand 构建（right = normalize(up × forward)，trueUp = forward × right），避免欧拉顺序不对称。
- [x] `FlyingSwordRenderer` 增加可切换路径（默认启用）：
  - `FlyingSwordModelTuning.USE_BASIS_ORIENTATION = true` → 使用 `OrientationOps` 结果 `poseStack.mulPose(quat)`；
  - `false` → 走旧有欧拉 Y→Z 路径，保留兼容（Legacy）。
- [x] 偏移语义保持：`preRoll`（绕 forward）、`yawOffsetDeg`（绕 world Y）、`pitchOffsetDeg`（绕局部 X）；
  - 在 `SwordVisualProfile` / `SwordModelOverrideDef` 增加可选项 `orientationMode = BASIS | LEGACY_EULER`（默认 BASIS），`upMode = WORLD_Y | OWNER_UP`（默认 WORLD_Y）。
- [x] 近对向/竖直退化处理：
  - 若 `|forward × up| < eps`，选水平 fallbackUp（或基于上一帧 right 符号）避免退化；
  - 连续性策略：保持 half-turn 两侧 roll/pitch 对称，无"缓慢抬头"。
- [x] AimResolver 维持"纯方向"输出（不投影/不钳制 y），姿态稳定性交由 `OrientationOps` 处理。
- [ ] 文档：在 `FLYINGSWORD_STANDARDS.md` 记录"渲染姿态默认为 BASIS（正交基/四元数），欧拉路径标记为 Legacy"。（待完成）

#### 测试（新增）✅
- [x] `OrientationOpsTest`：
  - yaw ∈ [0, 360°) 扫描：forward=(cosθ,0,sinθ) → 生成姿态后断言 roll/pitch 在左右半圆对称、无累积性抬头；
  - 近对向 θ=180°±ε 不产生 NaN/异常大 pitch；
  - 微小 y 扰动（±1e-6）不随半圆累积被放大。
- [x] 已有 `KinematicsOpsEdgeCaseTest` 作为回归基线，联动验证 180° 对向时不会诱发"朝上"趋势。

## Phase 6｜文档与测试
- [ ] 编写 `compat/guzhenren/flyingsword/AGENTS.md`（或更新现有文档）描述事件模型、系统职责、冷却与资源约定。
- [ ] 更新 `ai/AGENTS.md`，记录精简后的意图/轨迹集合与扩展开关使用方法。
- [ ] 为 calculator、UpkeepOps、SteeringOps 等纯逻辑模块补充 JUnit 5 测试；执行 `./gradlew compileJava` 与 `./gradlew test` 验证。
 - [ ] （补充）新增 `OrientationOpsTest`，并在 `docs/stages/PHASE_6.md` 标注渲染姿态测试覆盖要求。

## Phase 7｜最终清理
- [ ] 在所有开关默认关闭且编译通过后，删除未引用的轨迹实现、模板以及对应枚举项。
- [ ] 删除 `ForceHuntTargetGoal`、`SwordGoalOps` 等已迁移的旧路径。
- [ ] 若 TUI/Swarm 长期默认关闭，则将相关代码迁至独立可选模块或示例插件，核心保留最小接口。
