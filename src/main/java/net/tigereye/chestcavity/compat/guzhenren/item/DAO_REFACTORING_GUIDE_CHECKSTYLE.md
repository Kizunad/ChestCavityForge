# 道系统标准化重构指南 (Checkstyle 规范版)

本指南提供了符合项目 Checkstyle 规范(基于 Google Java Style)的道系统重构流程。

---

## 📋 重要规范

### Checkstyle 要求

本项目使用 **Google Java Style** (checkstyle 10.20.0),关键规范:

#### 1. 代码格式
- ✅ **缩进**: 2个空格(不是Tab)
- ✅ **行长度**: 最大100字符
- ✅ **大括号**: K&R风格 (左括号不换行)
- ✅ **空行**: 类/方法/字段之间需要空行分隔

#### 2. 命名规范
- ✅ **类名**: PascalCase (例如: `FengDaoCooldownOps`)
- ✅ **方法名**: camelCase, 至少3个字符 (例如: `calculateCooldown`)
- ✅ **参数名**: camelCase, 至少2个字符 (例如: `cc`, `baseTicks`)
- ✅ **常量**: UPPER_SNAKE_CASE (例如: `MAX_LIUPAI_EXP`)
- ✅ **包名**: 全小写 (例如: `net.tigereye.chestcavity.compat.guzhenren.util`)

#### 3. Javadoc 规范
- ✅ **类文档**: 所有 protected/public 类必须有 Javadoc
- ✅ **方法文档**: 所有 protected/public 方法必须有 Javadoc
- ✅ **第一句**: 必须以句号(。)结尾,这是summary
- ✅ **标签顺序**: `@param` → `@return` → `@throws` → `@deprecated`
- ✅ **代码示例**: 使用 `{@code ...}` 或 ` <pre>{@code ... }</pre>` 包裹

**Javadoc 模板**:
```java
/**
 * 类的简短描述(一句话,以句号结尾)。
 *
 * <p>详细描述第一段。
 *
 * <p>详细描述第二段(如果需要)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 示例代码
 * FengDaoCooldownOps.calculateCooldown(200L, 5000);
 * }</pre>
 *
 * @param paramName 参数描述
 * @return 返回值描述
 */
```

#### 4. Import 顺序
分4组,组间空行分隔:
1. `java.*` 和 `javax.*`
2. `net.minecraft.*`, `net.neoforged.*`, `net.tigereye.chestcavity.*`
3. 第三方库
4. `static` imports (最后)

**正确示例**:
```java
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import com.google.common.collect.ImmutableList;
```

#### 5. 常见错误

❌ **错误示例**:
```java
// 错误1: Javadoc第一句没有句号
/**
 * 计算冷却时间
 */

// 错误2: 行太长(>100字符)
public static long calculateCooldownWithVeryLongParameterNamesAndDescriptionThatExceedsLimit(...) {

// 错误3: import顺序错误
import net.minecraft.world.entity.Player;
import java.util.List;  // 应该在上面

// 错误4: 使用Tab缩进
public class Foo {
→ public void bar() {  // 应该用2个空格
}
```

✅ **正确示例**:
```java
/**
 * 计算冷却时间。
 */

// 正确: 超长行换行
public static long calculateCooldownWithVeryLongParameters(
    long baseTicks,
    int liupaiExp) {

// 正确: import顺序
import java.util.List;

import net.minecraft.world.entity.Player;

// 正确: 2空格缩进
public class Foo {
  public void bar() {
    // ...
  }
}
```

---

## 📋 目录

