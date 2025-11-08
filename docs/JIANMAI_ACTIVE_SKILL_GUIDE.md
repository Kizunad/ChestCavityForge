# 剑脉蛊（Jianmai Gu）主动技能开发指南

> **最后更新**: 2025-11-09
> **状态**: 接口已完成，准备开发新的主动技能

## 目录

1. [架构概览](#架构概览)
2. [快速开始](#快速开始)
3. [详细步骤](#详细步骤)
4. [参数配置](#参数配置)
5. [代码示例](#代码示例)
6. [测试和调试](#测试和调试)
7. [常见问题](#常见问题)

---

## 架构概览

### 整体架构

剑脉蛊（Jianmai Gu）采用 **被动 + 主动 分离设计**：

```
玩家 Tick 事件
    ↓
[1] 检查并回滚过期增幅（被动和主动分别检查）
    ↓
[2] 扫描周围飞剑，计算 JME（剑脉效率）
    ↓
[3] 刷新被动增幅（基于 JME）
    ↓
[4] 应用玩家属性修饰（移速、攻击力）
    ↓
[5] 主动技能激活（用户手动触发）
    ↓
[6] 应用主动道痕增幅
```

### 数据存储结构

**位置**: `player.getPersistentData()` 下的 `"chestcavity.jianmai"` 命名空间

```json
{
  "JME": 2.5,                    // 当前剑脉效率值（0-2.5）
  "JME_Tick": 1000,              // 上次 JME 刷新时间戳
  "JME_Radius": 12.0,            // 扫描半径（方块）
  "JME_DistAlpha": 1.25,         // 距离权重指数
  "JME_MaxCap": 2.5,             // JME 上限
  "JME_Amp": {                   // 临时增幅数据
    "ampK": 0.10,                // 道痕增幅系数
    "passiveDelta": 50.0,        // 被动道痕增量
    "passiveExpire": 1400,       // 被动过期时间（游戏刻）
    "activeDelta": 100.0,        // 主动道痕增量
    "activeExpire": 1300,        // 主动过期时间（游戏刻）
    "graceTicks": 40             // 宽限期（用于重登等）
  }
}
```

### 关键概念

#### 直接值调整（Direct Value Adjustment）

与其他系统不同，剑脉蛊增幅 **直接修改显示值**：

```
原始道痕：1060
+ 被动增幅：+50（来自 JME）
+ 主动增幅：+100（来自技能）
= 显示值：1210（用户可见）
```

不使用隐藏的倍率计算，保证用户看到的数值是实际生效的增幅。

#### 被动与主动的独立性

- **被动**: 由 JME 自动驱动，持续时间约 20 秒，每 10 tick 刷新一次
- **主动**: 由玩家技能激活，持续时间可配置（默认 15 秒）

两者可以**同时生效和叠加**：

```
被动增幅：passiveDelta = baseDaohen * ampK * JME
主动增幅：activeDelta = 用户指定（由技能消耗决定）
```

---

## 快速开始

### 最小化实现（10 分钟）

对于简单的主动技能，只需要：

```java
// 1. 在 JianmaiGuOrganBehavior.activateAbility() 中获取消耗和增幅量
double costScaled = /* 从消耗计算 */;
double deltaAmount = costScaled * JianmaiTuning.ACTIVE_DAOMARK_K;

// 2. 调用接口应用增幅
JianmaiAmpOps.applyActiveBuff(
    player,
    deltaAmount,                              // 道痕增量
    JianmaiTuning.ACTIVE_DURATION_TICKS,     // 持续时间（默认 300 ticks = 15 秒）
    now
);
```

**就这么简单！** 系统会自动处理：
- ✅ 道痕值调整
- ✅ 过期时间追踪
- ✅ 自动回滚处理
- ✅ 缓存失效

---

## 详细步骤

### 第 1 步：定义参数

在 `JianmaiTuning.java` 中添加参数：

```java
/** 主动技能道痕增幅系数 */
public static final double ACTIVE_DAOMARK_K = 0.8;

/** 主动技能消耗成本系数 */
public static final double ACTIVE_COST_K = 0.5;

/** 主动技能消耗软上限（防止无限增幅） */
public static final double ACTIVE_SOFTCAP = 200.0;

/** 主动技能持续时间（游戏刻） */
public static final int ACTIVE_DURATION_TICKS = 300;  // 15 秒

/** 主动技能冷却时间（游戏刻） */
public static final int ACTIVE_COOLDOWN_TICKS = 600;  // 30 秒
```

### 第 2 步：定义消耗规则

在 `JianmaiGuOrganBehavior.activateAbility()` 中实现消耗逻辑：

```java
private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player)) return;

    // 获取器官实例
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) return;

    // 获取冷却状态
    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = player.serverLevel().getGameTime();
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(K_ACTIVE_READY_AT);

    // 检查冷却
    if (!ready.isReady(now)) {
        long remaining = ready.remaining(now);
        player.displayClientMessage(
            Component.translatable("guzhenren.ability.cooldown", remaining / 20.0),
            true
        );
        return;
    }

    // 【关键】消耗资源
    OptionalDouble consumed = ResourceOps.tryConsumeTieredZhenyuan(
        player,
        5, 1,                              // 五转·一阶段
        ZhenyuanBaseCosts.Tier.BURST       // 爆发档
    );

    if (consumed.isEmpty()) {
        player.displayClientMessage(
            Component.translatable("guzhenren.ability.insufficient_resource"),
            true
        );
        return;
    }

    // 【关键】计算增幅量
    double costRaw = consumed.getAsDouble();
    double costScaled = Math.max(0.0,
        Math.min(costRaw * JianmaiTuning.ACTIVE_COST_K,
                 JianmaiTuning.ACTIVE_SOFTCAP)
    );
    double deltaAmount = costScaled * JianmaiTuning.ACTIVE_DAOMARK_K;

    // 【关键】应用主动增幅
    JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);

    // 启动冷却
    long readyAt = now + JianmaiTuning.ACTIVE_COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);

    // 反馈消息
    player.displayClientMessage(
        Component.translatable(
            "guzhenren.ability.jianmai.activated",
            String.format("+%.0f", deltaAmount),
            JianmaiTuning.ACTIVE_DURATION_TICKS / 20.0
        ),
        true
    );
}
```

### 第 3 步：注册主动技能

在 `JianmaiGuOrganBehavior` 的静态块中注册：

```java
static {
    OrganActivationListeners.register(ABILITY_ID, JianmaiGuOrganBehavior::activateAbility);
}
```

### 第 4 步：处理卸载清理

在 `JianmaiGuOrganBehavior.onRemoved()` 中完整清理：

```java
@Override
public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player)) return;

    // 清空 NBT 数据
    JianmaiNBT.clearAll(player);

    // 回滚所有增幅（包括被动和主动）
    JianmaiAmpOps.clearAll(player);

    // 移除玩家属性修饰（移速、攻击力）
    JianmaiPlayerTickEvents.removePlayerAttributes(player);
}
```

---

## 参数配置

### 核心参数说明

| 参数 | 默认值 | 说明 | 影响 |
|------|--------|------|------|
| `ACTIVE_DAOMARK_K` | 0.8 | 主动消耗转换系数 | 消耗越多，增幅越多 |
| `ACTIVE_COST_K` | 0.5 | 成本倍数 | 控制消耗量基数 |
| `ACTIVE_SOFTCAP` | 200.0 | 消耗软上限 | 防止无限增幅 |
| `ACTIVE_DURATION_TICKS` | 300 | 持续时间（ticks） | 15 秒后过期回滚 |
| `ACTIVE_COOLDOWN_TICKS` | 600 | 冷却时间（ticks） | 30 秒后可再用 |

### 与被动参数的关系

```
被动增幅量 = 基础道痕 × DAO_JME_K × JME
主动增幅量 = 消耗量 × ACTIVE_COST_K × ACTIVE_DAOMARK_K

例如：
基础道痕 = 1000
JME = 2.0
DAO_JME_K = 0.1
被动增幅 = 1000 × 0.1 × 2.0 = 200

消耗量 = 100
ACTIVE_COST_K = 0.5
ACTIVE_DAOMARK_K = 0.8
主动增幅 = 100 × 0.5 × 0.8 = 40

总显示 = 1000 + 200 + 40 = 1240
```

### 调优建议

**如果增幅太强**:
- 降低 `ACTIVE_DAOMARK_K`（如 0.5）
- 增加 `ACTIVE_COST_K`（如 0.8）
- 降低 `ACTIVE_SOFTCAP`（如 100）

**如果增幅太弱**:
- 提高 `ACTIVE_DAOMARK_K`（如 1.2）
- 降低 `ACTIVE_COST_K`（如 0.3）
- 提高 `ACTIVE_SOFTCAP`（如 300）

---

## 代码示例

### 示例 1：消耗真元的主动技能（当前实现）

```java
// 消耗五转·一阶段·爆发档真元
OptionalDouble consumed = ResourceOps.tryConsumeTieredZhenyuan(
    player, 5, 1, ZhenyuanBaseCosts.Tier.BURST
);

if (consumed.isEmpty()) {
    // 真元不足
    return;
}

double deltaAmount = consumed.getAsDouble() * JianmaiTuning.ACTIVE_COST_K * JianmaiTuning.ACTIVE_DAOMARK_K;
JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);
```

### 示例 2：消耗生命值的主动技能

```java
// 消耗 20% 生命值
double maxHealth = player.getMaxHealth();
double costHealth = maxHealth * 0.2;
player.hurt(player.damageSources().magic(), (float) costHealth);

// 计算增幅（消耗越多增幅越多）
double deltaAmount = costHealth * JianmaiTuning.ACTIVE_DAOMARK_K;
JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);
```

### 示例 3：消耗精力的主动技能

```java
// 消耗精力
Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
if (handleOpt.isEmpty()) return;

GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
OptionalDouble jingLi = handle.read("jingli");

if (jingLi.isEmpty() || jingLi.getAsDouble() < 100) {
    // 精力不足
    return;
}

// 消耗 100 点精力
double costEnergy = 100.0;
handle.adjustDouble("jingli", -costEnergy, true);

// 计算增幅
double deltaAmount = costEnergy * 0.5 * JianmaiTuning.ACTIVE_DAOMARK_K;
JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);
```

### 示例 4：条件触发的主动技能（如需要飞剑）

```java
// 检查是否有飞剑
List<FlyingSwordEntity> swords = player.level().getEntitiesOfClass(
    FlyingSwordEntity.class,
    player.getBoundingBox().inflate(20.0),
    sword -> sword.isAlive() && isOwnedBy(sword, player)
);

if (swords.isEmpty()) {
    player.displayClientMessage(Component.literal("需要飞剑！"), true);
    return;
}

// 消耗一把飞剑作为代价
FlyingSwordEntity sword = swords.get(0);
double swordLevel = sword.getSwordLevel();
double deltaAmount = swordLevel * 50.0;  // 等级越高增幅越多
sword.discard();  // 移除飞剑

JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);
```

---

## 测试和调试

### 启用调试日志

在 `logback.xml` 中配置（如果存在）或通过运行参数：

```bash
./gradlew runClient --args="--debug" 2>&1 | grep "\[JianmaiAmp\]"
```

调试日志会输出：

```
[JianmaiAmp] Applied active buff for player: oldActive=0.0, newActive=100.0, duration=15.0s
[JianmaiAmp] Active expired! Rolled back 100.0 for player
```

### 常见调试场景

#### 增幅未生效

检查清单：
1. ✅ 是否成功消耗了资源？（查看日志 "tryConsume"）
2. ✅ 是否计算了正确的 deltaAmount？（查看日志中的 newActive 值）
3. ✅ 道痕值是否实际改变了？（在游戏中查看显示值）

**诊断命令**（仅限服务端）：

```java
// 在 activateAbility() 中添加临时日志
LOGGER.info("[DEBUG] Cost: {}, Delta: {}", costRaw, deltaAmount);
LOGGER.info("[DEBUG] Resource handle: {}", handle.read("daohen_jiandao"));

// 应用后
JianmaiAmpOps.applyActiveBuff(...);
LOGGER.info("[DEBUG] After apply: {}", handle.read("daohen_jiandao"));
```

#### 过期回滚不工作

检查清单：
1. ✅ 玩家是否保持在线？（离线期间不会自动回滚，重新登录时会）
2. ✅ 是否有足够的 Tick 时间让过期检查运行？
3. ✅ PlayerTickEvent 是否正确触发？

**验证方法**：

```java
// 在 JianmaiPlayerTickEvents.onPlayerTick() 中添加
boolean rolled = JianmaiAmpOps.rollbackIfExpired(player, now);
if (rolled) {
    LOGGER.info("[DEBUG] Rollback executed for player {}", player.getName().getString());
}
```

#### 冷却不工作

检查清单：
1. ✅ MultiCooldown 是否正确初始化？
2. ✅ 冷却时间是否足够长？（检查 ACTIVE_COOLDOWN_TICKS）
3. ✅ 是否正确调用了 `ready.setReadyAt()`？

### 单元测试框架

创建 `JianmaiAmpOpsTest.java`：

```java
@ExtendWith(MockitoExtension.class)
public class JianmaiAmpOpsTest {

    @Mock
    private ServerPlayer player;

    @Mock
    private GuzhenrenResourceBridge.ResourceHandle handle;

    @Test
    public void testApplyActiveBuff_DirectAdjustment() {
        // 给定初始道痕值
        when(handle.read("daohen_jiandao")).thenReturn(OptionalDouble.of(1000.0));

        // 当应用 100 点增幅时
        JianmaiAmpOps.applyActiveBuff(player, 100.0, 300, 0);

        // 应该调整道痕值
        verify(handle).adjustDouble("daohen_jiandao", 100.0, true);
    }

    @Test
    public void testRollbackIfExpired_PassiveExpired() {
        // 给定被动已过期
        CompoundTag ampData = new CompoundTag();
        ampData.putDouble("passiveDelta", 50.0);
        ampData.putLong("passiveExpire", 0);  // 已过期
        ampData.putDouble("activeDelta", 0.0);

        // 当执行过期检查时
        boolean rolled = JianmaiAmpOps.rollbackIfExpired(player, 100);

        // 应该回滚被动增幅
        assertTrue(rolled);
        verify(handle).adjustDouble("daohen_jiandao", -50.0, true);
    }
}
```

---

## 常见问题

### Q1: 被动和主动增幅会冲突吗？

**A**: 不会。它们是完全独立的：
- 被动由 JME 自动维护（刷新间隔 10 tick）
- 主动由技能激活（持续 15 秒）
- 可以同时生效，增量直接相加

如果同时有被动 +50 和主动 +100，显示值会增加 +150。

### Q2: 如果玩家在技能持续期间卸载器官会怎样？

**A**: 卸载时会同时回滚被动和主动：

```java
JianmaiAmpOps.clearAll(player);  // 回滚被动 + 主动
```

所以如果卸载前有 +150 增幅，会直接扣掉 150。

### Q3: 如何处理重复激活技能的情况？

**A**: 每次激活都会 **先回滚旧的主动增幅，再应用新的**：

```java
// 旧增幅自动回滚
if (oldActiveDelta != 0.0) {
    handle.adjustDouble("daohen_jiandao", -oldActiveDelta, true);
}

// 应用新增幅
handle.adjustDouble("daohen_jiandao", deltaAmount, true);
```

所以即使快速连击也不会堆叠。

### Q4: 如何实现"增幅随飞剑数量变化"？

**A**: 在 `refreshFromJME()` 中已经实现了：

```java
// JME 计算已经包含了飞剑数量
double jme = scanAndCalculateJME(player);  // 考虑飞剑数量

// 被动增幅会随 JME 变化
double newPassiveDelta = baseDaohen * ampK * jme;
```

主动增幅可以在激活时考虑飞剑：

```java
List<FlyingSwordEntity> swords = /* 扫描飞剑 */;
double swordBonus = swords.size() * 10.0;  // 每把飞剑 +10 增幅
double deltaAmount = baseDelta + swordBonus;
JianmaiAmpOps.applyActiveBuff(player, deltaAmount, duration, now);
```

### Q5: 如何在多人模式下测试？

**A**: 每个玩家有独立的 NBT 数据：

```java
// 玩家 A 的数据
player_A.getPersistentData().getCompound("chestcavity.jianmai")

// 玩家 B 的数据
player_B.getPersistentData().getCompound("chestcavity.jianmai")
```

完全隔离，不会相互影响。

### Q6: 如何扩展到其他器官？

**A**: 使用同样的接口即可：

```java
// 在新的器官行为类中
private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    // ...消耗资源...
    double deltaAmount = /* 计算增幅 */;

    // 使用同样的接口
    JianmaiAmpOps.applyActiveBuff(player, deltaAmount, duration, now);
}
```

只需要确保：
1. 在 `onRemoved()` 中调用 `JianmaiAmpOps.clearAll()`
2. 在主动技能激活前检查冷却
3. 提供清晰的反馈消息

---

## 相关文件速查

| 文件 | 用途 | 关键方法 |
|------|------|---------|
| `JianmaiGuOrganBehavior.java` | 主动技能入口 | `activateAbility()` |
| `JianmaiAmpOps.java` | 增幅操作核心 | `applyActiveBuff()`, `rollbackIfExpired()`, `clearAll()` |
| `JianmaiNBT.java` | 数据持久化 | `writeAmp()`, `readAmp()` |
| `JianmaiPlayerTickEvents.java` | 被动事件处理 | `onPlayerTick()` |
| `JianmaiTuning.java` | 参数配置 | 所有常量定义 |
| `JiandaoDaohenOps.java` | 道痕计算 | `effectiveCached()` |

---

## 后续改进方向

- [ ] 实现"增幅链"机制（多次激活累积增幅）
- [ ] 添加"溅射"效果（增幅延伸到队友）
- [ ] 支持"条件触发"（达到某值自动激活）
- [ ] 可视化增幅效果（粒子、着色器）
- [ ] 增幅衰减函数（随时间递减）

---

**祝你开发顺利！** 如有问题欢迎提问。
