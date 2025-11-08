# 三转·剑脉蛊（Jianmai Gu）实现计划（落地清单）

目标：提供“周身飞剑 → 距离 → 剑脉效率（JME）”的被动循环；将 JME 转换为临时的“剑道道痕”增幅倍率（仅影响计算期，不落账）；在 GUARD 模式提升格挡率；主动技按指定资源集合消耗，发放一段时间的额外倍率券；与既有剑道技能/飞剑系统无缝接线。

— 本文仅列“需要调用的函数/事件/类、遵循的规则、文件与键名”，可直接按序开发。

## 一、文件与类（新增/修改）

- 新增（常量）：`compat/guzhenren/item/jian_dao/tuning/JianmaiTuning.java`
  - 含可调常量：JME 刷新参数、单剑贡献、玩家/飞剑增益系数、BLOCK 概率参数、临时增幅参数、主动技参数与上限。

- 新增（玩家 NBT 工具）：`compat/guzhenren/item/jian_dao/runtime/JianmaiNBT.java`
  - 负责读写玩家 PersistentData（命名空间 `chestcavity.jianmai`）下的：
    - `JME`(double)、`JME_Tick`(long)、`JME_Radius`(double)、`JME_DistAlpha`(double)、`JME_MaxCap`(double)
    - `JME_Amp` 复合：`ampK`(double)、`mult`(double)、`expireGameTime`(long)、`graceTicks`(int)
  - 使用世界刻 `level.getGameTime()` 判定过期；提供 `clearAll(ServerPlayer)` 立刻归零并移除节点。

- 新增（临时倍率逻辑）：`compat/guzhenren/item/jian_dao/runtime/JianmaiAmpOps.java`
  - `refreshFromJME(ServerPlayer nowPlayer, double jme, long now)`：当 `jme>0` 时刷新 `expireGameTime=max(expire, now + AMP_BASE_DURATION_TICKS)` 并计算衰减区线性插值 `mult = lerp_to_1(base, grace)`，`base=1+DAO_JME_K*jme`。
  - `applyActiveTicket(ServerPlayer, double activeMult, long now, int durationTicks)`：叠乘到 `JME_Amp.mult`，裁剪上限 `AMP_MULT_CAP`，刷新过期时间到更长。
  - `finalMult(ServerPlayer, long now)`：返回 `mult_from_JME(t) * mult_from_ACTIVE(t)` 并裁剪上限。

- 新增（被动事件）：`compat/guzhenren/item/jian_dao/events/JianmaiPlayerTickEvents.java`
  - `@EventBusSubscriber(modid = ChestCavity.MODID)`；服务端 `PlayerTickEvent`，每 `JME_TICK_INTERVAL` 刷新一次：
    1) 调用 `scanFriendlySwords(...)` 采样 `FlyingSwordEntity`（半径 `JME_Radius`）；过滤 `sword.isAlive() && sword.isOwnedBy(player)`。
    2) 距离权重：`w(d)=max(0, 1 - (d/R)^ALPHA)`；单剑贡献：`BASE_PER_SWORD * w(d) * (1 + LEVEL_BONUS*(level-1))`；聚合并 `clamp(0, JME_MaxCap)`。
    3) 写回 `JianmaiNBT.writeJME(player, JME)` 与 `writeLastTick(player, now)`；
    4) `JianmaiAmpOps.refreshFromJME(player, JME, now)`；
    5) 动态属性应用（见“六、运行时应用”）。

- 新增（格挡事件）：`compat/guzhenren/item/jian_dao/events/JianmaiGuardBlockEvents.java`
  - 监听 `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent`；
  - 条件：`target instanceof ServerPlayer p`，近身范围内存在 `AIMode.GUARD` 的友方飞剑（如半径 3.0）；
  - 正面判定：基于 `p.getViewVector(1)` 与 `atk.position()-p.position()` 的夹角（<=90°）；
  - 概率：`chance = clamp(BLOCK_BASE + BLOCK_K * JME, 0, 0.95)`；命中则 `event.setAmount(0)` 并静默。

