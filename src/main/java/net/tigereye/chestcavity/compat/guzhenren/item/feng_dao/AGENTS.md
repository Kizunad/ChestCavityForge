# 风道（feng_dao）标准化重构完成文档

本文档记录风道模块按照 `DAO_REFACTORING_GUIDE_CHECKSTYLE.md` 完成的标准化重构。

---

## 📋 重构概览

**重构日期**: 2025-01-12
**参考指南**: `DAO_REFACTORING_GUIDE.md` + `DAO_REFACTORING_GUIDE_CHECKSTYLE.md`
**代码规范**: Google Java Style (checkstyle 10.20.0)
**重构状态**: ✅ 完成

---

## 🎯 重构目标

将风道模块从分散的代码结构重构为标准化的DAO架构：
1. ✅ 统一冷却时间计算（基于流派经验）
2. ✅ 统一道痕值计算（可扩展框架）
3. ✅ 注册到全局技能系统
4. ✅ 符合 Checkstyle 代码规范

---

## 📁 目录结构（重构后）

```
feng_dao/
├── calculator/                          # ✅ 新增 - 计算逻辑层
│   ├── FengDaoCooldownOps.java             # 冷却时间计算
│   ├── FengDaoDaohenOps.java               # 道痕值计算
│   └── QingFengCalculator.java             # 清风轮蛊专用计算
├── behavior/                            # ✅ 已存在 - 行为逻辑
│   └── QingFengLunOrganBehavior.java       # 清风轮蛊器官行为（已更新）
├── tuning/                              # ✅ 已存在 - 调参常量
│   └── FengTuning.java                     # 风道数值配置
├── fx/                                  # ✅ 已存在 - 特效表现
│   └── FengFx.java                         # 风道特效
├── messages/                            # ✅ 已存在 - 消息提示
│   └── FengMessages.java                   # 风道消息
├── FengDaoOrganRegistry.java            # ✅ 已存在 - 器官注册
├── FengDaoClientAbilities.java          # ✅ 已存在 - 客户端
└── AGENTS.md                            # ✅ 本文档
```

---

## 🔧 核心组件详解

### 1. FengDaoCooldownOps（冷却计算）

**文件**: `calculator/FengDaoCooldownOps.java`
**职责**: 基于流派经验计算技能冷却时间

#### 核心方法

```java
// 标准冷却计算（最低1秒）
public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp)

// 自定义最小冷却
public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp, long minTicks)
```

#### 计算公式

```
实际冷却 = DaoCooldownCalculator.calculateCooldown(基础冷却, 流派经验)
         = baseTicks * (1 - reduction)

reduction = clamp(liupaiFengdaoExp / 10001, 0.0, 0.95)
最低冷却 = 20 ticks (1秒)
```

#### 使用示例

```java
// 主动技能：从快照读取流派经验
int liupaiExp = (int) SkillEffectBus.consumeMetadata(
    player, ABILITY_ID, "fengdao:liupai_fengdao", 0.0);
long cooldown = FengDaoCooldownOps.withFengDaoExp(200L, liupaiExp);
readyEntry.setReadyAt(now + cooldown);

// 被动技能：直接读取资源
int liupaiExp = (int) ResourceOps.openHandle(player)
    .map(h -> h.read("liupai_fengdao").orElse(0.0))
    .orElse(0.0);
long cooldown = FengDaoCooldownOps.withFengDaoExp(160L, liupaiExp);
```

---

### 2. FengDaoDaohenOps（道痕计算）

**文件**: `calculator/FengDaoDaohenOps.java`
**职责**: 汇总风道器官的道痕值

#### 核心方法

```java
// 计算道痕总值
public static double compute(ChestCavityInstance cc)
```

#### 注册的器官

| 器官名称 | 物品ID | 每个道痕值 |
|---------|--------|-----------|
| 清风轮蛊 | `guzhenren:qing_feng_lun_gu` | 1.0 |

#### 扩展方式

```java
// 在构造函数中注册新的道痕提供器
registerProvider(cc -> {
    int organCount = countOrgans(cc, ORGAN_ID);
    return calculateDaohen(organCount, DAOHEN_PER_STACK);
});
```

#### 使用示例

```java
// 在技能效果中使用道痕加成
double daohen = FengDaoDaohenOps.compute(cc);
float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f); // 每点道痕+10%伤害
```

---

### 3. 技能注册与快照

**文件**: `registration/ActivationHookRegistry.java`（已更新）

#### 注册的家族

```java
registerFamily("liupai_fengdao");   // 风道流派经验
registerFamily("daohen_fengdao");   // 风道道痕
```

#### 技能快照配置

