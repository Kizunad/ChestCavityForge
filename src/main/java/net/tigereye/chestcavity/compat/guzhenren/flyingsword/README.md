# 飞剑模块结构

本目录下的代码围绕“纯飞剑逻辑 + 跨系统桥接”的分层展开：

- `core/`：实体、控制器、类型与属性定义。仅依赖通用 Minecraft API。
- `ai/`：包含 Intent／Trajectory／行为规则。只负责决策，生成期望速度或命令。
- `motion/`：运动学与转向执行层（KinematicsSnapshot、SteeringOps 等），统一速度与转向限制。
- `ops/`：飞剑自身的可复用操作（破块、修复、音效等），不触碰外部系统。
- `integration/`：承载与外部系统的桥接逻辑。
  - `integration/domain/`：当前仅有 `SwordSpeedModifiers`，用于读取领域状态并调整速度。
  - `integration/resource/`：`UpkeepOps` 负责真元扣减与维持消耗，与 `ResourceOps` 对接。
  - 预留 `integration/rift/` 等目录，用于后续裂隙等系统接入。
- `client/`：渲染、模型覆写、特效等客户端组件（粒子、音效资源绑定仍在 `ops/SoundOps` 中）。
- `calculator/` 与 `tuning/`：纯数值计算和配置，不包含外部依赖。
- `events/`：飞剑专属事件钩子与上下文。

## 编辑指引
1. 若需要读取道痕、领域等外部数据，请把逻辑放在 `integration/` 子模块中，然后在核心或 AI 层通过该接口获取结果。
2. 纯运动或战术行为请保持在 `motion/` 与 `ai/` 中，确保可检测、可复用。
3. 扩展指挥棒或事件处理时，优先走 `events/FlyingSwordEventRegistry` 钩子，让外部模块注册监听器，而不是直接在实体里写死逻辑。
4. 更新后请执行 `./gradlew compileJava` 验证架构调整是否正常。
