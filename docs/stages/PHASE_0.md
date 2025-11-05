# Phase 0｜接线与开关

## 阶段目标
- 初始化事件系统；添加特性开关并接入（不改默认行为）。

## 任务列表
- 在 mod 构造流程调用 `FlyingSwordEventInit.init()`。
- 在 `FlyingSwordTuning` 新增开关：ADV_TRAJ/EXTRA_INTENTS/SWARM/TUI/GEO_OVERRIDE。
- ChestCavity 主类注册的 client loader 受开关控制。

## 依赖关系
- 无（起点阶段）。

## 验收标准
- 编译通过；默认开关=关闭时游戏可运行。
- 事件默认钩子（DefaultEventHooks/SwordClashHook）注册成功。

## 风险与回退
- 低；如失败，临时绕过 init 调用与开关读取。

