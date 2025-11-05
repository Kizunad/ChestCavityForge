# A 计划 — 快速落地：裂剑蛊 + 剑域蛊 非玩家自动化

目标：在不改动大型架构的前提下，让非玩家（Mob）能够在战斗中自动施放裂剑蛊主动、自动进入并维持剑域蛊的“持续期”，并在脱战后延迟关闭或停止施放；保持静默，不进行资源提示。

—

## 范围
- 裂剑蛊（LieJianGuOrganBehavior）
- 剑域蛊（JianYuGuOrganBehavior）

## 技术基线
- 战斗判定：`AIIntrospection.getRunningAttackGoalNames(mob)` + `mob.getTarget()!=null`
- 激活入口：`ActiveSkillOps.activateFor(mob, ABILITY_ID)`（静默，无 UI/提示）
- 状态持久化：`OrganState`（字段：`LastAttackGoals`、`DisengagedAt` 等），按青莲模式
- 冷却与就绪：`MultiCooldown`（非玩家不安排 Toast）

—

## 裂剑蛊（主动自动化）
实现思路：
- 进入战斗：若冷却就绪→自动施放一次“裂刃空隙”；
- 持续战斗：每当冷却就绪再次施放（节流由 `MultiCooldown` 控制）；
- 脱战：记录 `DisengagedAt`，脱战超过 5s 不再尝试施放；
- 无资源/无 UI：不做资源提示，`tryConsumeTieredZhenyuan` 失败时静默返回。

改动位点：
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/LieJianGuOrganBehavior.java:235`
  - 在 `onSlowTick` 中加入 `Mob` 分支：
    1) 读取攻击 Goal 与目标；
    2) 比对 `LastAttackGoals` 判定战斗状态变更；
    3) 进入战斗时调用 `ActiveSkillOps.activateFor(mob, ABILITY_ID)`；
    4) 持续战斗时，若 `MultiCooldown` 就绪则再施放；
    5) 脱战计时 `DisengagedAt`，超过 100 tick 停止尝试；
    6) 写回 `LastAttackGoals`。

验收标准：
- 装备后由 Mob 主动接战时能自动施放裂隙；
- 持续交战期内按冷却节奏重复施放；
- 脱战稳定停止；
- 不产生玩家提示或异常日志。

—

## 剑域蛊（直接进入持续期）
实现思路：
- 非玩家跳过“调域窗口”，进入战斗时直接调用“进入持续期”的内部方法（为 Mob 增加一个安全变体）；
- 持续期内每秒扣费使用 `ResourceOps.tryAdjustZhenyuan(owner, -amt, true)`（若无资源句柄静默）；
- 脱战 5s 结束持续期（将 `ActiveUntil` 置为 `now`）。

改动位点：
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianYuGuOrganBehavior.java:103`
  - 在 `onSlowTick` 中加入 `Mob` 分支：
    1) 战斗进入→若不在持续期，直接 `enterActivePhaseFor(owner, state, now)`（新增内部方法，复用 FX）；
    2) 持续战斗→维持领域与扣费；
    3) 脱战计时→超过 100 tick 将 `ActiveUntil` 清零；
    4) FX 维持：仅保留关键环形光效，避免过多粒子。
- 如现有的 `enterActivePhase(ServerPlayer, ...)` 仅玩家可用，新增 `enterActivePhaseFor(LivingEntity, OrganState, long)` 内部重载，避免 UI 依赖。

验收标准：
- Mob 进战立即生成/维持领域，正面锥额外减伤生效；
- 被动与主动扣费逻辑在 Mob 分支稳定运行；
- 脱战 5s 自动停止，FX 适度；
- 无玩家侧提示与异常日志。

—

## 共用实现细节
- 状态键建议：`LastAttackGoals`（ListTag/STRING）、`DisengagedAt`（long）
- 延迟阈值：`DISENGAGE_DELAY_TICKS = 100`
- 只在玩家分支安排 `ActiveSkillRegistry.scheduleReadyToast`；Mob 分支不触发。

## 测试与验证
- 命令生成持有器官的 Mob，实测接战/脱战行为；
- `./gradlew compileJava` 与 `./gradlew test`（仅跑纯逻辑）；
- 观察日志：无刷屏、无资源缺失 NPE；
- 区块卸载/重载后状态连续（OrganState 恢复正常）。

