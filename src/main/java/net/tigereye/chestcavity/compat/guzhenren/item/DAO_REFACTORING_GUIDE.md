# 道系统标准化重构指南

本指南提供了将任意"道"重构为标准化结构的完整步骤。

---

## 📋 目录

1. [准备工作](#准备工作)
2. [标准目录结构](#标准目录结构)
3. [步骤1: 创建calculator子包](#步骤1-创建calculator子包)
4. [步骤2: 重构fx特效](#步骤2-重构fx特效可选)
5. [步骤3: 重构behavior行为](#步骤3-重构behavior行为可选)
6. [步骤4: 注册到ActivationHookRegistry](#步骤4-注册到activationhookregistry)
7. [步骤5: 更新技能冷却逻辑](#步骤5-更新技能冷却逻辑)
8. [步骤6: 测试验证](#步骤6-测试验证)
9. [完整示例](#完整示例-风道fengdao)

---

## 准备工作

### 确认通用工具类已存在

确保以下工具类已创建:
- ✅ `net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator`
- ✅ `net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator`

### 确定道的信息

以**风道(feng_dao)**为例:
- **包路径**: `net.tigereye.chestcavity.compat.guzhenren.item.feng_dao`
- **中文名**: 风道
- **拼音**: fengdao
- **技能ID前缀**: `guzhenren:feng_*` (例如 `guzhenren:feng_blade`, `guzhenren:feng_tornado`)

---

## 标准目录结构

```
feng_dao/
├── calculator/                    # 必须 - 计算逻辑
│   ├── FengDaoCooldownOps.java       # 冷却计算
│   ├── FengDaoDaohenOps.java         # 道痕计算
│   └── [其他参数计算类]
├── fx/                            # 可选 - 特效
│   └── [特效类]
├── behavior/                      # 可选 - 行为逻辑
│   └── [行为类]
├── runtime/                       # 可选 - 运行时状态
│   └── [状态管理类]
├── tuning/                        # 推荐 - 调参
│   └── [参数常量类]
├── FengDaoOrganRegistry.java      # 必须 - 器官注册
└── FengDaoClientAbilities.java    # 可选 - 客户端
```

---

## 步骤1: 创建calculator子包

### 1.1 创建冷却计算类

**文件**: `feng_dao/calculator/FengDaoCooldownOps.java`

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 风道冷却时间计算
 */
public final class FengDaoCooldownOps {

  private FengDaoCooldownOps() {}

  /**
   * 根据风道流派经验计算冷却
   *
   * @param baseTicks 基础冷却(ticks)
   * @param liupaiFengdaoExp 流派经验(liupai_fengdao)
   * @return 实际冷却,最低20ticks(1秒)
   */
  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp);
  }

  /**
   * 根据风道流派经验计算冷却(自定义最小值)
   *
   * @param baseTicks 基础冷却(ticks)
   * @param liupaiFengdaoExp 流派经验(liupai_fengdao)
   * @param minTicks 最低冷却(ticks)
   * @return 实际冷却
   */
  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp, long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp, minTicks);
  }
}
```

### 1.2 创建道痕计算类

**文件**: `feng_dao/calculator/FengDaoDaohenOps.java`

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;
import net.tigereye.chestcavity.registration.CCOrganScores;

/**
 * 风道道痕计算
 */
public final class FengDaoDaohenOps extends DaohenCalculator {

  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  private FengDaoDaohenOps() {
    // 注册风道相关器官的道痕提供器
    // 示例1: 假设有一个风系器官,每个提供1.0道痕
    registerProvider(cc -> calculateDaohen(
        cc.getOrganScore(CCOrganScores.FENG_ORGAN),  // 替换为实际的器官Score
        1.0  // 每个器官提供1.0道痕
    ));

    // 示例2: 可以注册多个器官
    // registerProvider(cc -> calculateDaohen(
    //     cc.getOrganScore(CCOrganScores.FENG_ELITE_ORGAN),
    //     2.5  // 精英器官提供更多道痕
    // ));
  }

  /**
   * 计算风道道痕总值
   *
   * @param cc 胸腔实例
   * @return 道痕值
   */
  public static double compute(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
```

**⚠️ 重要提示:**
- 需要根据实际的器官系统替换 `CCOrganScores.FENG_ORGAN`
- 如果暂时没有器官,可以先留空或返回固定值用于测试

---

## 步骤2: 重构fx特效(可选)

如果道有特效代码,将其移动到 `fx/` 子包:

### 2.1 创建fx目录

```bash
mkdir -p feng_dao/fx
```

### 2.2 移动特效类

将所有特效相关的类移动到 `fx/` 目录下,例如:
- 粒子特效
- 音效
- 视觉效果

**命名规范**: `[技能名]Fx.java`

示例:
```
feng_dao/fx/
├── FengBladeSlashFx.java
├── FengTornadoFx.java
└── FengVisualEffects.java
```

---

## 步骤3: 重构behavior行为(可选)

如果有技能行为逻辑,创建 `behavior/` 子包:

### 3.1 创建behavior目录

```bash
mkdir -p feng_dao/behavior
```

### 3.2 移动或创建行为类

将技能的核心逻辑移动到独立的行为类中。

**命名规范**: `[技能名]Behavior.java`

示例:
```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior;

public class FengBladeBehavior {
    public static void executeSlash(ServerPlayer player, ...) {
        // 技能执行逻辑
    }
}
```

---

## 步骤4: 注册到ActivationHookRegistry

在 `ActivationHookRegistry.register()` 方法中添加注册代码。

### 4.1 注册流派和道痕家族

在 `register()` 方法的开头添加:

```java
// 注册风道流派和道痕家族
registerFamily("liupai_fengdao");
registerFamily("daohen_fengdao");
```

### 4.2 注册技能效果快照

在 `SkillEffectBus.register()` 调用处添加:

```java
// 技能效果: 风道技能需要快照道痕与流派经验
SkillEffectBus.register(
    "^guzhenren:feng_.*$",  // 匹配所有以 feng_ 开头的技能
    CompositeEffect.of(
        new ResourceFieldSnapshotEffect(
            "fengdao:",  // 快照字段前缀
            List.of("daohen_fengdao", "liupai_fengdao")  // 需要快照的资源字段
        )
    ));
```

**完整示例** (在 `ActivationHookRegistry.java` 中):

```java
public static void register() {
    if (initialised) {
        return;
    }
    initialised = true;

    // ... 现有注册 ...

    // ==================== 风道注册 ====================
    registerFamily("liupai_fengdao");
    registerFamily("daohen_fengdao");

    SkillEffectBus.register(
        "^guzhenren:feng_.*$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect(
                "fengdao:",
                List.of("daohen_fengdao", "liupai_fengdao")
            )
        ));

    // ... 其他注册 ...
}
```

---

## 步骤5: 更新技能冷却逻辑

在技能的激活逻辑中使用新的冷却计算。

### 5.1 找到技能注册位置

通常在 `FengDaoOrganRegistry.java` 或类似的注册类中。

### 5.2 更新冷却计算

**旧代码示例**:
```java
// 硬编码的冷却时间
long cooldown = 200L; // 10秒
```

**新代码示例**:
```java
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.FengDaoCooldownOps;

// 从快照字段读取流派经验
int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);

// 计算实际冷却,基础10秒,根据流派经验减免,最低1秒
long baseCooldown = 200L; // 10秒
long actualCooldown = FengDaoCooldownOps.withFengDaoExp(baseCooldown, liupaiExp);

// 设置冷却
player.getCooldowns().addCooldown(item, (int) actualCooldown);
```

### 5.3 在技能效果中使用道痕

如果技能伤害或效果需要道痕加成:

```java
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.FengDaoDaohenOps;

// 计算道痕值
double daohen = FengDaoDaohenOps.compute(cc);

// 应用道痕加成到伤害
float baseDamage = 10.0f;
float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f); // 每点道痕+10%伤害
```

---

## 步骤6: 测试验证

### 6.1 编译检查

```bash
./gradlew compileJava
```

### 6.2 功能测试清单

- [ ] **冷却计算**
  - 无流派经验时,冷却 = 基础冷却
  - 满流派经验(10001)时,冷却 = 20 ticks (1秒)
  - 中等流派经验时,冷却介于两者之间

- [ ] **道痕计算**
  - 无器官时,道痕 = 0
  - 有器官时,道痕 = 器官数量 × 倍率

- [ ] **注册验证**
  - `ActivationHookRegistry.isFamilyEnabled("liupai_fengdao")` 返回 true
  - `ActivationHookRegistry.isFamilyEnabled("daohen_fengdao")` 返回 true

- [ ] **技能触发**
  - 技能可以正常触发
  - 冷却时间符合预期
  - 效果增幅符合预期

---

## 完整示例: 风道(FengDao)

### 目录结构

```
feng_dao/
├── calculator/
│   ├── FengDaoCooldownOps.java
│   ├── FengDaoDaohenOps.java
│   └── FengBladeParamCalc.java  (可选)
├── fx/
│   ├── FengBladeFx.java
│   └── FengTornadoFx.java
├── behavior/
│   ├── FengBladeBehavior.java
│   └── FengTornadoBehavior.java
├── FengDaoOrganRegistry.java
└── FengDaoClientAbilities.java
```

### 完整代码示例

#### FengDaoCooldownOps.java

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

public final class FengDaoCooldownOps {
  private FengDaoCooldownOps() {}

  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp);
  }
}
```

#### FengDaoDaohenOps.java

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;
import net.tigereye.chestcavity.registration.CCOrganScores;

public final class FengDaoDaohenOps extends DaohenCalculator {
  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  private FengDaoDaohenOps() {
    registerProvider(cc -> calculateDaohen(
        cc.getOrganScore(CCOrganScores.FENG_ORGAN), 1.0));
  }

  public static double compute(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
```

#### ActivationHookRegistry 注册

```java
// 在 register() 方法中添加:

registerFamily("liupai_fengdao");
registerFamily("daohen_fengdao");

SkillEffectBus.register(
    "^guzhenren:feng_.*$",
    CompositeEffect.of(
        new ResourceFieldSnapshotEffect(
            "fengdao:",
            List.of("daohen_fengdao", "liupai_fengdao")
        )
    ));
```

#### 技能使用示例

```java
// 在风刃技能的激活方法中:

int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);
double daohen = FengDaoDaohenOps.compute(cc);

long cooldown = FengDaoCooldownOps.withFengDaoExp(200L, liupaiExp);
float damage = 10.0f * (1.0f + (float) daohen * 0.1f);

player.getCooldowns().addCooldown(item, (int) cooldown);
// ... 执行技能效果 ...
```

---

## 快速重构清单

使用此清单确保没有遗漏步骤:

- [ ] 创建 `calculator/` 子包
  - [ ] `XxxDaoCooldownOps.java`
  - [ ] `XxxDaoDaohenOps.java`
- [ ] (可选) 创建 `fx/` 子包并移动特效
- [ ] (可选) 创建 `behavior/` 子包并移动行为
- [ ] 在 `ActivationHookRegistry.register()` 中:
  - [ ] 添加 `registerFamily("liupai_xxxdao")`
  - [ ] 添加 `registerFamily("daohen_xxxdao")`
  - [ ] 添加 `SkillEffectBus.register()` 快照注册
- [ ] 更新技能代码:
  - [ ] 使用 `XxxDaoCooldownOps.withXxxDaoExp()` 计算冷却
  - [ ] 使用 `XxxDaoDaohenOps.compute()` 获取道痕
- [ ] 编译测试
- [ ] 游戏内功能测试

---

## 常见问题

### Q1: 如果道没有对应的器官怎么办?

**A**: 在 `DaohenOps` 中暂时不注册任何provider,或者返回固定值:

```java
private FengDaoDaohenOps() {
    // 暂时没有器官,返回固定值用于测试
    registerProvider(cc -> 0.0);
}
```

### Q2: 技能ID不规则怎么办?

**A**: 使用更复杂的正则表达式:

```java
// 匹配多种模式
SkillEffectBus.register(
    "^guzhenren:(feng_.*|tornado_.*|wind_.*)$",
    // ...
);
```

### Q3: 需要不同的最低冷却时间怎么办?

**A**: 使用三参数版本:

```java
// 某些技能最低冷却2秒
long cooldown = FengDaoCooldownOps.withFengDaoExp(baseTicks, liupaiExp, 40L);
```

### Q4: 如何验证注册成功?

**A**: 在游戏启动日志中搜索 "registerFamily",或在代码中调用:

```java
boolean enabled = ActivationHookRegistry.isFamilyEnabled("liupai_fengdao");
System.out.println("风道流派已注册: " + enabled);
```

---

## 批量重构建议

### 优先级

1. **高优先级** (已部分注册):
   - shui_dao (水道)
   - yan_dao (炎道)

2. **中优先级** (常用五行道):
   - feng_dao (风道)
   - lei_dao (雷道)
   - tu_dao (土道)
   - mu_dao (木道)
   - jin_dao (金道)

3. **低优先级** (特殊/高级道):
   - 其余道按需重构

### 批量操作脚本建议

可以编写脚本自动生成 calculator 子包的模板代码,减少重复工作。

---

**文档版本**: v1.0
**最后更新**: 2025-01-12
**维护者**: ChestCavity Mod Team
