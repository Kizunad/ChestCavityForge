# JianQiGu 剑光 Fx 设计（Projectile 绑定扁平光刃方案）

本方案给出一套**高性价比**实现：在 Projectile Renderer 中渲染摄像机朝向的扁平剑光 Quad，并根据“当前威能”动态缩放长度/宽度/亮度，同时在边缘撒少量粒子。

此文档为实现指导，具体代码应按本模块 AGENTS 规范落地在对应 Java 类中。

---

## 1. 总体思路

- 使用一个 **轻量 Projectile 实体**（或复用已有 `SwordSlashProjectile` 风格实体）。
- 在服务端实体中维护：
  - 初始威能 `initialPower`
  - 当前威能 `currentPower`
  - 最大威能 `maxVisualPower`（用于归一化，可直接用初始值）
- 客户端 Renderer：
  - 对该 Projectile 渲染一个始终与其运动方向对齐的“剑光条”Quad。
  - Quad 的长度、宽度、透明度和颜色强度根据 `currentPower / initialPower` 动态调整。
  - Quad 面向摄像机（billboard/半 billboard），保证观感。
  - 边缘添加少量粒子增强动感。

---

## 2. Projectile 侧字段与同步

建议为剑气斩击定义/扩展一个实体（示例）：

- 类名建议：`JianQiGuSlashProjectile`
- 关键字段（服务端为主，客户端只读）：

```java
// 仅示意：实际实现放在 JianQiGuSlashProjectile.java
private float initialPower;   // 激活时记录，通常是第一段伤害或归一化威能
private float currentPower;   // 随命中/衰减更新
```

同步方式（高性价比）：

- 初始威能：
  - 在构造/发射时通过数据管理器（DataAccessor/EntityDataAccessor）下发到客户端。
- 当前威能（用于 Fx 动态缩放）：
  - 可选方案 A：客户端不实时同步，用 `age / maxAge` 或基于飞行进度推导衰减，简单稳定。
  - 可选方案 B：在命中时通过内建同步（如 entity data）更新 `currentPower`，更精确但略复杂。

推荐：

- 将“视觉威能”独立为 `visualPower`：
  - 服务端发生命中/衰减时更新 `visualPower` 到 EntityData，客户端 Renderer 只需读一个 float 即可。

---

## 3. Renderer：扁平剑光 Quad 实现步骤

你可以参考现有 `FlyingSwordRenderer` 和 `SwordSlashRenderer` 的写法，创建专用渲染器，例如：

- `JianQiGuSlashRenderer`（在 [`JiandaoClientRenderers`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/JiandaoClientRenderers.java:15) 中注册）

核心渲染逻辑（伪代码级步骤说明）：

1. 在 `render` 方法中获取基础数据：
   - `float partialTicks`
   - `PoseStack poseStack`
   - `MultiBufferSource buffer`
   - 实体 `proj`：
     - 位置：`x, y, z`
     - 方向：根据 `proj.getDeltaMovement()` 或 `proj.getYRot()/getXRot()`
     - `visualPower`：从 Entity Data 获取，范围建议 `[0, 1]` 或直接 `[0, initial]` 后归一化

2. 计算视觉参数（跟随威能动态缩放）：

   - 归一化威能：
     - `float p = clamp(visualPower / maxVisualPower, 0.0F, 1.0F);`
   - 长度：
     - `float baseLen = 3.0F;`
     - `float len = baseLen + p * 5.0F;` // 威能越高越长
   - 宽度（厚度）：
     - `float baseWidth = 0.15F;`
     - `float width = baseWidth + p * 0.25F;`
   - 透明度：
     - `float alpha = 0.35F + 0.4F * p;`
   - 颜色：
     - 基础：淡蓝/青白剑光，例如 `(r,g,b) = (0.7F + 0.3F*p, 0.9F, 1.0F)`
     - 可在高威能时略偏蓝白，高亮。

