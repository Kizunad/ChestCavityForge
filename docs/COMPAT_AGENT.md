# 通用兼容工具库（Agent Toolkit）

目标：为“玩家 + 非玩家（NPC/怪物）”提供统一的兼容入口，降低行为/技能/效果对 `ServerPlayer` 的硬依赖，便于将既有逻辑自然拓展到非玩家生物。

核心组件
- `compat/common/agent/Agent`：面向任意 `LivingEntity` 的轻量封装。读取 CC 实例、服务器 tick、尝试打开资源句柄。
- `compat/common/agent/Agents`：静态工具集。统一属性修饰、护盾应用、资源消耗与库存“流派标签”的收集。
- `compat/common/agent/AgentContext`：非技能专用的上下文对象，玩家/NPC 通用；含 `serverTime`、CC 实例与“流派标签”快照。
- `compat/common/agent/AgentTempState`：运行期（内存）临时状态，提供属性修饰器的 TTL 管理与回滚辅助。

常见用法片段
```java
// 统一获取 Agent（玩家/NPC 同源）
Agents.from(entity).ifPresent(agent -> {
  long now = agent.serverTime();
  ChestCavityInstance cc = agent.chestCavity();
});

// 安装一个临时属性修饰器（10 秒），到点自动回滚
AgentTempState.applyAttributeWithTTL(
    living, Attributes.MOVEMENT_SPEED, ChestCavity.id("demo/sprint"), 0.15,
    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 200);

// 统一护盾应用
Agents.applyAbsorption(living, 8.0, ChestCavity.id("demo/shield"), true);

// 资源：按真元标量消费（玩家/NPC 通用）
Agents.tryConsumeScaledZhenyuan(living, 12.0);

// 收集本次“流派标签”信息（可用于条件判断/提示）
AgentContext ctx = AgentContext.of(living);
if (ctx.hasFlow("冰雪道")) { /* ... */ }
```

接入建议
- 在新逻辑中优先依赖 `LivingEntity` + `Agents/AgentContext`，减少对 `ServerPlayer` 的直接引用。
- 玩家专属分支（如聊天提示、客户端动画）应隔离在“可选路径”中；核心计算与数据流保持实体无关。
- 与器官行为相关的冷却/状态，若与物品位绑定，优先继续使用 `OrganStateOps/MultiCooldown`；其余仅运行期的临时增益可用 `AgentTempState`。

后续扩展位
- 提供统一的事件钩子（如死亡/离线）来自动调用 `AgentTempState.cleanupAll(entity)`。
- 为技能系统提供 `AgentEffectContext` 与非玩家触发的 Effect 总线封装。