- 新增（器官行为）：`compat/guzhenren/item/jian_dao/behavior/organ/JianmaiGuOrganBehavior.java`
  - `enum` 单例实现 `OrganRemovalListener`（必要时也可实现慢 tick，但本器官逻辑主在线上事件）；
  - `static { OrganActivationListeners.register(ABILITY_ID, JianmaiGuOrganBehavior::activateAbility); }`
  - `activateAbility(LivingEntity entity, ChestCavityInstance cc)`：
    - 仅服务端、`ServerPlayer`；
    - 资源支付：`ResourceOps.payCost(player, ResourceCost, hint)`；
    - 强化券：`activeMult = 1 + ACTIVE_DAOMARK_K * clamp(COST_RAW * ACTIVE_COST_K, 0, ACTIVE_SOFTCAP)` → `JianmaiAmpOps.applyActiveTicket(player, activeMult, now, ACTIVE_DURATION_TICKS)`；
    - 可选冷却：用 `MultiCooldown` 绑定 `OrganState.of(organ, "JianmaiGu")` 的 `"ActiveReadyAt"`；到点以 `ActiveSkillRegistry.scheduleReadyToast(...)` 提示；
  - `onRemoved(...)`：`JianmaiNBT.clearAll(player)` 并移除相关属性修饰（`chestcavity:modifiers/jianmai_speed`, `.../jianmai_atk`）。

- 修改（注册）：`compat/guzhenren/item/jian_dao/JiandaoOrganRegistry.java`
  - `OrganIntegrationSpec.builder(ResourceLocation.parse("guzhenren:jian_mai_gu"))`
    - `.addRemovalListener(JianmaiGuOrganBehavior.INSTANCE)`（如需慢 tick可再添加）；
    - `.build()` 并并入 `SPECS`。

- 修改（客户端热键）：`compat/guzhenren/item/jian_dao/JiandaoClientAbilities.java`
  - 在 `onClientSetup` 追加字面 ID：`guzhenren:jianmai_overdrive`。

- 新增（计算期“通用道痕增幅”）：
  - `compat/guzhenren/item/jian_dao/calculator/JiandaoDaohenOps.java`
    - `compute(ChestCavityInstance cc)`：
      1) 读 base：`ResourceOps.openHandle(player).map(h -> h.read("daohen_jiandao")).orElse(0.0)`；
      2) 读乘区：`JianmaiAmpOps.finalMult(player, now)`；
      3) 返回 `base * finalMult`（不落账）。
  - `compat/guzhenren/item/jian_dao/behavior/ComputedJiandaoDaohenEffect.java`（实现 `Effect`）
    - `applyPre`: `SkillEffectBus.putMetadata(ctx, "daohen", JiandaoDaohenOps.compute(ctx.chestCavity()))`。
  - `registration/ActivationHookRegistry.java`
    - 为 `^guzhenren:(jiandao/.*|jian_.*|.*_slash|.*_wave|flying_sword_.*)$` 注册 `ComputedJiandaoDaohenEffect`（沿用已有 `ResourceFieldSnapshotEffect` 以快照字段）。

- 修改（飞剑系统）
  - `compat/guzhenren/flyingsword/calculator/context/CalcContext.java`：新增 `public double ownerJme = 0.0;`
  - `compat/guzhenren/flyingsword/calculator/context/CalcContexts.java`：在 `from(FlyingSwordEntity sword)` 里，若 owner 为玩家：`ctx.ownerJme = net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.JianmaiNBT.readJME(owner)`；
  - 钩子：在 `DefaultHooks.registerDefaults()` 或单独注册一个 Hook：
    ```java
    FlyingSwordCalcRegistry.register((ctx, out) -> {
      double j = ctx.ownerJme;
      if (j > 0.0) {
        out.speedBaseMult *= (1.0 + JianmaiTuning.SWORD_SPD_K * j);
        out.speedMaxMult  *= (1.0 + JianmaiTuning.SWORD_SPD_K * j);
        out.damageMult    *= (1.0 + JianmaiTuning.SWORD_ATK_K * j);
      }
    });
    ```

## 二、事件与规则

- 被动刷新：`PlayerTickEvent`（服务端），间隔 `JME_TICK_INTERVAL`；按“距离权重+等级加成”聚合 JME → `JianmaiAmpOps.refreshFromJME`。
- Guard 格挡：`LivingIncomingDamageEvent`（服务端），正面锥形+GUARD 飞剑条件下按 `BLOCK_BASE + BLOCK_K * JME` 概率免伤（上限 0.95）。
- 主动技：热键包触发 → `OrganActivationListeners.activate(id, cc)` → `JianmaiGuOrganBehavior.activateAbility`，用 `ResourceOps.payCost` 支付后发券。
- OnRemove：立刻 `JME=0；清空 JME_Amp；移除属性修饰`，静默不落日志。
- 临时增幅：仅影响“计算期”的 `daohen_jiandao` 读取（通过 `ComputedJiandaoDaohenEffect` 注入元数据）；严禁写回永久值。

## 三、数据结构（NBT 键）

命名空间：`chestcavity.jianmai`

