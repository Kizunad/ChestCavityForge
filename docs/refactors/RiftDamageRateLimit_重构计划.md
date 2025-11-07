# 裂隙/共鸣伤害限频节流 重构计划

本文档规划“对同一目标的裂隙/共鸣伤害做全局限频（如每 10 tick 最多结算一次）”的技术方案与落地步骤，避免频繁刷新受伤冷却导致近战手感变差的问题。

---

## 背景与问题
- 现状：裂隙（RiftEntity）每秒对范围目标造成魔法伤害；共鸣波（RiftManager.dealResonanceWaveDamage）在链路中对多目标造成伤害。
- 场景：被动微裂隙叠加 + 剑气共鸣传播，目标身上“十多点紫色伤害”高频出现。
- 负面效果：频繁小额伤害不断刷新目标受伤冷却，导致玩家随后近战攻击易被“冷却窗口吞伤”，体验像“a不到”。

## 目标与非目标
- 目标
  - 对“同一目标”来自“裂隙/共鸣波”的伤害进行全局节流：同一目标在 `WINDOW_TICKS` 内只结算一次该类伤害。
  - 限频只针对“裂隙系统内伤害”，不影响其他来源（近战、道具、环境）的正常结算。
  - 在保留总伤期望的前提下，降低“刷新受伤冷却”的频度，改善近战手感。
- 非目标
  - 不改变裂隙/共鸣的定位、总体强度、视觉/音效逻辑。
  - 不引入玩家端特例（如对玩家豁免），优先保持系统一致性。

## 设计方案（RateLimiter）
### 核心思路
- 在服务端为“裂隙系统伤害”维护一份“目标 → 上次命中 tick”的速率限制表，只要在窗口内则跳过本次伤害。
- 窗口建议值：`WINDOW_TICKS = 10`（0.5s），可配置。

### 数据结构与归属
- 归属：`RiftManager`（全局统一，便于在“裂隙穿刺伤害”和“共鸣波伤害”两个入口复用）。
- 结构：`Long2LongOpenHashMap` 或 `Object2LongOpenHashMap<UUID, long>`（若不引 F&H，可用 `ConcurrentHashMap<UUID, Long>`）。
- Key：目标实体 UUID。
- Value：世界时间 `gameTime` 的上次命中 tick。
- 清理：
  - 被动惰性清理：在写入新命中时同时剔除过旧条目（`now - last > MAX_KEEP_TICKS`）。
  - 关卡卸载/维度切换：随 `RiftManager.clearLevel` 一并清理特定维度（可按 level 维度分桶 Key）。

### API 形态（示意）
```java
// RiftManager.java
public final class RiftManager {
  private static final boolean RL_ENABLED = true; // RiftTuning 开关
  private static final int RL_WINDOW_TICKS = 10;  // RiftTuning 窗口

  private final Map<UUID, Long> damageRateLimiter = new ConcurrentHashMap<>();

  public boolean tryPassDamageGate(LivingEntity target, long now) {
    if (!RL_ENABLED) return true;
    UUID id = target.getUUID();
    Long last = damageRateLimiter.get(id);
    if (last != null && (now - last) < RL_WINDOW_TICKS) {
      return false; // 限频：拒绝本次
    }
    damageRateLimiter.put(id, now);
    return true;
  }
}
```

### 接入点
- 裂隙穿刺伤害：`RiftEntity.dealPierceDamage(ServerLevel level)`
  - 在计算出 `finalDamage` 与构造 `damageSource` 后、调用 `target.hurt(...)` 之前，先调用 `RiftManager.getInstance().tryPassDamageGate(target, level.getGameTime())`，未通过则 `continue`。
- 共鸣波伤害：`RiftManager.dealResonanceWaveDamage(...)`
  - 同上，遍历目标时先调用限频门，通过才结算伤害。

