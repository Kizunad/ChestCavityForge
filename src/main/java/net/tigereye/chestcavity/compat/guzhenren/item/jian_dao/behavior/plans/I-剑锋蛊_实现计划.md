# 五转/四转·剑锋蛊（JianFengGu）实现计划（Draft v0.1）

> 模块：`chestcavity` 扩展 · 剑道系主动+被动器官  
> 关联物品ID：
> - 四转：`item.guzhenren.jianfenggu`（资源：`guzhenren:jianfenggu`）
> - 五转：`item.guzhenren.jian_feng_gu_5`（资源：`guzhenren:jian_feng_gu_5`）
>
> 主动：锋芒化形（Fengmang Huaxing）  
> 被动：意随形动（Yi Sui Xing Dong）

---

## 0. 概念与目标（KISS / YAGNI）
- 主动·锋芒化形：将真元凝为剑刃附于右臂，短时强化攻击；发动后在身后生成“自有飞剑”实体：
  - 四转：1 把；五转：2 把；
  - 进入“随侍/环绕”队形，默认不主动出击。
- 触发·高额一击协同：宿主对单体造成高额伤害（四转≥100，五转≥500）时，生成的飞剑立即对同一目标发动一次“协同突击”，随后回归原位。
  - 协同伤害继承宿主“剑道道痕”与部分攻击修正（按既有统一计算工具）；
  - 一次触发仅消耗其中一把飞剑；多把按轮询/就近选择。
- 被动·意随形动：每当上述“协同突击”累计完成 3 次，触发一次“剑意共振”：
  - 为宿主周围“隶属于自身”的全部飞剑提供短时速度提升（等效剑心域增幅的一个子集）；
  - 持续时间与加成幅度随剑道道痕、剑道流派经验与四/五转阶段递增。

约束与边界：
- 不重造飞剑系统，完全复用 `compat/guzhenren/flyingsword` 子系统（DRY）。
- 不引入跨器官全局状态，仅使用本器官 `OrganState` 与已存在的飞剑控制器（KISS）。
- 非玩家除了无消耗之外，逻辑不变

---

## 1. 目录与新建类（遵循 AGENTS.md）
- behavior/organ：
  - `JianFengGuOrganBehavior.java`（enum 单例；OnHit/SlowTick + 主动技注册）
- behavior/active：
  - `JianFengHuaxingActive.java`（轻量入口，参数校验与转调）
- tuning：
  - `JianFengGuTuning.java`（常量与阈值）
- fx：
  - `JianFengGuFx.java`（粒子/音效与 UI Toast）

注册：
- `OrganActivationListeners.register(ABILITY_ID, JianFengGuOrganBehavior::activateAbility)`。
- `JiandaoOrganRegistry` 新增本器官的 `OrganIntegrationSpec`（OnHit + SlowTick）。

---

## 2. 常量与标识（`tuning/JianFengGuTuning.java`）
- MOD/ID：
  - `MOD_ID = "guzhenren"`
  - ORGAN_ID_FOUR = `guzhenren:jianfenggu`；ORGAN_ID_FIVE = `guzhenren:jian_feng_gu_5`
  - ABILITY_ID = `guzhenren:jian_feng/huaxing`
- 主动：
  - `ACTIVE_BASE_DURATION_TICKS = 10s = 200`
  - `ACTIVE_BONUS_DAMAGE_MULT = 0.10`（对近战单次伤害的附加系数）
  - 成本：`COST_ZHENYUAN = BURST TIER`，`COST_JINGLI = 20.0`（按转数对应增大，五转使用5转1阶段BURST，四转使用4转1阶段BURST）
  - 冷却：`COOLDOWN_TICKS = 20s = 400`
  - 生成飞剑：`SPAWN_COUNT_FOUR = 1`，`SPAWN_COUNT_FIVE = 2`；`SPAWN_OFFSET = (0, 1.6, -1.2)`（相对宿主身后）
- 高额一击阈值：
  - `HIGH_HIT_THRESHOLD_FOUR = 100.0f`，`HIGH_HIT_THRESHOLD_FIVE = 500.0f`
  - 两次触发间隔：`COOP_MIN_INTERVAL_TICKS = 10`（去抖）
- 共振：
  - `COOP_COUNT_FOR_RESONANCE = 3`
  - `RESONANCE_BASE_DURATION_TICKS = 60`；`RESONANCE_BASE_SPEED_BONUS = +15%`
  - 规模：`+ (daohen / 200) * 5%` 速度；持续 `+ (liupaiExp / 300) * 20` 刻；五转额外 `+25%`

---

## 3. 行为设计（`behavior/organ/JianFengGuOrganBehavior.java`）

