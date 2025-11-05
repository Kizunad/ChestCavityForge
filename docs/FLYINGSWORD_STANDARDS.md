# 飞剑模块重构｜规范文档（Standards）

## 1. 代码风格与命名
- Java 风格统一：与仓库现有 Checkstyle/Spotless 保持一致（若存在）。
- 命名：
  - 事件类/上下文：`*Context`、`*Hook`、`*Registry`；前置事件动词为 `onXxx`。
  - 系统类：`XxxSystem`（Movement/Combat/Defense/BlockBreak/Targeting/Progression/Lifecycle）。
  - 配置常量：`FlyingSwordTuning`；布尔开关以 `ENABLE_*` 约定。
  - MultiCooldown Key：`cc:flying_sword/<uuid>/<domain>`。

## 2. 目录结构
```
compat/guzhenren/flyingsword/
  api/  core/  systems/  events/  integration/  ai/  client/  tuning/
  ops/  calculator/  ui/
```
- api：对外稳定入口；core：实体与属性；systems：服务端系统；events：上下文与注册；integration：资源/冷却；ai：意图与轨迹；client：渲染；tuning：常量与配置。

## 3. 事件约定
- 前置事件可取消/改参；后置事件只读。
- Fire 顺序按注册顺序；短路遵守 `ctx.cancelled/cancelDefault/preventDespawn`。
- 默认钩子实现不得假设其他钩子存在；失败需捕获异常、降噪。

## 4. 资源与冷却
- 维持消耗只在 UpkeepSystem 中调用 `ResourceOps`，并触发 `OnUpkeepCheck`。
- 冷却统一 `MultiCooldown`；实体内字段仅镜像。

## 5. Git 提交规范
- 类型建议：feat / fix / refactor / docs / test / chore
- 范围：`[flyingsword]` 前缀；一句话概述 + 必要说明；涉及开关/事件需在描述中标注。

## 6. 文档编写
- 需求/框架/红线/规范/实施/阶段/测试文档置于 `docs/`；阶段文档置于 `docs/stages/`。
- Mermaid 图用于流程/序列说明；避免大段代码示例。

## 7. 测试约定
- 仅测试纯逻辑（calculator/motion/integration/systems 内可抽象部分）。
- JUnit5 + Mockito（受限于 MC 类不可 mock）；以“输入-输出”断言为主。
- `./gradlew test` 或 `./scripts/run-tests.sh`。