```json
{
  "JME": 0.0,
  "JME_Tick": 0,
  "JME_Radius": 12.0,
  "JME_DistAlpha": 1.25,
  "JME_MaxCap": 2.5,
  "JME_Amp": {
    "ampK": 0.10,
    "mult": 1.0,
    "expireGameTime": 0,
    "graceTicks": 40
  }
}
```

过期判断：`now >= expireGameTime` → 清空节点；`mult` 在 `graceTicks` 内线性插值回 1.0。

## 四、算法与公式（实现细节）

- 距离权重：`w(d)=max(0, 1 - (d/R)^ALPHA)`
- 单剑贡献：`contrib = BASE_PER_SWORD * w(d) * (1 + LEVEL_BONUS * (level-1))`
- JME 聚合：`JME = clamp(sum, 0, JME_MaxCap)`
- 临时增幅：
  - 基于 JME：`base = 1 + DAO_JME_K * JME`；`expire = max(expire, now + AMP_BASE_DURATION_TICKS)`；`mult = lerp_to_1_if_in_grace(base)`
  - 主动券：`activeMult = 1 + ACTIVE_DAOMARK_K * clamp(COST_RAW * ACTIVE_COST_K, 0, ACTIVE_SOFTCAP)`；叠乘并裁剪 `AMP_MULT_CAP`；`expire` 取更长
  - 最终倍率：`final = clamp(mult_JME * mult_ACTIVE, 1.0, AMP_MULT_CAP)`
- Guard 概率：`chance = clamp(BLOCK_BASE + BLOCK_K * JME, 0, 0.95)`

## 五、运行时应用（属性/飞剑）

- 玩家属性（0.5s 刷新时更新；仅实时，不落账）：
  - `Attributes.MOVEMENT_SPEED`：`ADD_MULTIPLIED_TOTAL`，amount=`PLAYER_SPEED_K * JME`，ID=`chestcavity:modifiers/jianmai_speed`
  - `Attributes.ATTACK_DAMAGE`：`ADD_MULTIPLIED_BASE`，amount=`PLAYER_ATK_K * JME`，ID=`.../jianmai_atk`
  - OnRemove：用 `AttributeInstance.removeModifier(ResourceLocation)` 清除上述两项。

- 飞剑乘区：通过 `FlyingSwordCalcRegistry.register` 钩子读取 `ctx.ownerJme`，对 `out.speedBaseMult/out.speedMaxMult/out.damageMult` 乘 `(1 + K*JME)`（K 见常量）。

## 六、主动技（不落账）

- 费用：`ResourceOps.payCost(ServerPlayer, ResourceCost, hint)`，成功后计算 `activeMult` 并 `JianmaiAmpOps.applyActiveTicket`。
- 冷却（可选）：`MultiCooldown` + `OrganState` 键 `"ActiveReadyAt"`；`ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now)`。
- 客户端热键：`JiandaoClientAbilities` 以字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`。

## 七、遵循规范

- Attack Ability 注册：`enum` 单例 + static 注册，客户端热键仅登记字面 ID，禁止在构造函数做注册/重逻辑。
- Ops 统一：资源→`ResourceOps`；冷却→`MultiCooldown`；禁止直接 `LinkageManager.adjust/ledger.remove`。
- 临时增幅：仅 NBT 临时态 + 世界刻过期；严禁写回 `daohen_jiandao` 永久值。
- 日志静音：INFO 以下，默认不打日志；仅错误异常打印。

## 八、单元测试（纯逻辑）

位置：`src/test/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/`

- `JianmaiMathTest`：`w(d)`、单剑贡献、截断、概率上限。
- `JianmaiAmpOpsTest`：JME→Amp 刷新/叠乘/衰减到期（世界刻推进为参数）。
- `JiandaoDaohenOpsTest`：乘 `finalMult` 仅影响计算期（通过桩实现 handle 读取）。

（注意：Minecraft 核心类不可 Mockito mock；仅测纯函数/静态计算器）

## 九、验收清单

- [ ] 玩家周身飞剑→JME 被动每 0.5s 刷新，半径/指数可镜像常量
- [ ] JME 临时增幅严格过期；离线/跨服不可卡永久
- [ ] 玩家移速/攻击力与飞剑速度/伤害随 JME 实时放大
- [ ] GUARD 正面格挡按 JME 提升，最大 95%
- [ ] 主动技支付资源→发券；倍率与 JME 增幅相乘，受上限；不落账
- [ ] 剑道技能在“通用道痕增幅”管线读到已乘倍率后的值
- [ ] OnRemove 归零+清理所有属性修饰

