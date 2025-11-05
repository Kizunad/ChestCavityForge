# B 计划 — 影子与投射抽象：剑影蛊 非玩家支持

目标：让 Mob 能触发剑影蛊的“分身主动”与“被动影袭/残像”，在没有玩家皮肤/展示物品的情况下提供安全降级外观与伤害逻辑。

—

## 技术基线
- 激活入口：`ActiveSkillOps.activateFor(mob, ABILITY_ID)`
- 影子/投射安全降级：为 `ShadowService` 和 `SingleSwordProjectile` 增加非玩家兼容分支或新增 `spawnForMob` 重载：
  - 皮肤 Tint：无玩家皮肤时使用统一“墨影”色板；
  - 展示物品：缺省为铁剑/石剑；
  - 生成/寿命：与玩家版本一致或略缩短；
- 状态与冷却：`OrganState` + `MultiCooldown`（Mob 不提示）。

—

## 任务清单
1) ShadowService 兼容
- 新增 `captureTintFor(LivingEntity)` 与 `resolveDisplayStackFor(LivingEntity)`：
  - 玩家：复用现有实现；
  - 非玩家：返回墨影 Tint 与默认剑物品；

2) SingleSwordProjectile 兼容
- 新增 `spawnForMob(Level, LivingEntity, Vec3 anchor, Vec3 tip, tint, display)`：
  - 投射初速与角度沿用玩家版本；
  - 伤害系数按 `JianYingTuning.BASE_DAMAGE` 基础值；

3) 主动“分身”自动化
- 在行为类 `onSlowTick` 中加入 Mob 分支：
  - 战斗进入→冷却就绪时自动激活“分身”；
  - 持续战斗→周期性尝试；脱战→5s 停止；

4) 被动影袭/残像
- onHit 的玩家限定改造为：
  - 若 `attacker instanceof Mob` 亦允许触发“被动影袭”与“残像”；
  - 使用 `spawnForMob`；伤害缩放按通用效率（账本增益若无则为 1.0）。

—

## 改动位点
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianYingGuOrganBehavior.java`
  - 扩展 onSlowTick：Mob 自动化状态机；
  - 放开 onHit 非玩家触发路径；
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/common/`（若需要新增工具）
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/entity/SingleSwordProjectile.java`（新增重载）
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/common/ShadowService.java`（或所在位置）

—

## 验收标准
- Mob 在近战命中时能偶发影袭，FX 克制；
- 进战时能周期性召唤“分身”自助攻击；
- 无玩家皮肤仍稳定显示“墨影剑”；
- 无提示/无异常日志；性能平稳。