### 3.1 状态键（OrganState）
- `STATE_ROOT = "JianFengGu"`
- `KEY_ACTIVE_UNTIL`（long）：主动态截止 tick（0=未激活）
- `KEY_SPAWNED_SWORD_IDS`（ListTag/UUID 或 EntityId 列表）：本次主动生成的飞剑标识（仅用于“协同突击/回位”）
- `KEY_LAST_COOP_TICK`（long）：上次协同触发 tick（去抖）
- `KEY_COOP_COUNT`（int）：协同已累计次数（达 3 触发共振后清零）

### 3.2 主动技（activateAbility）-  src/main/java/net/tigereye/chestcavity/skill/ActiveSkillRegistry.java 内注册(两个器官都能触发这个技能即可)
- 前置：仅服务端 `ServerPlayer`；冷却就绪 + 资源可支付（`ResourceOps.payCost`）
- 设置：`KEY_ACTIVE_UNTIL = now + ACTIVE_BASE_DURATION_TICKS`
- 生成飞剑：调用 `FlyingSwordSpawner.spawnBehind(player, count, SPAWN_OFFSET, flags)`
  - 为每把飞剑附着“本器官会话标签”与“回位锚点”（宿主身后随侍）；
  - 记录其 `UUID/EntityId` 至 `KEY_SPAWNED_SWORD_IDS`；
- FX：`JianFengGuFx.playActivate(player)` + 冷却 Toast 安排

### 3.3 高额一击协同（OnHit 钩子）
- 若攻击者=宿主本体、目标为 `LivingEntity`、伤害≥阈值、`now - KEY_LAST_COOP_TICK ≥ COOP_MIN_INTERVAL_TICKS`：
  - 选剑：从 `KEY_SPAWNED_SWORD_IDS` 中查找仍存活且可行动的飞剑；轮询/就近选择一把；
  - 发令：通过 `SwordCommandCenter` 以临时“突击”意图执行一次短程直突 + 即时伤害（参数按道痕/玩家攻击属性折算倍率）；
  - 回位：执行完毕后切回“随侍/环绕”模式，回到锚点；
  - 计数：`KEY_COOP_COUNT += 1`，更新时间戳 `KEY_LAST_COOP_TICK = now`；
  - 若计数达到 `COOP_COUNT_FOR_RESONANCE`：触发“剑意共振”。

### 3.4 剑意共振（SlowTick/即时触发）
- 触发时机：`KEY_COOP_COUNT >= 3` 时立刻触发一次，共振后将 `KEY_COOP_COUNT = 0`；
- 作用域：`FlyingSwordController.getPlayerSwords(level, player)`，过滤 `isOwnedBy(player)`；
- 增益：为每把飞剑注入“速度提升”效果（借助 `MovementSystem` 的 speedScale/desiredMaxFactor 临时提升），持续 `duration = f(daohen, liupaiExp, 转数)`；
- FX：`JianFengGuFx.playResonance(player)`（环形微粒 + 清脆短音）；

### 3.5 主动态维持（SlowTick）
- 若 `now >= KEY_ACTIVE_UNTIL`：
  - 结束主动态：清空 `KEY_SPAWNED_SWORD_IDS` 的“会话标签”，不强制回收实体（交给飞剑控制器自然回位/散开）；
  - 不额外清理他人飞剑。

---

## 4. 计算与协同伤害（复用，DRY）
- 协同伤害倍率：`damage *= (1.0 + JianFengGuTuning.ACTIVE_BONUS_DAMAGE_MULT) * scaleByDaohen(player)`；
- `scaleByDaohen(player) = 1.0 + min(daohen / 300, 0.5)`（上限 +50%）；
- 伤害管线：沿用 `compat/guzhenren/util/DamagePipeline`；
- 突进几何：借鉴 `JianSuo` 突进实现，构造一次性 `SteeringCommand`，步数≤6，保证“即发即回”。

---

## 5. 粒子与音效（`fx/JianFengGuFx.java` + 资源占位）
- 激活：
  - 粒子：`END_ROD`（沿右臂→手掌路径 6–8 点）、`ENCHANT`（短尾迹）；
  - 音效：`guzhenren:jianfeng/activate`（新增，短促金铁声，音高 1.1）
- 协同突击：
  - 粒子：飞剑起手 `SWEEP_ATTACK`，命中 `CRIT`；
  - 音效：起手 `PLAYER_ATTACK_SWEEP`，命中 `ANVIL_LAND`（或 `guzhenren:jianfeng/assist_hit` 自定义）
- 共振：
  - 粒子：以宿主为圆心 3.5 格环状“花瓣状光屑”（密度低，1 圈）；
  - 音效：`guzhenren:jianfeng/resonate`（玻璃轻鸣叠铃，音高 1.2）

