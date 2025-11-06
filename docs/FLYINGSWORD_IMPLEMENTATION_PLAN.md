# 飞剑模块重构｜实施文档（Implementation Plan）

> 依据：需求方案（Requirements）、技术框架（Tech Framework）、红线（Constraints）、规范（Standards）。

## 1. 范围与目标
- 裁剪复杂度，保留核心最小集合，事件化扩展点完整。
- 分阶段迁移系统逻辑，保证功能不回退、性能稳定。

## 2. 执行路线
1) Phase 0/1：接线与开关 → 裁剪注册
2) Phase 2：分层迁移 movement/combat/upkeep，削薄实体
3) Phase 3：新增关键事件（Mode/Target/Upkeep/PostHit/BlockBreakAttempt）
4) Phase 4：冷却/资源一致性（MultiCooldown + ResourceOps）
5) Phase 5：客户端/指挥降噪（Gecko/TUI 开关化）
6) Phase 6：测试与文档完善
7) Phase 7：清理未引用与发布

## 3. 准入清单（每阶段）
- 编译通过：`./gradlew compileJava`
- 纯逻辑测试：`./gradlew test`
- 人工验收：核心交互（召唤/环绕/拦截/命中/受击/召回）
- 日志：默认静音；必要时启用 DEBUG 标志

## 4. 风险与回退
- 大改动以开关控制；失败可切回原行为（开启扩展意图/轨迹/TUI/Swarm）
- 系统迁移逐块进行；每块迁移后回归测试再继续

## 5. 指标与观测
- tick 预算、目标搜索次数、命中率、Upkeep 失败次数；按阶段对比记录
- 决策日志（候选意图/得分）仅在 DEBUG 开启时输出

## 6. 角色协作
- 产品：确认默认开关与体验把关
- 工程：实现与回归验证
- 维护：文档/测试更新、基线记录

## 7. 验收标准（总）
- 事件短路与后置只读语义正确
- ORBIT/GUARD/HUNT/RECALL 行为无回退
- 冷却与资源集中、无负值与残留
- 默认关闭高级项时性能可接受
- **测试覆盖率 ≥ 70%**（Phase 6 新增）
- **核心功能手动测试通过**（Phase 6 新增）

## 8. Phase 6 实施总结（2025-11-06）

**阶段目标**：文档与测试完善

**文档更新**：
- ✅ 补充 `FLYINGSWORD_STANDARDS.md` 测试规范章节（+120 行）
- ✅ 创建 `FLYINGSWORD_MANUAL_TEST_CHECKLIST.md` 手动测试清单（+350 行）
- ✅ 更新 `PHASE_6.md` 详细任务文档（+450 行）

**单元测试新增**：
- ✅ `FlyingSwordCalculatorTest.java`（+550 行）
  - 覆盖伤害计算、维持消耗、经验计算、耐久损耗
  - 测试边界条件和实际场景
  - 包含 30+ 测试用例
- ✅ `UpkeepOpsTest.java`（+400 行）
  - 覆盖区间消耗计算、模式倍率、状态倍率、速度影响
  - 测试边界条件和精度
  - 包含 25+ 测试用例
- ⚠️ `SteeringOpsTest.java`（跳过 - 需要 Minecraft 环境）
- ⚠️ `CombatSystemTest.java`（跳过 - 需要 Minecraft 环境）

**测试覆盖率**：
- 目标：≥ 70%
- 预计：核心计算器 ≥ 85%，资源消耗 ≥ 80%
- 验证：`./gradlew test`

**代码变更统计**：
- 新增测试代码：~950 行
- 新增文档：~920 行
- 总计：~1,870 行（测试与文档完成）

**验收状态**：
- [x] 文档齐备（需求、框架、约束、规范、实施、阶段）
- [x] 单元测试覆盖核心纯函数
- [x] 手动测试清单完整
- [ ] 测试执行并通过（待验证）
- [ ] 覆盖率达标（待验证）

**下一步（Phase 7）**：
- 清理未引用代码
- 删除旧式 Goal 系统
- 评审与发布准备

