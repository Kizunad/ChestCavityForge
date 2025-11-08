# 3–5 转 · 剑梭蛊（JianSuoGu）实现计划

> 版本：Plan v0.1（NeoForge 1.21.1 / ChestCavityForge 扩展）
> 模块：`chestcavity` · 剑道系主动+被动器官（含飞剑钩子）

---

## 0. 目标与范围

- 主动：向前突进（路径伤害，按“剑道道痕”放大距离与伤害，命中友伤过滤与命中去重）。
- 被动：受击时有几率“反向后退 + 本次减伤 + 短暂无敌帧”，具全局冷却；NPC 同样生效。
- 飞剑：每隔 S 秒沿当前朝向执行一次短突进（不消耗资源，独立冷却），仅对非友方生效。
- 平衡范围：3–5 转。资源、冷却随“转数/阶段”和“道痕/流派经验”缩放（通过 `ResourceOps` + 快照）。

---

## 1. 资源与注册（调用点）

- 能力 ID：`guzhenren:jian_suo_gu_dash`
- 组织结构（新增类/文件）：
  - `compat/guzhenren/item/jian_dao/behavior/organ/JianSuoGuOrganBehavior.java`（enum 单例）
  - `compat/guzhenren/item/jian_dao/behavior/active/JianSuoGuActive.java`（主动技实现）
  - `compat/guzhenren/item/jian_dao/behavior/passive/JianSuoGuEvadePassive.java`（被动受击处理）
  - `compat/guzhenren/item/jian_dao/runtime/jian_suo/JianSuoRuntime.java`（纯逻辑：突进/路径采样/去重）
  - `compat/guzhenren/item/jian_dao/tuning/JianSuoGuTuning.java`（常量/可热更钩子）
  - `compat/guzhenren/flyingsword/events/hooks/JianSuoGuSwordAutoDashHook.java`（飞剑自动突进 Hook）
  - （可选）`compat/guzhenren/item/jian_dao/calculator/JianSuoCalc.java`（公式/映射，便于单测）

- 注册要求：
  - `OrganActivationListeners.register(ABILITY_ID, JianSuoGuOrganBehavior::activateAbility)`（在行为 enum 的 `static {}` 中执行）。
  - `ActivationHookRegistry`：在 `register()` 内新增一条：
    - `SkillEffectBus.register("^guzhenren:jian_suo_gu_dash$", new ResourceFieldSnapshotEffect("jiandao:", List.of("daohen_jiandao", "liupai_jiandao")))`（参数快照）。
  - 客户端热键：在 `JiandaoClientAbilities.onClientSetup` 里以字面 `ResourceLocation.parse("guzhenren:jian_suo_gu_dash")` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`（不要引用行为类常量）。
  - 飞剑钩子：`FlyingSwordEventInit.init()` 后通过 `FlyingSwordEventRegistry.register(new JianSuoGuSwordAutoDashHook())` 注册。

---

## 2. 常量与公式（`JianSuoGuTuning`）

```java
// 主动突进
public static final double BASE_D = 5.0;        // 基础距离（格）
public static final double BASE_ATK = 1.0;      // 基础伤害系数
public static final double RAY_WIDTH = 0.8;     // 胶囊体半宽
public static final int MAX_DASH_T = 12;        // 逐帧推进上限
public static final int HIT_ONCE_T = 10;        // 命中去重窗口（tick）

// NPC/飞剑适配
public static final double NPC_GOAL_LOCK_MAXDIST = 24.0;
public static final double SWORD_DASH_INTERVAL_S = 3.0; // 飞剑自动突进间隔

// 被动躲避
public static final double EVADE_COOLDOWN_S = 6.0;
public static final double EVADE_BACKSTEP = 2.4;
public static final int EVADE_I_FRAMES_T = 6;
public static final double EVADE_CHANCE_BASE = 0.10;
public static final double EVADE_CHANCE_PER_100 = 0.06; // 上限 0.60
public static final double EVADE_REDUCE_MIN = 0.10;
public static final double EVADE_REDUCE_MAX = 0.90;
public static final double EVADE_REDUCE_PER_100 = 0.08;

// 资源消耗（基础值，交由 ResourceOps 按转数/阶段缩放）
public static final double BASE_COST_ZHENYUAN = 6.0;
public static final double BASE_COST_JINGLI = 2.0;
public static final double BASE_COST_NIANTOU = 1.0; // 可选，仅玩家

