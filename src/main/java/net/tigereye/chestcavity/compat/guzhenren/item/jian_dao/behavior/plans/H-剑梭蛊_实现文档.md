# 剑梭蛊（JianSuoGu）实现文档

> 版本：Implementation v1.0
> 实现日期：2025-11-08
> 模块：ChestCavityForge · 剑道系器官（主动+被动+飞剑增强）

---

## 概述

剑梭蛊是一个3-5转剑道器官，提供：
- **主动技能「剑梭突进」**：向前突进并沿路径造成伤害
- **被动效果「剑影身法」**：受击时有概率后退、减伤、获得无敌帧
- **飞剑增强**：飞剑定时自动沿朝向短突进

---

## 文件结构

### 核心逻辑类
```
compat/guzhenren/item/jian_dao/
├── tuning/JianSuoGuTuning.java           # 常量配置
├── calculator/JianSuoCalc.java            # 计算公式
├── runtime/jian_suo/JianSuoRuntime.java   # 运行时逻辑
├── behavior/
│   ├── organ/JianSuoGuOrganBehavior.java  # 器官行为枚举
│   ├── organ/JianSuoGuState.java          # 状态键定义
│   ├── active/JianSuoGuActive.java        # 主动技能
│   └── passive/JianSuoGuEvadePassive.java # 被动躲避
└── flyingsword/events/hooks/
    └── JianSuoGuSwordAutoDashHook.java    # 飞剑钩子
```

### 数据文件
```
resources/data/chestcavity/organs/guzhenren/human/jian_dao/
├── jian_suo_gu_3.json  # 三转器官数据
├── jian_suo_gu_4.json  # 四转器官数据
└── jian_suo_gu_5.json  # 五转器官数据
```

### 本地化
```
resources/assets/chestcavity/lang/zh_cn.json
├── item.guzhenren.jiansuogu         # 三转翻译
├── item.guzhenren.jiansuogusizhuan  # 四转翻译
└── item.guzhenren.jiansuoguwuzhuan  # 五转翻译
```

---

## 功能详解

### 1. 主动技能：剑梭突进

**能力 ID**：`guzhenren:jian_suo_gu_dash`

**执行流程**（`JianSuoGuActive.java`）：
1. 冷却检查（60-120 ticks，基于道痕递减）
2. 资源消耗：
   - 真元：6.0（基础，按转数缩放）
   - 精力：2.0（基础，按转数缩放）
   - 念头：1.0（玩家可选）
3. 读取剑道道痕（通过 `GuzhenrenResourceBridge`）
4. 计算突进距离与伤害：
   - 距离 = `BASE_DASH_DISTANCE * (1 + 0.25 * DH100)`，上限17格
   - 伤害 = `BASE_ATK * (1 + 0.35 * DH100) * (1 + vScale)`
5. 执行突进（`JianSuoRuntime.tryDashAndDamage`）：
   - 逐帧推进（最多12步）
   - 碰撞预测（遇障碍提前终止）
   - 胶囊体采样命中（半宽0.8）
   - 友方过滤（`CombatEntityUtil.areEnemies`）
   - 命中去重（10 tick窗口）
   - 造成伤害 + 轻击退

**方向决策**：
- 玩家：视线方向的水平分量
- NPC：优先朝向目标（距离≤24格），否则朝向

**特效**：
- 起手：`SWEEP_ATTACK` 粒子 + `PLAYER_ATTACK_SWEEP` 音效
- 命中：`CRIT` 粒子 + `ANVIL_LAND` 音效

---

### 2. 被动效果：剑影身法

**触发条件**：受击时

**执行流程**（`JianSuoGuEvadePassive.java`）：
1. 冷却检查（6秒全局冷却）
2. 概率触发：
   - 基础几率：10%
   - 每100道痕增加：6%
   - 上限：60%
3. 减伤计算：
   - 基础减伤：10%
   - 每100道痕增加：8%
   - 上限：90%
4. 反向后退：
   - 距离：2.4格
   - 方向：伤害源反向（若无来源则朝向反向）
   - 安全移动：分8步检测碰撞，失败则退化为速度设定
5. 无敌帧：6 ticks
6. 返回减伤后的伤害

**特效**：
- 躲避：`CLOUD` 粒子 + `WIND_CHARGE_SHOOT` 音效

**NPC 支持**：完全生效，无区别对待

---

### 3. 飞剑增强：自动突进

**触发条件**：每3秒（`SWORD_DASH_INTERVAL_S`）

**执行流程**（`JianSuoGuSwordAutoDashHook.java`）：
1. 检查主人是否拥有剑梭蛊器官（任意转数）
2. 冷却检查（每把飞剑独立冷却）
3. 读取主人的剑道道痕（实时值）
4. 计算参数：
   - 距离 = `dashDistance(daohen) * 0.6`
   - 伤害 = `pathDamage(daohen, swordVelocity) * 0.6`
