# FxEngine 时间线系统 · 测试用例

## 测试策略

- 单元测试（JUnit 5）：仅测纯逻辑（预算/合并/状态机/门控判定）；避免依赖 MC 世界对象。
- 集成手册测试：Shockfield 回调内提交时间线，观察运行与预算行为。

## 主要用例

- 预算与合并
  - 预算关闭：无限提交不触发丢弃；
  - 预算开启：提交 > perLevel 上限 → 触发合并或丢弃（按策略）；
  - 同 mergeKey 重复提交 → TTL 延长；
  - 不同 owner 各自计数；
- 状态机
  - onStart 仅执行一次；
  - tickInterval=1/2/5 正常触发；TTL 到期 onStop；
  - onTick 异常不影响其他 Track；
- 门控
  - 玩家半径不足 → onTick 跳过（或暂停配置）；
  - 区块未加载 → 跳过；
- Owner 死亡/移除 → onStop(OWNER_REMOVED)；

- 注册中心（Registry）
  - Register/Play：注册 id=f"chestcavity:fx/shockfield/ring" 的 factory；在 onWaveCreate 与 onHit 两处均可 Play 并正常实例化。
  - 合并键：通过 mergeKey("shockfield:ring@"+waveId) 实现同一波场 FX 合并与续期。
  - 上下文：传入 waveId、ownerId、level 与自定义参数（颜色/半径缩放），factory 能正确构建 spec。

## 验收清单（手册）

- 在默认配置（预算关闭）下提交典型 FX，长时间运行稳定无刷屏。
- 开启预算后提交超量 FX，活跃数不超过上限；丢弃率/合并数可观察。