// 冷却（建议下限）
public static final int ACTIVE_COOLDOWN_MIN_T = 60;  // ≥3s（60–120 之间按道痕微调）
```

公式映射（在 `JianSuoCalc` 或 `JianSuoRuntime` 静态方法内实现，便于测试）：

- 距离：`dashDist = clamp(BASE_D * (1 + 0.25 * DH100), 0, 17.0)`；`DH100 = floor(daohen/100)`
- 伤害：`pathDamage = BASE_ATK * (1 + 0.35 * DH100) * (1 + vScale)`，`vScale ∈ [0, 0.25]`
- 躲避几率：`chance = clamp(0.10 + 0.06 * DH100, 0, 0.60)`
- 减伤比例：`reduce = clamp(0.10 + 0.08 * DH100, 0.10, 0.90)`

---

## 3. 主动技执行流（`JianSuoGuActive`）

入口：`static ActiveSkillRegistry.TriggerResult activate(ServerPlayer player, ChestCavityInstance cc)`

1) 方向决议：
   - 玩家：`Vec3 dir = player.getLookAngle();` → 水平归一化
   - 非玩家：
     - 若存在目标且 `distanceTo ≤ NPC_GOAL_LOCK_MAXDIST`：面向目标中心 `target.position()`
     - 否则：取当前朝向 `Vec3.directionFromRotation(0, yaw)`

2) 资源检定：
   - `var pay = ResourceOps.consumeStrict(player, BASE_COST_ZHENYUAN, BASE_COST_JINGLI);`
   - 玩家可选念头：`ResourceOps.consumeNiantouOptional(player, BASE_COST_NIANTOU);`
   - 失败 → 返回 `TriggerResult.FAILED`（静默，按 UI 规范可轻提示）

3) 计算参数：
   - 读取快照字段：`jiandao:daohen_jiandao`、`jiandao:liupai_jiandao`（由 `ActivationHookRegistry` 预先写入）
   - `double dashDist = JianSuoCalc.dashDistance(daoHen)`
   - `double dmg = JianSuoCalc.pathDamage(daoHen, currentSpeed)`

4) 逐帧突进与路径伤害（委托 `JianSuoRuntime`）：
   - `JianSuoRuntime.tryDashAndDamage(player, dir, dashDist, dmg, RAY_WIDTH, MAX_DASH_T, HIT_ONCE_T)`
   - 要点：
     - 碰撞预测：每步计算 `movedBB = bbox.move(step)`，`level.noCollision(movedBB)` 为真才推进；否则提前终止
     - 胶囊采样：`AABB sweep = new AABB(prevPos, newPos).inflate(RAY_WIDTH, player.getBbHeight()*0.5, RAY_WIDTH)`
     - 友方过滤：`CombatEntityUtil.areEnemies(player, entity)`
     - 去重：`session.hitEntities.add(entity.getId())` + `now - LAST_HIT.getOrDefault(uuid,0) >= HIT_ONCE_T`
     - 造成伤害：`target.hurt(player.damageSources().playerAttack(player), (float)dmg)` + 轻击退

5) 冷却：
   - `MultiCooldown cd = MultiCooldown.builder(state).withSync(cc, organ).build();`
   - `cd.entry(KEY_READY_AT).setReadyAt(now + computeCooldownTicks(daoHen));`
   - `ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);`

---

## 4. 被动：受击后退与减伤（`JianSuoGuEvadePassive`）

接口：`OrganIncomingDamageListener.onIncomingDamage(...)` 内路由到本类静态方法。

执行步骤：
1) 冷却检查：
   - `MultiCooldown cd = MultiCooldown.builder(state).withSync(cc, organ).build();`
   - `Entry evade = cd.entry(KEY_EVADE_READY_AT)`；若 `evade.getReadyTick() > now` → 原样返回伤害

2) 掷骰：
   - 从快照或实时读取 `daoHen`；`chance = JianSuoCalc.evadeChance(daoHen)`
   - `if (!RandomSource.create().nextDouble() < chance) return damage;`

3) 计算减伤并应用：
   - `reduce = JianSuoCalc.evadeReduce(daoHen)`
   - `float remaining = amount * (float)(1.0 - reduce)`

4) 反向后退：
   - `Vec3 back = JianSuoRuntime.backstepVector(victim, source).normalize().scale(EVADE_BACKSTEP);`
   - `JianSuoRuntime.safeSlide(victim, back)`（检查 `level.noCollision`，尝试小步移动；若失败，退化为 `setDeltaMovement(back*0.35)`）

5) 无敌帧：
   - `victim.invulnerableTime = Math.max(victim.invulnerableTime, EVADE_I_FRAMES_T);`（短暂无敌）

6) 写入冷却：
   - `evade.setReadyAt(now + secondsToTicks(EVADE_COOLDOWN_S));`

7) 返回 `remaining`。

与其它免伤互斥：依赖 `OrganDefenseEvents` 的顺序，若其它 `IncomingDamageShield` 已将伤害降为 0 则不触发本被动；同一 tick 仅生效一类免伤（通过 `DamagePipeline` 保证一次管线）。

---

## 5. 飞剑：定时自动突进（`JianSuoGuSwordAutoDashHook`）

实现接口：`FlyingSwordEventHook.onTick(TickContext ctx)`。

流程：
1) 判定主人是否具备“剑梭蛊”器官：
   - 通过 `ChestCavityEntity.of(owner)` → `ChestCavityInstance cc` → 遍历 `cc.inventory` 检出本器官。

2) 冷却与间隔：
   - 维护 `Map<Integer /*swordId*/, Long /*lastDashTick*/>`（静态 `ConcurrentHashMap`）。
   - 若 `now - last < secondsToTicks(SWORD_DASH_INTERVAL_S)` → 跳过。

3) 方向与距离/伤害：
   - 方向：优先 `sword.getTargetEntity()` 朝向；否则 `sword.getViewVector(1.0F)` 的水平分量。
   - 读取主人道痕（快照对主动技，飞剑用实时值即可）：`daoHen = GuzhenrenResourceBridge.open(owner).getDouble("jiandao:daohen_jiandao")`
   - `dist = JianSuoCalc.dashDistance(daoHen) * 0.6`（状态系数占位）
   - `dmg = JianSuoCalc.pathDamage(daoHen, sword.getDeltaMovement().length()) * 0.6`

4) 执行突进与路径伤害：
   - 复用 `JianSuoRuntime.tryDashAndDamage(sword, dir, dist, dmg, RAY_WIDTH, /*steps*/6, HIT_ONCE_T)`
   - 友方过滤：`CombatEntityUtil.areEnemies(owner, target)`（以主人立场判断敌友）

5) 记录时间与 FX：
   - `LAST_SWORD_DASH.put(sword.getId(), now);`
   - 适度粒子/音效（沿用 `ZhiZhuangGuOrganBehavior.spawnDashTrail/ImpactFx` 风格，轻量）。

---

## 6. 工具/纯逻辑（`JianSuoRuntime` 关键函数）

- `boolean tryDashAndDamage(Entity self, Vec3 dir, double dist, double dmg, double halfWidth, int maxSteps, int hitDedupTicks)`
  - `Vec3 step = dir.normalize().scale(dist / maxSteps);`
  - for 1..maxSteps：
    - `AABB moved = self.getBoundingBox().move(step); if (!level.noCollision(moved)) break; self.setPos(self.position().add(step));`
    - `AABB sweep = new AABB(prevPos, self.position()).inflate(halfWidth, self.getBbHeight()*0.5, halfWidth);`
    - `List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, sweep, pred)`
    - 过滤：非自身、`areEnemies(attacker, it)`、`hitDedup ok`
    - `it.hurt(damageSource, (float)dmg);` + 轻击退 `it.push(dx, 0.05, dz)`

- `Vec3 backstepVector(LivingEntity self, DamageSource src)`：优先按来源位置反向，否则按 `self.getLookAngle().scale(-1)`。
- `void safeSlide(LivingEntity self, Vec3 delta)`：切 6–8 步尝试 `noCollision`，可退化为设定 `DeltaMovement`。

命中去重：
- `private static final Map<Integer, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();`
- 判定：`now - LAST_HIT_TICK.getOrDefault(entity.getId(), 0L) >= hitDedupTicks`。

---

## 7. 冷却/状态键（`JianSuoGuOrganBehavior`）

- `STATE_ROOT = "JianSuoGu"`
- `KEY_READY_AT`（主动技冷却）
- `KEY_EVADE_READY_AT`（被动冷却）
- MultiCooldown 构建：`MultiCooldown.builder(state).withSync(cc, organ).build()`

---

## 8. 资源/道痕快照（`ActivationHookRegistry`）

- 已在“注册要求”中添加：`ResourceFieldSnapshotEffect("jiandao:", ["daohen_jiandao", "liupai_jiandao"])`，确保主动激活读到一致参数。

---

## 9. 数据与本地化

- 物品/器官定义（占位，后续补数值）：
  - `src/main/resources/data/chestcavity/organs/guzhenren/human/jian_dao/jian_suo_gu_3.json`
  - `src/main/resources/data/chestcavity/organs/guzhenren/human/jian_dao/jian_suo_gu_4.json`
  - `src/main/resources/data/chestcavity/organs/guzhenren/human/jian_dao/jian_suo_gu_5.json`

- 本地化键：
  - `item.guzhenren.jiansuogu`: "剑梭蛊"（3转）
  - `item.guzhenren.jiansuogusizhuan`: "剑梭蛊"（4转）
  - `item.guzhenren.jiansuoguwuzhuan`: "剑梭蛊"（5转）

---

## 10. 日志与调试

- `DEBUG` 级别输出：激活时打印 `daoHen/dashDist/dmg/dir`；被动触发时打印 `chance/roll/reduce/backstep`；飞剑定时突进打印 `owner/swordId/dist/dmg`（节流，每次突进一次）。

---

## 11. 测试用例（JUnit 5，纯逻辑）

- `JianSuoCalcTest`：
  - `dashDistance(0/100/200/400)` 封顶 ≤ 17.0
  - `pathDamage(daoHen, v)` 单调递增，`vScale∈[0,0.25]`
  - `evadeChance/evadeReduce` 上下限与斜率符合预期

- `JianSuoRuntimeTest`：
  - 胶囊采样集合并去重（可用伪实体/简化 AABB 框架）
  - `backstepVector` 在缺少 direct entity 时退化为朝向反向

---

## 12. 验收清单（DoD）

- [ ] JSON/注册：三转/四转/五转“剑梭蛊”物品与器官数据、本地化键就绪
- [ ] 资源桥：主动技能严格扣除真元/精力（玩家可选念头），不足时拒绝
- [ ] 突进运动：逐帧碰撞预测，终点安全不穿模
- [ ] 路径命中：胶囊采样准确，友方过滤与去重有效
- [ ] 飞剑突进：每 S 秒短突进，对非友方有效，状态系数可调
- [ ] 被动躲避：概率/减伤/后退/无敌帧正确，冷却可靠，互斥无冲突
- [ ] DEBUG：关键参数在调试级别可追踪
- [ ] 编译与基础平衡：以 4/5 转一阶段通过初测

---

## 13. 落地顺序建议

1) `tuning/` + `runtime/` + `calculator/` 骨架与单测
2) `organ/behavior` + 主动 `active/` + 被动 `passive/` 接口对接（`OrganActivationListeners`、`OrganIncomingDamageListener`）
3) `ActivationHookRegistry` 快照注册 + 客户端热键加入
4) 飞剑 Hook 实现并注册
5) 数据与本地化文件
6) `./gradlew compileJava` + `./gradlew test` + 手动游戏内定向验证

---

## 14. 备注（实现细节提示）

- 友方判断统一复用 `CombatEntityUtil.areEnemies(attacker, target)`。
- 冷却统一由 `MultiCooldown` 管理，并与 `OrganStateOps` 同步；不要直接写入 NBT。
- 资源统一 `ResourceOps`（玩家 `consumeStrict`，非玩家可 `consumeWithFallback`）。
- FX 占位：
  - 起手：音效 `SoundEvents.PLAYER_ATTACK_SWEEP`（低音量），少量 `ParticleTypes.SWEEP_ATTACK`
  - 命中：`SoundEvents.ANVIL_LAND`（低音量），`ParticleTypes.CRIT` 少量
  - 躲避：`SoundEvents.WIND_CHARGE_SHOOT`（极低音量），`ParticleTypes.CLOUD` 少量