5. 方向决策：
   - 优先：目标方向
   - 否则：飞剑视线方向（水平分量）
6. 执行突进（复用 `JianSuoRuntime.tryDashAndDamage`）
   - 步数：6（相比主动减半）
   - 其他机制同主动技能

**友方过滤**：以主人立场判断（`CombatEntityUtil.areEnemies(owner, target)`）

---

## 常量配置

### 主动突进（`JianSuoGuTuning`）
| 常量 | 值 | 说明 |
|------|-----|------|
| `BASE_DASH_DISTANCE` | 5.0 | 基础距离（格） |
| `BASE_ATK` | 1.0 | 基础伤害系数 |
| `RAY_WIDTH` | 0.8 | 胶囊体半宽 |
| `MAX_DASH_STEPS` | 12 | 最大推进步数 |
| `HIT_ONCE_DEDUP_TICKS` | 10 | 命中去重窗口 |
| `MAX_DASH_DISTANCE` | 17.0 | 距离上限 |
| `DASH_DIST_PER_100_DAOHEN` | 0.25 | 道痕距离增幅 |
| `DAMAGE_PER_100_DAOHEN` | 0.35 | 道痕伤害增幅 |
| `VELOCITY_SCALE_MAX` | 0.25 | 速度增幅上限 |

### 被动躲避
| 常量 | 值 | 说明 |
|------|-----|------|
| `EVADE_COOLDOWN_S` | 6.0 | 冷却时间（秒） |
| `EVADE_BACKSTEP_DISTANCE` | 2.4 | 后退距离（格） |
| `EVADE_INVULN_FRAMES` | 6 | 无敌帧（tick） |
| `EVADE_CHANCE_BASE` | 0.10 | 基础触发几率 |
| `EVADE_CHANCE_PER_100` | 0.06 | 每100道痕增幅 |
| `EVADE_CHANCE_MAX` | 0.60 | 几率上限 |
| `EVADE_REDUCE_MIN` | 0.10 | 最小减伤 |
| `EVADE_REDUCE_PER_100` | 0.08 | 每100道痕增幅 |
| `EVADE_REDUCE_MAX` | 0.90 | 减伤上限 |

### 飞剑增强
| 常量 | 值 | 说明 |
|------|-----|------|
| `SWORD_DASH_INTERVAL_S` | 3.0 | 自动突进间隔（秒） |
| `SWORD_DASH_DISTANCE_SCALE` | 0.6 | 距离缩放（相对主动） |
| `SWORD_DASH_DAMAGE_SCALE` | 0.6 | 伤害缩放（相对主动） |

### 资源消耗
| 常量 | 值 | 说明 |
|------|-----|------|
| `BASE_COST_ZHENYUAN` | 6.0 | 真元（按转数缩放） |
| `BASE_COST_JINGLI` | 2.0 | 精力（按转数缩放） |
| `BASE_COST_NIANTOU` | 1.0 | 念头（玩家可选） |

### 冷却时间
| 常量 | 值 | 说明 |
|------|-----|------|
| `ACTIVE_COOLDOWN_MIN_TICKS` | 60 | 最小冷却（3秒） |
| `ACTIVE_COOLDOWN_MAX_TICKS` | 120 | 最大冷却（6秒） |

---

## 器官属性

### 三转（`jiansuogu`）
- 剑道道痕：+50
- 速度：-2
- 力量：-5

### 四转（`jiansuogusizhuan`）
- 剑道道痕：+80
- 速度：-3
- 力量：-8

### 五转（`jiansuoguwuzhuan`）
- 剑道道痕：+120
- 速度：-4
- 力量：-10

---

## 注册路径

### 1. 器官行为注册
**位置**：`JiandaoOrganRegistry.java`

```java
// 三转
OrganIntegrationSpec.builder(JianSuoGuOrganBehavior.ORGAN_ID_3)
    .addIncomingDamageListener(JianSuoGuOrganBehavior.INSTANCE)
    .build()

// 四转、五转同理
```

### 2. 主动技能注册
**位置**：`JianSuoGuOrganBehavior.java` 静态初始化块

```java
static {
    OrganActivationListeners.register(
        ABILITY_ID,
        JianSuoGuOrganBehavior::activateAbility
    );
}
```

### 3. 飞剑钩子注册
**位置**：`FlyingSwordEventInit.java`

```java
FlyingSwordEventRegistry.register(
    new JianSuoGuSwordAutoDashHook()
);
```

---

## 公式总结

### 距离计算（`JianSuoCalc.dashDistance`）
```
DH100 = floor(daohen / 100)
dist = clamp(BASE_DASH_DISTANCE * (1 + 0.25 * DH100), 0, 17.0)
```