### 可配置项（RiftTuning）
- `public static final boolean RATE_LIMIT_ENABLED = true;`
- `public static final int RATE_LIMIT_WINDOW_TICKS = 10;`
- 可加：`public static final int RATE_LIMIT_MAX_KEEP_TICKS = 20 * 60; // 1 分钟`（清理窗口）

### 兼容与并发
- 并发：游戏主线程场景下无需重并发，但采用 `ConcurrentHashMap` 可降低未来更改的心智负担。
- 多维度：当前 `RiftManager` 已按 Level 管理 rift 集，限频表是否需要分 Level？
  - 简化：UUID 全局唯一，跨维度迁移极少在窗口内发生，可不分 Level。
  - 若严格隔离：可在 Key 组合上附加 `level.dimension()` 标识。

## 落地步骤
1. 参数与开关
   - 在 `RiftTuning` 增加 `RATE_LIMIT_ENABLED`、`RATE_LIMIT_WINDOW_TICKS`、（可选）`RATE_LIMIT_MAX_KEEP_TICKS`。
2. 管理器与状态
   - 在 `RiftManager` 增加 `damageRateLimiter` 字段与 `tryPassDamageGate(...)` 方法；在 `clearLevel(...)` 时考虑清理。
3. 注入入口
   - `RiftEntity.dealPierceDamage(...)` 内调用限频门。
   - `RiftManager.dealResonanceWaveDamage(...)` 内调用限频门。
4. 清理策略
   - 在 `tryPassDamageGate` 写入路径上进行惰性清理（每 N 次命中尝试清掉过期条目）。
5. 调试与日志
   - 在 `ChestCavity.LOGGER.debug` 增加命中/拦截统计（受 DEBUG 开关控制，默认关闭）。
6. 文档与配置说明
   - 在 `docs/TESTING_SUMMARY.md` 简述限频机制；在 `RiftTuning` 常量上添加注释说明。

## 风险与回退
- 风险
  - 限频窗口过大：降低总伤有效输出。应保守取值（10 tick）并保留开关。
  - 统计表膨胀：长时运行服务器可能产生一定 Key 保留。采用惰性清理并设置 `MAX_KEEP_TICKS` 限界。
  - 交互边界：当玩家刚好在窗口内切入，可能“错过”一次裂隙伤害，但这是设计期望，用于换取近战手感。
- 回退
  - 通过 `RATE_LIMIT_ENABLED=false` 一键关闭。

## 测试计划（JUnit 5，纯逻辑）
- 抽取/模拟：将 `tryPassDamageGate(now)` 的核心判定提炼为纯函数（或对 `RiftManager` 方法做最小 Mock 环境）。
- 用例
  - 首次命中通过；`now+1` 内再次命中被拒；`now+WINDOW` 再次通过。
  - 多目标互不干扰；同一目标跨两次来源（裂隙/共鸣）同样受限频。
  - 开关关闭时，所有尝试均通过。
  - 惰性清理：超出 `MAX_KEEP_TICKS` 后再次写入时表项被更新/收敛。
- 不在单元测试中触碰 Minecraft 实体；用 UUID 替代。

## 发布与验证
- 构建：`./gradlew compileJava`；
- 定向手测：
  - 在实验世界中叠加多个微裂 + 主裂 + 共鸣波，观察“紫色伤害”频度明显降低；
  - 玩家近战连续攻击同一目标时，误吞伤害显著减少；
  - 切换 `RATE_LIMIT_WINDOW_TICKS` 与 `RATE_LIMIT_ENABLED` 进行对比验证；
  - 长时间运行观察内存与日志（在 DEBUG 开启时）。

## 后续可选优化（非本次范围）
- 同 tick 合并：将同 tick、同来源对同一目标的多次结算合并为一次（与限频互补）。
- 伤害窗口按“目标最近一次被玩家近战命中”动态缩短，进一步保障玩家刚刚出手的击中手感。
- 将限频数据分维度存储，进一步精确管理跨维度情况。

---

责任人：ChestCavityForge 开发团队
最后更新：2025-11-07

