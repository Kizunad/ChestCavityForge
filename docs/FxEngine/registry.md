# FxRegistry · 注册/调用规范

## 目标

- 通过“先注册（Register），后调用（Play）”的模式，在多处复用相同 FX 方案，减少重复实现与耦合。

## API（设计稿）

- register(id, factory)
  - id: ResourceLocation 风格字符串（见 standards.md）
  - factory: (FxContext) -> FxTrackSpec（纯函数，按上下文构建规格）
- unregister(id)
- play(id, context) -> FxHandle
  - context: { level, ownerId?, waveId?, position?, customParams }

## 建议用法

- 在模块初始化或 FX 实现类装配时统一注册：
  - FxRegistry.register("chestcavity:fx/shockfield/ring", ctx -> buildRingSpec(ctx))
- 在任意 Shockfield 回调中调用：
  - FxRegistry.play("chestcavity:fx/shockfield/ring", FxContext.from(state, level))

## mergeKey 与 waveId

- factory 中建议以 waveId 构造 mergeKey（如 "shockfield:ring@"+waveId）实现同一波场 FX 合并/续期。

## 预算与限流

- play 时仍走 FxScheduler 的预算与限流路径（当预算开启时）；
- 注册中心仅提供模板与构建，不绕过引擎能力。

