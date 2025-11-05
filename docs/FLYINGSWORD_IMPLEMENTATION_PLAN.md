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

