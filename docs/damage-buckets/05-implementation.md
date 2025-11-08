# 实施文档 — OnHit 聚合接口与伤害桶

说明：本实施文档仅覆盖“聚合接口与桶”的引入与接线，不包含具体器官/行为迁移。

## 实施范围
- 新增：聚合上下文、聚合接口、配置与调试、兼容期桥接机制。
- 改动：`ChestCavityUtil.onHit` 增加“聚合阶段 + 一次性应用 + 兼容阶段”。
- 不改动：`onIncomingDamage`、DoT/Reaction 管线、器官具体实现。

## 步骤
1) 定义聚合上下文与接口
   - `AggregationContext { baseDamage, sameBucketPercent, flatBonus, notes? }`
   - `OnHitModifier.contribute(ctx, inputs)`（inputs 为只读快照）

2) 接入聚合阶段
   - 在 `ChestCavityUtil.onHit` 进入时创建 `ctx`，扫描实现 `OnHitModifier` 的监听器并收集贡献。
   - 计算一次性应用：`damage = damage * (1 + ctx.sameBucketPercent) + ctx.flatBonus`。

3) 兼容阶段
   - 继续调用未迁移的 `OrganOnHitListener`；对已迁移者根据标记跳过，避免重复。

4) 配置与调试
   - 新增配置键：`UNIFY_ONHIT_BUCKETS`（默认 false）、`DEBUG_ONHIT_AGGREGATION`（默认 false）。
   - DEBUG 时输出聚合摘要（限流）。

5) 容错
   - 聚合异常 -> 记录一次 WARN 并回退到旧管线。

6) 单元测试（纯逻辑）
   - 输入：不同贡献组合（百分比/flat/空），预期输出：正确聚合一次。
   - 边界：负值/NaN/∞、大额 flat、零伤害。

## 验收标准
- 默认关闭时，行为与性能与当前线上一致。
- 打开后，存在至少一个“示例贡献者”进入同乘区（可用最小模拟实现），聚合计算正确。
- CI 中新增的聚合单测全部通过。

## 回滚策略
- 任意问题可通过关闭 `UNIFY_ONHIT_BUCKETS` 立刻回退旧管线。

## 风险与缓解
- 风险：与旧监听器重复增伤。
  - 缓解：显式标记已迁移者；兼容阶段识别并跳过。
- 风险：日志噪音或性能退化。
  - 缓解：默认关闭调试；严控对象分配；增加基准测试样例。

