# Phase 8｜渲染姿态统一与“半圆抬头”治理

## 阶段目标
- 以“正交基/四元数”为核心统一飞剑的姿态计算接口，默认取代现有欧拉 Y→Z 路径；
- 根治 +X ↔ -X 半圆飞行时出现的“缓慢抬头/半圆不对称”问题；
- 保持资源/视觉档兼容，并提供可选 Legacy 回退；
- 为后续扩展（OWNER_UP、定制上向、模型覆写）提供稳定挂点。

## 依赖关系
- 依赖 P5 完成（渲染与网络路径降噪）与 P6（测试框架、边界用例）；
- 可与常规 bugfix 并行进行，建议单独分支。

## 任务清单

### 8.1 引入 OrientationOps（四元数姿态计算）
位置：`compat/guzhenren/flyingsword/client/orientation/OrientationOps.java`

API（建议）：
```java
public final class OrientationOps {
  public static Quaternionf orientationFromForwardUp(
      Vec3 forward,
      Vec3 up,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      OrientationMode mode,     // BASIS(默认) | LEGACY_EULER
      UpMode upMode             // WORLD_Y(默认) | OWNER_UP
  );
}
```

实现要点：
- 基于正交基：`forwardN = normalize(forward)`；`right = normalize(up × forwardN)`；`trueUp = forwardN × right`；
- 退化处理：若 `|up × forward| < eps`，选水平 fallbackUp（或基于上一帧 right 符号）；
- 偏移应用：
  - `preRoll`：绕 forward 轴旋转；
  - `yawOffsetDeg`：绕 world-Y 旋转；
  - `pitchOffsetDeg`：绕局部 X 旋转；
- 返回四元数，供 `poseStack.mulPose(quat)` 直接使用。

### 8.2 Renderer 切换与兼容
- 在 `FlyingSwordRenderer` 增加开关：`FlyingSwordModelTuning.USE_BASIS_ORIENTATION = true`（默认 true）。
- `true`：通过 `OrientationOps` 计算姿态；`false`：沿用现有欧拉 Y→Z 路径（Legacy）。
- 视觉档/模型覆写保持字段：`preRollDeg/yawOffsetDeg/pitchOffsetDeg`；新增可选：
  - `orientationMode = BASIS | LEGACY_EULER`（默认 BASIS）
  - `upMode = WORLD_Y | OWNER_UP`（默认 WORLD_Y）

### 8.3 AimResolver 约束
- AimResolver 继续只返回“纯方向”（不对 y 分量投影/钳制），姿态稳定性交由 `OrientationOps` 负责。

### 8.4 测试与回归
- 新增 `OrientationOpsTest`：
  - yaw ∈ [0, 360°) 扫描：forward=(cosθ,0,sinθ) → 断言 roll/pitch 在左右半圆对称，无累积性抬头；
  - 近对向 θ=180°±ε 不产生 NaN/异常大 pitch；
  - 微小 y 扰动（±1e-6）不随半圆累积放大；
- 与现有 `KinematicsOpsEdgeCaseTest` 联动作为回归基线。

### 8.5 文档与迁移说明
- `FLYINGSWORD_STANDARDS.md`：默认姿态计算切换为 BASIS，欧拉路径标记为 Legacy；
- `FLYINGSWORD_TODO.md`：将 Orientation 计划项迁移至 Phase 8 完成项；
- 若对外暴露 Profile/Override schema，补充字段说明与兼容策略。

## 验收标准
- 视觉：
  - +X ↔ -X 连贯半圆飞行不再出现“缓慢抬头”；
  - 全方向巡航时 roll/pitch 分布对称，左右半圆无系统性偏差；
  - TARGET/OWNER 对齐模式保持合理、无突跳；
- 稳定性：
  - 不出现 NaN/Inf；
  - 近竖直/近对向不退化为“朝上”或“朝下”的异常姿态；
- 兼容性：
  - 旧资源在 `LEGACY_EULER=false` 下外观基本一致；
  - 将开关置为 `false` 时，行为与当前版本一致。

## 风险与回退
- 风险：少量旧资源对欧拉顺序有依赖，切换后视觉略差异；
- 回退：`FlyingSwordModelTuning.USE_BASIS_ORIENTATION = false` 或 Profile `orientationMode=LEGACY_EULER`。

## 实施记录
- 计划创建日期：2025-11-06
- 关联问题：飞行半圆“缓慢抬头”与欧拉顺序不对称

