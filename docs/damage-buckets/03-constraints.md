# 红线文档（Constraints）— OnHit 聚合接口与伤害桶

## 架构/流程红线
- 不改变 `LivingIncomingDamageEvent` 的订阅点与触发顺序；不更改受击侧 `onIncomingDamage` 的调用次序。
- 不突破 BYPASS 语义：带有 `BYPASS_ORGAN_HOOKS` 等标签的伤害必须跳过聚合与监听链路。
- 不将 DoT、Reaction 的调度与结算顺序混入 OnHit 聚合中。

## 技术红线
- 不引入新第三方依赖；不修改 Minecraft/NeoForge 核心类。
- 不使用反射/动态代理篡改现有监听器行为。
- 不在服务端主线程之外运行聚合逻辑；禁止跨线程共享可变状态。

## 代码实践红线
- 监听器中禁止直接 `setAmount` 或绕过管线写事件；所有改动统一在聚合/兼容阶段生效。
- 禁止在聚合阶段直接改写 damage；必须通过 `sameBucketPercent/flatBonus` 汇总。
- 禁止在默认配置下输出高频日志；调试日志必须受开关控制并限流。

## 安全红线
- 不进行任何网络请求或外部 IO。
- 不读取或写入与本逻辑无关的磁盘文件。

## 性能红线
- 单次命中聚合的平均耗时应低于 50 微秒（本地评估），严禁引入 O(n^2) 操作。
- 禁止在聚合阶段构造大对象（如 Map、List）除非 DEBUG 模式。

## 兼容性红线
- 对旧监听器的调用语义必须保持一致，除非显式被识别为“已迁移”并跳过以避免重复。
- 不改变 `OrganIncomingDamageListener`（受击）侧现有数值逻辑与顺序。

