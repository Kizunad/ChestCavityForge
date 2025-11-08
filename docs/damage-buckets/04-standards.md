# 规范文档（Standards）— OnHit 聚合接口与伤害桶

## 命名与结构
- 包路径建议：`net.tigereye.chestcavity.util.damage`（聚合上下文/工具）
- 接口命名：`OnHitModifier`（聚合接口）、`AggregationContext`（上下文）
- 标识命名：使用 `ResourceLocation` 或类名+常量作为“已聚合标记”的 Key，避免冲突。

## 字段语义与单位
- 百分比增伤采用小数表示：`0.25` 表示 +25%。
- 平直加伤单位与事件伤害一致（float）。
- 任何字段默认值为 0；不允许使用负值抵消，改用 clamp。

## 配置与日志
- 配置键：`UNIFY_ONHIT_BUCKETS`、`DEBUG_ONHIT_AGGREGATION`。
- 日志等级：调试信息使用 DEBUG；异常使用 WARN；禁止 INFO 级高频日志。

## 代码风格
- 保持与项目现有 Java 风格一致（大括号、缩进、命名约定）。
- 公共入口方法添加 Javadoc，描述输入/输出与副作用。
- 不在实现中嵌入业务器官的常量/ID，保持通用性。

## Git 提交规范
- `feat(damage-buckets): ...` 引入新聚合接口与上下文
- `docs(damage-buckets): ...` 文档与示意
- `refactor(onhit): ...` 接入聚合阶段（不包含具体器官迁移）

## 文档规范
- 关键变更需在 `docs/damage-buckets/` 下更新对应文档。
- Mermaid 图使用简洁节点与箭头，避免过度复杂。