```java
// 为所有 qing_feng_lun_gu/* 技能注册快照
SkillEffectBus.register(
    "^guzhenren:qing_feng_lun_gu/.*$",
    CompositeEffect.of(
        new ResourceFieldSnapshotEffect(
            "fengdao:",  // 快照前缀
            List.of("daohen_fengdao", "liupai_fengdao")  // 快照字段
        )
    )
);
```

#### 快照字段访问

```java
// 在技能激活时读取快照值
int liupaiExp = (int) SkillEffectBus.consumeMetadata(
    player, skillId, "fengdao:liupai_fengdao", 0.0);
double daohen = SkillEffectBus.consumeMetadata(
    player, skillId, "fengdao:daohen_fengdao", 0.0);
```

---

### 4. 技能冷却更新

**文件**: `behavior/QingFengLunOrganBehavior.java`（已更新）

#### 更新的技能

| 技能 | 技能ID | 基础冷却 | 更新内容 |
|-----|--------|---------|---------|
| 突进 (Dash) | `qing_feng_lun_gu/dash` | 120 ticks (6秒) | ✅ 使用流派经验减免 |
| 风域 (Wind Domain) | `qing_feng_lun_gu/wind_domain` | 900 ticks (45秒) | ✅ 使用流派经验减免 |
| 风环护盾 (Wind Ring) | 被动触发 | 160 ticks (8秒) | ✅ 使用流派经验减免 |

#### 更新前后对比

**更新前**:
```java
// 硬编码冷却时间
readyEntry.setReadyAt(now + FengTuning.DASH_COOLDOWN_TICKS);
```

**更新后**:
```java
// 动态计算冷却时间（主动技能从快照读取）
int liupaiExp = (int) SkillEffectBus.consumeMetadata(
    player, DASH_ABILITY_ID, "fengdao:liupai_fengdao", 0.0);
long cooldown = FengDaoCooldownOps.withFengDaoExp(
    FengTuning.DASH_COOLDOWN_TICKS, liupaiExp);
readyEntry.setReadyAt(now + cooldown);

// 被动技能直接读取资源
int liupaiExp = (int) ResourceOps.openHandle(player)
    .map(h -> h.read("liupai_fengdao").orElse(0.0))
    .orElse(0.0);
long ringCooldown = FengDaoCooldownOps.withFengDaoExp(
    FengTuning.WIND_RING_COOLDOWN_TICKS, liupaiExp);
```

---

## 📊 冷却时间对照表

### 突进技能 (Dash)

| 流派经验 | 减免比例 | 实际冷却 |
|---------|---------|---------|
| 0 | 0% | 120 ticks (6.0秒) |
| 2500 | ~25% | 90 ticks (4.5秒) |
| 5000 | ~50% | 60 ticks (3.0秒) |
| 7500 | ~75% | 30 ticks (1.5秒) |
| 10001+ | 95% | 20 ticks (1.0秒，最低) |

### 风域技能 (Wind Domain)

| 流派经验 | 减免比例 | 实际冷却 |
|---------|---------|---------|
| 0 | 0% | 900 ticks (45.0秒) |
| 2500 | ~25% | 675 ticks (33.8秒) |
| 5000 | ~50% | 450 ticks (22.5秒) |
| 7500 | ~75% | 225 ticks (11.3秒) |
| 10001+ | 95% | 45 ticks (2.3秒) |

### 风环护盾 (Wind Ring)

| 流派经验 | 减免比例 | 实际冷却 |
|---------|---------|---------|
| 0 | 0% | 160 ticks (8.0秒) |
| 2500 | ~25% | 120 ticks (6.0秒) |
| 5000 | ~50% | 80 ticks (4.0秒) |
| 7500 | ~75% | 40 ticks (2.0秒) |
| 10001+ | 95% | 20 ticks (1.0秒，最低) |

---

## ✅ Checkstyle 验证

### 代码规范检查

```bash
./gradlew checkstyleMain
```

**结果**: ✅ 通过（0 violations）

### 关键规范点

- ✅ **Javadoc**: 所有 public 方法都有完整文档，第一句以句号结尾
- ✅ **行长度**: 所有行不超过 100 字符，超长行正确换行
- ✅ **缩进**: 使用 2 个空格（非 Tab）
- ✅ **Import 顺序**: 按 java/javax → minecraft → 第三方 → static 排序
- ✅ **命名规范**: 类名 PascalCase，方法名 camelCase，常量 UPPER_SNAKE_CASE

---

## 🧪 测试建议

### 功能测试清单

- [ ] **冷却计算测试**
  - [ ] 无流派经验时，冷却 = 基础冷却
  - [ ] 满流派经验(10001)时，冷却 = 20 ticks (1秒)
  - [ ] 中等流派经验(5000)时，冷却约为基础的50%

- [ ] **道痕计算测试**
  - [ ] 无器官时，道痕 = 0
  - [ ] 1个清风轮蛊时，道痕 = 1.0
  - [ ] 多个清风轮蛊时，道痕 = 器官数量 * 1.0

