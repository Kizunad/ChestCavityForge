# FxEngine 时间线系统 · 规范（Standards）

## 代码风格

- 遵循项目现有格式化配置；方法短小、单一职责；回调中避免复杂分支。

## 命名约定

- 引擎/接口：FxTimelineEngine、FxScheduler、FxTrack、FxTrackSpec、StopReason。
- 配置：fxEngine.enabled、fxEngine.budget.enabled、fxEngine.budget.perLevelCap、fxEngine.budget.perOwnerCap。
- 合并键：mergeKey 采用 “domain:kind@owner/wave” 格式，便于排查。
- 注册ID：fxId 使用命名空间+路径，如
  - chestcavity:fx/shockfield/ring
  - chestcavity:fx/shockfield/subwave_pulse

## 目录结构（文档）

- 设计文档：docs/FxEngine/
  - requirements.md / tech-framework.md / constraints.md / standards.md
  - implementation.md / master-plan.md / stages/*.md / tests.md

## Git 提交规范

- 前缀：[fxengine]；范围：docs/engine/core/test；语义清晰，一次只做一件事。
- 附带简述预算/限流/合并策略变更点；关联任务或阶段文档。

## 文档规范

- Mermaid 图用于流程/关系；少量且清晰。
- 变更同步更新相关章节与阶段计划。