3. 对齐剑光方向：

   - 使用实体朝向旋转 poseStack：
     - 沿 Y 轴旋转至投射物水平朝向。
     - 沿 X 轴旋转至俯仰角。
   - 再做一个 **朝向摄像机的微调**：
     - 可使用与 `RenderType.entityCutout` + `Camera` 类似的 billboarding 方式，使 Quad 面向相机，避免平面过薄。
     - 高性价比实现：仅围绕自身朝向法线旋转，使剑光在视角中始终有一定可见宽度。

4. 绘制 Quad：

   - 在局部坐标系下构造四个顶点：
     - 沿前方 Z 方向布置长度 `len`。
     - 沿左右 ±`width` 布置高度/宽度。
   - 使用带 Additive/Translucent 的 `RenderType`：
     - 推荐类似 `RenderType.entityTranslucentCull` 或自定义 additive 类型，确保剑光发光感。
   - 使用一张高宽比纹理（如 256x32）的剑光贴图：
     - 中心高亮，边缘渐隐。
     - U 方向拉伸，V 控制亮度渐变。
   - 在顶点写入：
     - 颜色 `(r, g, b, alpha)`
     - UV 坐标
     - Overlay/Light 值（可提高 light 值模拟自发光）。

5. 增加简单动画：

   - 利用 `age` 和 `partialTicks`：
     - 让 UV 在 U 方向轻微滚动，形成“能量流动”效果。
     - 或让 `alpha` 在末端略微波动。

---

## 4. 边缘粒子方案（增强但不重）

为避免大量粒子，可以：

- 在客户端的 `tick` 或 Renderer 内（谨慎）触发**低频率**粒子：
  - 每隔数 tick：
    - 在剑光中段或尾端附近随机一点生成 1-2 个粒子。
  - 粒子类型：
    - 使用已有简洁粒子（如小白色光点）或自定义轻量粒子。
  - 粒子行为：
    - 寿命短（5-10 tick）。
    - 略向外扩散。
    - Alpha 从 0.6 衰减到 0。

粒子触发逻辑建议集中到专门的 Fx 工具类：

- `JianQiGuFx.spawnSlashTrailParticles(proj, visualPower, level)`：
  - 根据 `visualPower` 控制粒子数量（p 越高粒子稍多）。

---

## 5. 威能联动规则（可直接采用）

高性价比可行的“跟随威能缩放”绑定规则：

- `visualPower` 定义：
  - 初始：`visualPower = 1.0F`
  - 每次命中后：
    - 服务端按伤害衰减公式更新 `currentPower`，并同步为 `visualPower = currentPower / initialPower`。
- 渲染层使用 `p = visualPower`：
  - 长度：`len = baseLen + p * extraLen`
  - 宽度：`width = baseWidth + p * extraWidth`
  - 透明度：`alpha = baseAlpha + p * extraAlpha`
  - 颜色：可略随 p 调整，以突出高威能时更刺眼、更长的剑光。

这样：

- 高威能时：剑光长、粗、亮，粒子略多。
- 威能被多段命中削弱后：剑光逐渐变短变细变暗，直观反映伤害衰减。

---

## 6. 实现落点建议（与现有结构对齐）

建议创建以下实际代码类（在 Java 中，不在本 md 内）：

- [`JianQiGuFx`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/fx/JianQiGuFx.java:1)
  - 方法：
    - `applySlashVisual(JianQiGuSlashProjectile proj, float visualPower)`：用于集中处理粒子/特效调用（如有需要）。
- `JianQiGuSlashProjectile`（或在现有 Projectile 上扩展字段）：
  - 管理 `initialPower`、`currentPower` / `visualPower`，负责在命中时更新。
- `JianQiGuSlashRenderer`
  - 读取 `visualPower` 并按上述步骤渲染 Quad。
  - 在 [`JiandaoClientRenderers.onRegisterRenderers`](src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/JiandaoClientRenderers.java:15) 中注册。

此方案实现成本低，性能开销小，且能够做到：

- 一道清晰的剑光斩击。
- 随威能放大/缩小（长度、宽度、亮度、粒子密度联动）。
- 表达“多段命中后剑气衰减”的视觉反馈。