- [ ] **技能注册验证**
  - [ ] `ActivationHookRegistry.isFamilyEnabled("liupai_fengdao")` 返回 true
  - [ ] `ActivationHookRegistry.isFamilyEnabled("daohen_fengdao")` 返回 true

- [ ] **游戏内测试**
  - [ ] 突进技能可以正常触发
  - [ ] 风域技能冷却时间随流派经验变化
  - [ ] 风环护盾被动正常工作

### 单元测试（可选）

```java
// calculator 层可以添加单元测试
@Test
void testCooldownCalculation() {
    long base = 200L;
    assertEquals(200L, FengDaoCooldownOps.withFengDaoExp(base, 0));
    assertEquals(20L, FengDaoCooldownOps.withFengDaoExp(base, 10001));
}
```

---

## 📝 迁移与扩展指南

### 添加新器官的道痕

1. 在 `FengDaoDaohenOps` 构造函数中注册新的 provider：

```java
private FengDaoDaohenOps() {
    // 现有：清风轮蛊
    registerProvider(cc -> {
        // ... existing code ...
    });

    // 新增：假设有新器官"风刃蛊"
    registerProvider(cc -> {
        Item windBladeItem = BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "feng_ren_gu")
        );
        int count = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack organ = cc.inventory.getItem(i);
            if (organ.getItem() == windBladeItem) {
                count += organ.getCount();
            }
        }
        return calculateDaohen(count, 2.0); // 每个提供2.0道痕
    });
}
```

### 添加新技能的冷却计算

1. 在 `ActivationHookRegistry` 中确保技能ID匹配正则表达式
2. 在技能激活方法中读取快照并使用 `FengDaoCooldownOps`：

```java
private static void activateNewSkill(ServerPlayer player, ChestCavityInstance cc) {
    // ... 前置检查 ...

    // 读取快照的流派经验
    int liupaiExp = (int) SkillEffectBus.consumeMetadata(
        player, NEW_SKILL_ID, "fengdao:liupai_fengdao", 0.0);

    // 计算冷却
    long cooldown = FengDaoCooldownOps.withFengDaoExp(
        FengTuning.NEW_SKILL_COOLDOWN_TICKS, liupaiExp);

    readyEntry.setReadyAt(now + cooldown);
}
```

---

## 🔗 依赖与工具

### 必需工具类

- ✅ `DaoCooldownCalculator` - 通用冷却计算器
- ✅ `DaohenCalculator` - 通用道痕计算基类
- ✅ `SkillEffectBus` - 技能快照系统
- ✅ `ResourceOps` - 资源读写工具
- ✅ `MultiCooldown` - 多键冷却管理

### 配置文件

- ✅ `FengTuning.java` - 所有数值配置
- ✅ `ActivationHookRegistry.java` - 全局技能注册

---

## 🚀 未来优化方向

### 优先级：高

- [ ] 将 `FengDaoDaohenOps` 迁移为使用配置文件的道痕倍率（而非硬编码 1.0）
- [ ] 为 calculator 层添加单元测试

### 优先级：中

- [ ] 将所有硬编码的技能ID提取到常量文件
- [ ] 优化道痕计算性能（考虑缓存）

### 优先级：低

- [ ] 添加更多风道器官
- [ ] 扩展风道组合技能系统

---

## 📚 参考文档

- **重构指南**: `DAO_REFACTORING_GUIDE.md`
- **代码规范**: `DAO_REFACTORING_GUIDE_CHECKSTYLE.md`
- **通用工具**: `util/DaoCooldownCalculator.java`, `util/DaohenCalculator.java`
- **全局注册**: `registration/ActivationHookRegistry.java`

---

## ✨ 提交信息

```
feat(feng_dao): refactor to standardized DAO structure

按照 DAO_REFACTORING_GUIDE_CHECKSTYLE.md 完成风道(feng_dao)标准化重构：

1. 创建 calculator 子包
   - FengDaoCooldownOps: 基于流派经验的冷却时间计算
   - FengDaoDaohenOps: 道痕值计算框架(清风轮蛊)

2. 注册到 ActivationHookRegistry
   - 注册 liupai_fengdao 和 daohen_fengdao 家族
   - 为 qing_feng_lun_gu/* 技能注册快照效果

3. 更新技能冷却逻辑
   - activateDash: 突进技能使用流派经验减免冷却
   - activateWindDomain: 风域技能使用流派经验减免冷却
   - onIncomingDamage: 风环护盾被动使用流派经验减免冷却

所有代码符合 Google Java Style (checkstyle 通过)。
```

---

**文档版本**: v1.0
**最后更新**: 2025-01-12
**维护者**: ChestCavity Mod Team