资源建议：在 `src/main/resources/assets/guzhenren/sounds.json` 增加 `jianfeng.*` 三条目；粒子全部复用原生类型，无需新注册。

---

## 6. 器官属性（基础条目，随阶段差异化）
- 四转（`guzhenren:jianfenggu`）
  - 剑道道痕：+80
  - 移动速度：+0.2（右臂轻刃化带来轻微灵活）
  - 防御:    -20  (剑作为器官导致的防御不足)
  - 力量：+20
- 五转（`guzhenren:jian_feng_gu_5`）
  - 剑道道痕：+140
  - 移动速度：+0.4
  - 防御:    -40  (剑作为器官导致的防御不足)
  - 力量：+40

说明：最终数值以整体平衡为准，保持与同档位“剑梭/裂剑/剑影”一致的强度阶梯；本计划提供初值用于实现与联调。

---

## 7. 玩家 UI 文档（目标路径与草案）
- 目标文件：`src/main/resources/assets/guzhenren/docs/human/jian_dao/jian_feng_gu.json`
- 草案内容：

```json
{
  "id": "guzhenren:jianfenggu",
  "title": "剑锋蛊",
  "summary": "锋芒化形，召唤飞剑随侍；强力一击引发协同突击，三次后触发剑意共振。",
  "details": [
    "主动·锋芒化形：消耗 60 真元 + 20 精力；持续 10 秒；冷却 20 秒",
    "生成飞剑：四转 1 把，五转 2 把，随侍环绕，不主动出击",
    "协同突击：当你对同一目标造成高额一击（四转≥100 / 五转≥500）时，飞剑立即突进斩击并回位",
    "增益：主动期内你的近战伤害 +10%，协同突击继承你的剑道道痕与部分攻击修正",
    "被动·意随形动：每完成 3 次协同突击触发“剑意共振”，为你名下全部飞剑短时加速（随道痕/流派经验/阶段递增）",
    "属性（四/五转）：剑道道痕 +80/+140，移动速度 +0.02/+0.04，力量 +2/+4"
  ],
  "tags": [
    "剑锋蛊",
    "guzhenren:jianfenggu",
    "guzhenren:jian_feng_gu_5",
    "jian_dao"
  ],
  "icon": "guzhenren:jianfenggu"
}
```

---

## 8. 代码位点与改动清单（最小侵入）
- 新增：
  - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianFengGuOrganBehavior.java`
  - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/active/JianFengHuaxingActive.java`
  - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/tuning/JianFengGuTuning.java`
  - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/fx/JianFengGuFx.java`
- 修改：
  - `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/JiandaoOrganRegistry.java`（注册本器官 OnHit/SlowTick）
  - 可选：`compat/guzhenren/flyingsword/ai/command/SwordCommandCenter`（若需便捷“一次性突击”API，添加 `assistStrikeOnce(owner, sword, target, params)` 轻量封装）
  - 可选：`assets/guzhenren/sounds.json`（新增 `jianfeng.*` 声音条目，稍后实现）

---

## 9. 验收标准（可操作）
- 主动发动后，按阶段生成 1/2 把飞剑，随侍环绕；10 秒后自动结束；冷却与资源正确结算。
- 当出现高额一击时，恰有一把飞剑立刻突进命中并回位，不连发、无刷屏；多把按轮询合理使用。
- 完成 3 次协同后，出现“共振”FX，全部自有飞剑出现明显的短时加速。
- 仅影响“自有飞剑”，不干扰他人飞剑；非玩家不触发。
- 日志安静，无 NPE；离开/进入区块与死亡复活后状态稳定。

---

## 10. 原则落实（SOLID / DRY / KISS / YAGNI）
- 单一职责（S）：`OrganBehavior` 只协调状态与接口调用；运动/AI 仍由飞剑子系统负责；数值集中 `tuning`。
- 开闭（O）：通过新增封装 API（可选）扩展突击行为，避免修改既有运动系统核心流程。
- 里氏（L）：沿用既有 `Organ*Listener` 接口，不改变契约。
- 接口分离（I）：UI/音效置于 `fx`，不与行为/计算耦合。
- 依赖倒置（D）：依赖抽象的控制器与工具类（`SwordCommandCenter`、`ResourceOps`、`MultiCooldown`），不直接操纵底层数据结构。

---

## 11. 后续（小步扩展，非本次范围）
- 非玩家自动化路径（进入战斗→主动生成→跟随→同样触发协同）；
- 共振期间的受击强化（如格挡概率提升）与更丰富的 FX；
- 与“剑幕/剑引”联动的策略（共振期间优先守卫或集火）。