### 伤害计算（`JianSuoCalc.pathDamage`）
```
DH100 = floor(daohen / 100)
vScale = min(velocity / 10.0, 0.25)
damage = BASE_ATK * (1 + 0.35 * DH100) * (1 + vScale)
```

### 躲避几率（`JianSuoCalc.evadeChance`）
```
DH100 = floor(daohen / 100)
chance = clamp(0.10 + 0.06 * DH100, 0, 0.60)
```

### 减伤比例（`JianSuoCalc.evadeReduce`）
```
DH100 = floor(daohen / 100)
reduce = clamp(0.10 + 0.08 * DH100, 0.10, 0.90)
```

### 冷却时间（`JianSuoCalc.activeCooldown`）
```
DH100 = floor(daohen / 100)
ratio = min(DH100 / 10.0, 1.0)
cd = MAX_CD - ratio * (MAX_CD - MIN_CD)
cd = max(cd, MIN_CD)
```

---

## 状态管理

### NBT 根键：`"JianSuoGu"`

### 状态字段（`JianSuoGuState`）
| 键名 | 类型 | 说明 |
|------|------|------|
| `active_ready_tick` | Long | 主动技能冷却就绪时间 |
| `evade_ready_tick` | Long | 被动躲避冷却就绪时间 |

### 冷却管理（`MultiCooldown`）
```java
MultiCooldown cd = MultiCooldown.builder(state)
    .withSync(cc, organ)
    .build();

cd.entry(KEY_READY_TICK).setReadyAt(now + cooldownTicks);
```

---

## 调试日志

### DEBUG 级别输出（SLF4J Logger）

**主动技能**：
```
[JianSuoGuActive] daohen={}, dashDist={}, damage={}, cd={}
[JianSuoGuActive] Dashed {} blocks
```

**被动躲避**：
```
[JianSuoGuEvadePassive] Triggered! daohen={}, chance={}
[JianSuoGuEvadePassive] Damage reduced from {} to {} (reduce={})
```

**飞剑突进**：
```
[JianSuoGuSwordDash] Sword {} dashing: daohen={}, dist={}, dmg={}
[JianSuoGuSwordDash] Actual distance: {}
```

**运行时逻辑**：
```
[JianSuoRuntime] Dash blocked at step {}/{}, pos={}
[JianSuoRuntime] Dash hit entity {} at pos={}, damage={}
[JianSuoRuntime] safeSlide: {}/{} steps, final pos={}
```

---

## 测试要点

### 主动技能测试
1. 资源不足时拒绝激活
2. 冷却中拒绝激活
3. 突进距离随道痕递增
4. 伤害随道痕、速度递增
5. 遇障碍提前终止
6. 友方不被命中
7. 同一实体在10 tick内只命中一次

### 被动效果测试
1. 冷却中不触发
2. 触发概率符合预期（统计100次）
3. 减伤比例正确
4. 后退安全（不穿墙）
5. 无敌帧生效
6. NPC 同样触发

### 飞剑增强测试
1. 主人无剑梭蛊时不触发
2. 间隔3秒触发一次
3. 每把飞剑独立冷却
4. 突进距离、伤害为主动的60%
5. 友方过滤正确

---

## 性能优化

### 命中去重
- 使用 `ConcurrentHashMap` 存储上次命中时间
- 定期清理（通过 `onDespawnOrRecall` 钩子）

### 碰撞检测
- 分步检测（12步）避免一次性大范围计算
- 遇障碍提前终止，减少无效计算

### 飞剑冷却
- 每把飞剑独立记录（`Map<Integer, Long>`）
- 消散时清理记录（防止内存泄漏）

---

## 已知限制

1. **穿墙风险**：快速推进可能略微穿过薄墙（已通过分步检测最小化）
2. **友伤判定**：依赖 `isAlliedTo`，可能受到模组兼容性影响
3. **飞剑突进方向**：无目标时仅取视线方向，可能不够灵活
4. **冷却精度**：基于 tick（1/20秒），可能与显示时间略有偏差

---

## 扩展建议

1. **主动技能**：
   - 添加冲刺加速效果
   - 突进路径留下剑气残影
   - 蓄力机制（蓄力越久距离越远）

2. **被动效果**：
   - 触发时生成幻影分身吸引仇恨
   - 后退方向可配置（前/后/左/右）
   - 连续触发递减几率（防止无限躲避）

3. **飞剑增强**：
   - 突进后返回原位（来回穿刺）
   - 多段突进（连续3次短突进）
   - 与其他剑道蛊联动（如剑引蛊标记目标）

---

## 版本历史

### v1.0 (2025-11-08)
- 初始实现
- 完成主动技能、被动效果、飞剑增强
- 完成注册与本地化
- 通过基础功能测试

---

## 参考文件

- 实现计划：`H-剑梭蛊_实现计划.md`
- 器官数据：`jian_suo_gu_*.json`
- 本地化：`zh_cn.json`