1. [准备工作](#准备工作)
2. [标准目录结构](#标准目录结构)
3. [步骤1: 创建calculator子包](#步骤1-创建calculator子包)
4. [步骤2: 重构fx特效](#步骤2-重构fx特效可选)
5. [步骤3: 重构behavior行为](#步骤3-重构behavior行为可选)
6. [步骤4: 注册到ActivationHookRegistry](#步骤4-注册到activationhookregistry)
7. [步骤5: 更新技能冷却逻辑](#步骤5-更新技能冷却逻辑)
8. [步骤6: Checkstyle验证](#步骤6-checkstyle验证)
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
- **技能ID前缀**: `guzhenren:feng_*`

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
 * 风道冷却时间计算工具类。
 *
 * <p>基于风道流派经验(liupai_fengdao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 */
public final class FengDaoCooldownOps {

  private FengDaoCooldownOps() {}

  /**
   * 根据风道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp);
  }

  /**
   * 根据风道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withFengDaoExp(
      long baseTicks,
      int liupaiFengdaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiFengdaoExp,
        minTicks);
  }
}
```

**关键点**:
- ✅ 类注释第一句以句号结尾
- ✅ 所有public方法都有完整Javadoc
- ✅ 超过100字符的行正确换行
- ✅ 使用2空格缩进

### 1.2 创建道痕计算类

**文件**: `feng_dao/calculator/FengDaoDaohenOps.java`

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;
import net.tigereye.chestcavity.registration.CCOrganScores;

/**
 * 风道道痕计算工具类。
 *
 * <p>汇总风道相关器官的道痕值,用于技能效果增幅计算。
 */
public final class FengDaoDaohenOps extends DaohenCalculator {

  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  private FengDaoDaohenOps() {
    // 注册风道相关器官的道痕提供器
    // 示例: 假设有一个风系器官,每个提供1.0道痕
    registerProvider(cc ->
        calculateDaohen(
            cc.getOrganScore(CCOrganScores.FENG_ORGAN),
            1.0));

    // 可以注册多个器官
    // registerProvider(cc ->
    //     calculateDaohen(
    //         cc.getOrganScore(CCOrganScores.FENG_ELITE_ORGAN),
    //         2.5));
  }

  /**
   * 计算风道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double compute(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
```

**关键点**:
- ✅ Import按顺序分组(本项目import, 第三方库)
- ✅ Lambda表达式正确换行
- ✅ 注释的代码保持正确缩进

---

## 步骤2: 重构fx特效(可选)

创建 `fx/` 子包并移动特效类。

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

创建 `behavior/` 子包并移动技能行为逻辑。

**命名规范**: `[技能名]Behavior.java`

---

## 步骤4: 注册到ActivationHookRegistry

在 `ActivationHookRegistry.register()` 方法中添加:

```java
// 注册风道流派和道痕家族
registerFamily("liupai_fengdao");
registerFamily("daohen_fengdao");

// 技能效果: 风道技能需要快照道痕与流派经验
SkillEffectBus.register(
    "^guzhenren:feng_.*$",
    CompositeEffect.of(
        new ResourceFieldSnapshotEffect(
            "fengdao:",
            List.of("daohen_fengdao", "liupai_fengdao"))));
```

---

## 步骤5: 更新技能冷却逻辑

在技能激活方法中:

```java
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.FengDaoCooldownOps;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.FengDaoDaohenOps;

// 读取流派经验
int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);
double daohen = FengDaoDaohenOps.compute(cc);

// 计算冷却
long baseCooldown = 200L; // 10秒
long actualCooldown = FengDaoCooldownOps.withFengDaoExp(
    baseCooldown,
    liupaiExp);

// 应用冷却
player.getCooldowns().addCooldown(item, (int) actualCooldown);

// 应用道痕加成到伤害
float baseDamage = 10.0f;
float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
```

---

## 步骤6: Checkstyle验证

### 6.1 运行Checkstyle检查

```bash
./gradlew checkstyleMain
```

### 6.2 常见Checkstyle错误及修复

#### 错误1: SummaryJavadoc
```
First sentence of Javadoc is missing an ending period.
```

**修复**: Javadoc第一句必须以句号结尾
```java
// ❌ 错误
/**
 * 计算冷却时间
 */

// ✅ 正确
/**
 * 计算冷却时间。
 */
```

#### 错误2: LineLength
```
Line is longer than 100 characters.
```

**修复**: 超长行换行
```java
// ❌ 错误
public static long calculateCooldownWithVeryLongMethodNameAndParameters(long baseTicks, int exp) {

// ✅ 正确
public static long calculateCooldownWithVeryLongMethodName(
    long baseTicks,
    int exp) {
```

#### 错误3: CustomImportOrder
```
Import statement is in the wrong order.
```

**修复**: 调整import顺序
```java
// ❌ 错误
import net.minecraft.world.entity.Player;
import java.util.List;

// ✅ 正确
import java.util.List;

import net.minecraft.world.entity.Player;
```

#### 错误4: MissingJavadocMethod
```
Missing a Javadoc comment.
```

**修复**: 为public/protected方法添加Javadoc
```java
// ❌ 错误
public void doSomething() {
}

// ✅ 正确
/**
 * 执行某操作。
 */
public void doSomething() {
}
```

### 6.3 临时抑制Checkstyle警告

如果某些警告无法立即修复,可以临时抑制:

```java
// 抑制单行
// CHECKSTYLE.SUPPRESS: LineLength
public static long veryLongMethodNameThatExceedsLimitButCannotBeChanged(...) {

// 抑制多行
// CHECKSTYLE.OFF: MagicNumber
public static final int SOME_VALUE = 12345;
public static final int ANOTHER_VALUE = 67890;
// CHECKSTYLE.ON: MagicNumber

// 使用注解抑制
@SuppressWarnings("checkstyle:MagicNumber")
public void method() {
  int value = 12345;
}
```

### 6.4 编译检查

确保代码可以编译:

```bash
./gradlew compileJava
```

---

## 完整示例: 风道(FengDao)

### FengDaoCooldownOps.java (完整)

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 风道冷却时间计算工具类。
 *
 * <p>基于风道流派经验(liupai_fengdao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);
 * long cooldown = FengDaoCooldownOps.withFengDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class FengDaoCooldownOps {

  private FengDaoCooldownOps() {}

  /**
   * 根据风道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp);
  }

  /**
   * 根据风道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withFengDaoExp(
      long baseTicks,
      int liupaiFengdaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiFengdaoExp,
        minTicks);
  }
}
```

### FengDaoDaohenOps.java (完整)

```java
package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;
import net.tigereye.chestcavity.registration.CCOrganScores;

/**
 * 风道道痕计算工具类。
 *
 * <p>汇总风道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = FengDaoDaohenOps.compute(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class FengDaoDaohenOps extends DaohenCalculator {

  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  private FengDaoDaohenOps() {
    // 注册风道相关器官的道痕提供器
    registerProvider(cc ->
        calculateDaohen(
            cc.getOrganScore(CCOrganScores.FENG_ORGAN),
            1.0));
  }

  /**
   * 计算风道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double compute(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
```

---

## 快速重构清单

- [ ] 创建 `calculator/` 子包
  - [ ] `XxxDaoCooldownOps.java`
  - [ ] `XxxDaoDaohenOps.java`
- [ ] (可选) 创建 `fx/` 子包
- [ ] (可选) 创建 `behavior/` 子包
- [ ] 在 `ActivationHookRegistry.register()` 中注册
- [ ] 更新技能代码使用新的计算方法
- [ ] **运行 `./gradlew checkstyleMain`**
- [ ] **修复所有Checkstyle警告**
- [ ] **运行 `./gradlew compileJava`**
- [ ] 游戏内功能测试

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

3. **低优先级**:
   - 其余15个道

---

**文档版本**: v2.0 (Checkstyle规范版)
**最后更新**: 2025-01-12
**维护者**: ChestCavity Mod Team
