# C 计划 — 冥想与领域：剑心蛊 非玩家支持

目标：让 Mob 拥有“冥想态”与“剑心域”，在脱战恢复与受伤打断间平衡；非玩家路径跳过 UI/移动属性减速，仅保留领域与倍率。

—

## 技术基线
- 自动化状态机：
  - 脱战 ≥2s → 进入冥想；
  - 受伤/获得有效目标 → 退出冥想；
- 领域维持：周期刷新/重建；
- 资源与剑势：每秒 +精力（若可用）、+剑势（冻结期不增长）；
- 玩家专属提示/Toast 全部跳过。

—

## 任务清单
1) onSlowTick 增加 Mob 分支
- 读取/写入 `OrganState`：`Meditating`、`ReadyAt`、`SwordMomentum`、`FreezeTicks`、`DomainLevel`；
- 脱战/进战条件来自 `AIIntrospection` 和 `mob.getTarget()`；

2) 领域支持
- 将 `spawnOrRefreshDomain(ServerPlayer, level)` 改为通用重载 `spawnOrRefreshDomain(LivingEntity, level)`，为非玩家省略减速与玩家提示；

3) 受伤事件
- `onIncomingDamage`：若非玩家且在冥想→退出并设定冻结期（同玩家逻辑），仅保留倍率计算；

—

## 改动位点
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianXinGuOrganBehavior.java:131`
  - 扩展 onSlowTick / onIncomingDamage 的实体类型判定；
  - 提取领域刷新方法通用重载；

—

## 验收标准
- Mob 能在脱战进入“冥想”，受伤打断；
- 领域存在与倍率生效；
- 冷却/冻结等状态在 `OrganState` 中持续；
- 无 UI/提示，日志安静。

