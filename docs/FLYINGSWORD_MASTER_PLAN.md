# 飞剑模块重构｜总任务文档（Master Plan）

## 1. 项目整体目标
- 裁剪为“核心最小 + 事件驱动 + 可选扩展”的飞剑系统。
- 保持玩家体验与外部扩展的兼容性，降低维护成本与运行开销。

## 2. 里程碑（Milestones）
- M0：需求与方案确定（本次提交：需求方案 + 总计划）
- M1：功能开关与事件接线（Phase 0/1 完成，编译通过）
- M2：核心系统分层迁移（movement/combat/upkeep 就绪）
- M3：事件模型补全 + 冷却/资源一致性
- M4：客户端与指挥降噪（Gecko/TUI 可选）
- M5：单元测试完善 + 文档齐备
- M6：清理未引用代码，发布候选版本

## 3. 关键交付物（Deliverables）
- 需求方案：`docs/FLYINGSWORD_REQUIREMENTS.md`
- 重构执行计划：`docs/FLYINGSWORD_REFACTOR_PLAN.md`（✅ 已完成）
- AI 智能改进建议：`docs/FLYINGSWORD_AI_INTELLIGENCE_IMPROVEMENT.md`（✅ 已完成）
- TODO 裁剪清单：`docs/FLYINGSWORD_TODO.md`（✅ 已完成）
- 技术框架方案：`docs/FLYINGSWORD_TECH_FRAMEWORK.md`（待产）
- 红线文档：`docs/FLYINGSWORD_CONSTRAINTS.md`（待产）
- 规范文档：`docs/FLYINGSWORD_STANDARDS.md`（待产）
- 实施文档：`docs/FLYINGSWORD_IMPLEMENTATION_PLAN.md`（待产）
- 阶段任务文档：
  - `docs/stages/PHASE_0.md`（✅ 已完成详细规划）
  - `docs/stages/PHASE_1.md`（✅ 已创建）
  - `docs/stages/PHASE_2.md`（✅ 已创建）
  - `docs/stages/PHASE_3.md`（✅ 已创建）
  - `docs/stages/PHASE_4.md`（✅ 已创建）
  - `docs/stages/PHASE_5.md`（✅ 已完成详细规划并实施）
  - `docs/stages/PHASE_6.md`（✅ 已创建）
  - `docs/stages/PHASE_7.md`（✅ 已创建）
  - `docs/stages/PHASE_8.md`（✅ 已创建）
- 测试用例文档：`docs/FLYINGSWORD_TEST_CASES.md`（待产）

## 4. 阶段规划（Stage Plans 摘要）
- Phase 0｜接线与开关：事件初始化接线；添加开关（高级轨迹/扩展意图/Swarm/TUI/Gecko）。✅
- Phase 1｜裁剪：精简轨迹/意图；停用旧 Goal；TUI/Gecko/Swarm 默认关闭并可配置。✅
- Phase 2｜分层重构：建立 core+systems，削薄实体；迁移 movement/combat/upkeep。✅
- Phase 3｜事件扩展：ModeChange/TargetAcquired/Lost/UpkeepCheck/PostHit/BlockBreakAttempt 等。✅
- Phase 4｜冷却与资源：统一 MultiCooldown/ResourceOps，失败策略可配。✅
- Phase 5｜客户端与网络：渲染降噪；网络仅必要同步。✅
- Phase 6｜文档与测试：补文档与 JUnit5；门槛覆盖率≥70%。✅
- Phase 7｜最终清理：删除未引用实现，评审与发布。✅
- Phase 8｜姿态统一：OrientationOps 上线，根治"半圆抬头"，保留 Legacy 回退。📋

## 5. 依赖关系
- Phase 1 依赖 Phase 0。
- Phase 2 依赖 Phase 1（可与 Phase 3/4 局部并行）。
- Phase 6 贯穿全程但在 Phase 2 后集中推进。

## 6. 验收标准（全局）
- 编译与测试：`./gradlew compileJava`、`./gradlew test` 通过。
- 体验不回退：ORBIT/GUARD/HUNT/RECALL 行为正确；召回入库/离线入库稳定。
- 性能与稳定：默认开关关闭下，实体 Tick 平稳，无负冷却/资源。
- 扩展性：事件钩子可订阅/短路；文档与示例完整。

## 7. 角色与责任
- 产品/玩法把关：确认默认开关与功能留存。
- 工程实现：按阶段推进、保持最小改动、单元测试补齐。
- 审核与发布：每里程碑评审后合入主线。

## 8. 风险与回退
- 大型迁移：分系统逐步迁、保留回退分支；功能开关可一键回滚。
- 兼容性：确保 `api/` 与事件上下文契约稳定；提供迁移说明。

---

> 注：阶段任务细节、任务清单与依赖，请参考 `docs/FLYINGSWORD_REFACTOR_PLAN.md` 与 `docs/FLYINGSWORD_TODO.md`，并在 `docs/stages/` 下逐步产出具体阶段文档。
