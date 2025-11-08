# 五转·碎刃蛊（SuiRenGu）计划草案

> 版本：Draft v0.1（NeoForge 1.21.1 / ChestCavityForge 扩展）  
> 模块：`chestcavity` 扩展 · 剑道系核心主动器官（无被动）

## 0. 概念与目标

* **核心理念**：牺牲“当前在场且可回收”的自有飞剑，瞬时换取「剑道道痕」临时增幅，遵循现有增幅逻辑与 UI。重置期限 / 消耗 / 冷却保持一致，杜绝永久堆叠。
* **风险/收益**：高爆发 + 冷却 + 资源门槛，需“战斗/撤退”节奏；不写入永久道痕，结算完毕即还原。

## 1. 触发链路

1. `ActiveSkillRegistry` 注册 `guzhenren:sui_ren_gu`（标签 `爆发`、`增益`），`sourceHint` 指向行为文件。
2. `JiandaoClientAbilities` 以字面 `ResourceLocation.parse("guzhenren:sui_ren_gu")` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`。
3. `OrganActivationListeners` 通过 `SuiRenGuOrganBehavior.INSTANCE` 注册能力。
4. `ActivationHookRegistry` 在 `SkillEffectBus` 中为 `^guzhenren:sui_ren_gu$` 注册 `ResourceFieldSnapshotEffect("jiandao:", List.of("daohen_jiandao", "liupai_jiandao"))`。
5. 网络包 / 服务器 `OrganActivationListeners.activate` 触发行为，`SuiRenGuActive.activate` 读取 `ChestCavityInstance` + `MultiCooldown` + `ResourceOps` + 飞剑列表。

## 2. 核心常量（`tuning/SuiRenGuBalance.java`）

| 名称 | 描述 | 建议值 |
| --- | --- | --- |
| `E_CAP` | 经验上限 | 200 |
| `D_REF/A_REF/S_REF` | 耐久/攻击/ 速度 归一化参考值 | 500f / 10f / 1.5f |
| `ALPHA_EXP` | 经验权重（E=50 -> Δ=100） | 2.0f |
| `BETA_ATTR` | 属性项权重（与 `W_*` 联动） | 20f |
| `W_D/W_A/W_S` | 耐久/攻击/速度 占比 | 0.50/0.35/0.15 |
| `PER_SWORD_CAP` | 单把收益上限 | 400 |
| `CAST_TOTAL_CAP` | 单次施放总上限 | 2000 |
| `BASE_DURATION_TICKS` | 基础持续 | 200（10s） |
| `DURATION_PER_SWORD` | 每把 +2s | 40 |
| `DURATION_CAP` | 上限 60s | 1200 |
| `BASE_COST_...` | 真元/精力/念头 | 120/60/40 |
| `COOLDOWN_TICKS` | 冷却（45s） | 900 |

## 3. 计算器（`calculator/SuiRenGuCalc.java`）

* `int deltaForSword(int exp, double maxDur, double maxAtk, double maxSpeed)`
  * `e = clamp(exp, 0, E_CAP)`。
  * `^D = min(maxDur / D_REF, 1)`（其余同理）。
  * `W = w_D*^D + w_A*^A + w_S*^S`。
  * `Δ = ALPHA_EXP * e + BETA_ATTR * W`。
* `int totalForCast(List<SwordStats>)`：`Σ min(delta_i, PER_SWORD_CAP)` → `min(..., CAST_TOTAL_CAP)`。
* `int durationForCast(int count)`：`clamp(BASE + count * DURATION_PER_SWORD, 0, DURATION_CAP)`。

## 4. 资源与冷却

1. `ResourceCost`（`BASE_COST_*`）交给 `ResourceOps.payCost(player, cost, hint)`，若失败直接 `return`。
2. `OrganState`（`state/SuiRenGuState.java`）包含：
   * `ROOT = "SuiRenGu"`；
   * `KEY_READY_TICK`, `KEY_BUFF_END_AT_TICK`, `KEY_BUFF_APPLIED_DELTA`。
3. `MultiCooldown` 通过 `builder(state).withSync(cc, organ).build()` 获得 `readyEntry`。
4. 激活成功后：
   * `readyEntry.setReadyAt(now + COOLDOWN_TICKS)`；
   * `ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now)`；
   * `SuiRenGuFx.playCooldownToast(...)`（辅助）。

## 5. 行为实现（`behavior/organ/SuiRenGuOrganBehavior.java` 与 `behavior/active/SuiRenGuActive.java`）

1. `activateAbility`：
   * 仅服务端 `ServerPlayer`；
   * 校验 `MultiCooldown`、资源；
   * `List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(serverLevel, player)`；
   * 过滤 `isOwnedBy(player)`、`isRecallable()`、`getDurability() > 0`；
   * 若列表空：静默失败（不消耗，冷却不变）；
   * 逐把 `SwordStats stats = new SwordStats(s.getExperience(), s.getSwordAttributes())`，`int delta = SuiRenGuCalc.deltaForSword(...)`；
   * 播放 `SuiRenGuFx.playShardBurst(player, s)` + `SoundOps`；
   * `FlyingSwordEventRegistry.fireDespawnOrRecall(new DespawnContext(..., Reason.OTHER, null))` 之后 `s.discard()`；
   * 聚合 `total += Math.min(delta, PER_SWORD_CAP)`；
   * `total = Math.min(total, CAST_TOTAL_CAP)`；
   * `duration = SuiRenGuCalc.durationForCast(count)`；
   * 调用 `applyTempScarBuff(player, cc, organ, total, duration)`。
2. `applyTempScarBuff`：
   * 通过 `ResourceOps.openHandle(player)` 增加 `daohen_jiandao`；
   * 记录 `KEY_BUFF_APPLIED_DELTA += total`、`KEY_BUFF_END_AT_TICK = now + duration`；
   * `buffEndEntry.onReady(serverLevel, now, () -> clearAppliedBuff(...))`：回滚 `daohen_jiandao`、重置 `KEY_BUFF_APPLIED_DELTA`。

## 6. 状态/回收

* `clearAppliedBuff(...)`：
  * 读取 `KEY_BUFF_APPLIED_DELTA`，`handle.adjustDouble("daohen_jiandao", -value, true)`；
  * `OrganStateOps.setIntSync(cc, organ, KEY_BUFF_APPLIED_DELTA, 0, v -> 0, 0)`；
  * 可选同步 to client。
* 结束时刻 `buffEndEntry.onReady` 保证即使多次叠加也在最长 60s 内完成回滚。

## 7. FX & 提示 (`fx/SuiRenGuFx.java`)

* `playShardBurst`：在每把飞剑位置生成“刃落如霰”粒子，参考飞剑 `FlyingSwordFX.spawnRecallEffect`。
* `playShardSound`：复用 `FlyingSwordSoundTuning` + `CCSoundEvents` （新增 `CUSTOM_FLYINGSWORD_SHATTER`）。
* `scheduleCooldownToast`：`ActiveSkillRegistry.scheduleReadyToast` + `CountdownOps`。

## 8. 测试建议（纯逻辑）

1. `SuiRenGuCalcTest`（JUnit 5）
   * `deltaForSword(E=50,W=0) == 100`；
   * `totalForCast` capped by `CAST_TOTAL_CAP`；
   * `durationForCast` 上不超过 `DURATION_CAP`。
2. `SuiRenGuBalanceTest`
   * 属性归一化不溢出；
   * `PER_SWORD_CAP`/`ALPHA`/`BETA` 组合稳定。
3. `ResourceOps` 可 Mockito `ResourceHandle`/`GuzhenrenResourceBridge` 验证 `applyTempScarBuff` 回滚逻辑（非实体）。

## 9. 验收要点

- 无目标时不消耗资源、不冷却；
- 每把飞剑 `ΔH` ≤ `PER_SWORD_CAP`，总增幅 ≤ `CAST_TOTAL_CAP`；
- Buff 持续 `BASE_DURATION + 2s/剑`，最长 60s，到期清除；
- `daohen_jiandao` 增幅仅临时写入 `ResourceHandle`，回滚不落账；
- `ActiveSkillRegistry`/`ActivationHookRegistry` 快照链路、客户端热键、冷却提示齐备；
- `OrganActivationListeners` 注册 + `enum` 单例（避免构造器副作用）。

## 10. 后续步骤

1. 先建 `tuning/` + `calculator/` + `state` skeleton；
2. 完成 `behavior` + `fx` + `ActiveSkillRegistry`；
3. 跑 `./gradlew compileJava` + `./gradlew test`；
4. 手动验证飞剑数量/资源/buff 时间边界。

## 11. 数据与文档同步要求

- `src/main/resources/data/chestcavity/organs/guzhenren/human/jian_dao/*.json`  
  - 新增或更新剑道系器官配置时，确保反映碎刃蛊调参：`"chestcavity:speed": -5`、`"chestcavity:strength": -20`、`"chestcavity:health": -20`、`"guzhenren:daohen_jiandao": 100`（或同等字段）
- `src/main/resources/assets/guzhenren/docs/human/jian_dao/*.json`  
  - 文档要同步说明：数值调整原因、临时道痕增幅机制、碎刃蛊使用注意事项（资源需求/飞剑牺牲/收益上限）。  
  - 若尚未存在章节，新增“碎刃蛊”节，记录属性副作用（-5 移速、-20 力量、-20 最大生命、 + 100 剑道道痕）。
