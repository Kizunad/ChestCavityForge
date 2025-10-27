# 如何为 ChestCavity 编写测试

## 🎯 测试策略概览

由于 ChestCavity 项目的 API 已经存在且相对复杂，测试策略分为三个层次：

1. **工具类测试**：测试独立的工具方法（最简单）
2. **逻辑单元测试**：测试单个类的核心逻辑
3. **集成测试**：测试多个组件的协同工作

---

## 📚 当前可用的测试工具

### 1. Mock 工具

已提供的 Mock 助手位于 `src/test/java/net/tigereye/chestcavity/util/mock/`：

- `MockMinecraftEnvironment` - 创建模拟的 Minecraft 对象
- `MockChestCavityHelper` - 操作模拟的胸腔实例

### 2. 测试框架

- **JUnit 5** - 测试框架
- **Mockito** - Mock 框架（支持 mock final 类）
- **断言库** - JUnit 内置的 `Assertions`

---

## 🚀 快速开始：编写第一个测试

### 步骤 1：创建测试类

测试类应该位于 `src/test/java/` 下，包路径与被测试类相同。

```java
package net.tigereye.chestcavity.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChestCavityUtil 测试")
class ChestCavityUtilTest {
    // 测试方法将在这里
}
```

### 步骤 2：编写测试方法

使用 AAA 模式（Arrange-Act-Assert）：

```java
@Test
@DisplayName("测试某个功能")
void testSomeFeature() {
    // Arrange（准备）：设置测试数据
    int input = 5;

    // Act（执行）：调用被测试的方法
    int result = someMethod(input);

    // Assert（断言）：验证结果
    assertEquals(10, result, "结果应该是输入的两倍");
}
```

### 步骤 3：运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "ChestCavityUtilTest"

# 运行特定测试方法
./gradlew test --tests "ChestCavityUtilTest.testSomeFeature"
```

---

## 📖 实际示例：测试现有代码

### 示例 1：测试工具方法

假设你有一个工具方法需要测试：

```java
// 被测试的代码（在 src/main/java 中）
public class MathHelper {
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
```

测试代码：

```java
// 测试代码（在 src/test/java 中）
package net.tigereye.chestcavity.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MathHelper 测试")
class MathHelperTest {

    @Test
    @DisplayName("clamp 应该限制值在范围内")
    void testClamp() {
        assertEquals(5, MathHelper.clamp(5, 0, 10), "值在范围内应保持不变");
        assertEquals(0, MathHelper.clamp(-5, 0, 10), "小于最小值应返回最小值");
        assertEquals(10, MathHelper.clamp(15, 0, 10), "大于最大值应返回最大值");
    }

    @Test
    @DisplayName("边界条件测试")
    void testClampBoundaries() {
        assertEquals(0, MathHelper.clamp(0, 0, 10), "等于最小值");
        assertEquals(10, MathHelper.clamp(10, 0, 10), "等于最大值");
    }
}
```

---

### 示例 2：使用 Mock 测试依赖注入

```java
package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.util.mock.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("器官状态测试")
class OrganStateTest {

    private Player mockPlayer;
    private ChestCavityInstance mockCC;

    @BeforeEach
    void setUp() {
        // 每个测试前准备 Mock 对象
        mockPlayer = MockMinecraftEnvironment.createMockPlayer("TestPlayer");
        mockCC = MockChestCavityHelper.createMockChestCavity(mockPlayer);
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理
        MockMinecraftEnvironment.cleanup();
    }

    @Test
    @DisplayName("测试持久化数据读写")
    void testPersistentData() {
        // Arrange
        var data = MockChestCavityHelper.getPersistentData(mockCC);

        // Act
        data.putFloat("test_key", 123.45f);
        float retrieved = data.getFloat("test_key");

        // Assert
        assertEquals(123.45f, retrieved, 0.001f, "数据应正确存储和读取");
    }
}
```

---

### 示例 3：测试 MultiCooldown（真实 API）

基于实际的 MultiCooldown API：

```java
package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultiCooldown 测试")
class MultiCooldownRealTest {

    @Test
    @DisplayName("Entry 基础功能测试")
    void testEntryBasics() {
        // Arrange: 创建一个测试用的 organ state
        ItemStack organ = new ItemStack(Items.DIAMOND);
        OrganState state = OrganState.of(organ, "test_root");

        MultiCooldown cooldown = MultiCooldown.builder(state)
            .withOrgan(organ)
            .build();

        // Act: 获取一个 entry 并设置冷却
        MultiCooldown.Entry entry = cooldown.entry("test_key");
        long currentTime = 100;
        entry.setReadyAt(currentTime + 50);

        // Assert: 验证冷却状态
        assertFalse(entry.isReady(currentTime), "应该还在冷却中");
        assertEquals(50, entry.remaining(currentTime), "剩余时间应为 50");

        assertTrue(entry.isReady(currentTime + 50), "时间到期后应该就绪");
        assertEquals(0, entry.remaining(currentTime + 50), "到期后剩余时间应为 0");
    }

    @Test
    @DisplayName("EntryInt 倒计时测试")
    void testEntryIntCountdown() {
        // Arrange
        ItemStack organ = new ItemStack(Items.DIAMOND);
        OrganState state = OrganState.of(organ, "test_root");

        MultiCooldown cooldown = MultiCooldown.builder(state).build();

        // Act
        MultiCooldown.EntryInt countdown = cooldown.entryInt("countdown_key");
        countdown.setTicks(10);

        // Assert
        assertEquals(10, countdown.getTicks(), "初始值应为 10");
        assertFalse(countdown.isReady(), "倒计时未结束应返回 false");

        // 模拟 tick down
        for (int i = 0; i < 10; i++) {
            assertTrue(countdown.tickDown(), "应该成功递减");
        }

        assertEquals(0, countdown.getTicks(), "倒计时结束后应为 0");
        assertTrue(countdown.isReady(), "倒计时结束应返回 true");
        assertFalse(countdown.tickDown(), "已结束不应再递减");
    }
}
```

---

## 🛠️ 测试最佳实践

### 1. 命名规范

```java
// ✅ 好的命名
@Test
@DisplayName("冷却时间到期后应该就绪")
void testCooldownReadyAfterExpiry() { }

// ❌ 差的命名
@Test
void test1() { }
```

### 2. 一个测试只验证一件事

```java
// ✅ 好的做法
@Test
void testSetReadyAt_ShouldUpdateCooldown() {
    entry.setReadyAt(100);
    assertEquals(100, entry.getReadyTick());
}

@Test
void testIsReady_ReturnsFalseWhenNotExpired() {
    entry.setReadyAt(100);
    assertFalse(entry.isReady(50));
}

// ❌ 差的做法（一个测试做太多事）
@Test
void testEverything() {
    // 测试设置、读取、到期、清除...
}
```

### 3. 使用 setUp 和 tearDown

```java
@BeforeEach
void setUp() {
    // 每个测试前的准备工作
}

@AfterEach
void tearDown() {
    // 每个测试后的清理工作
    MockMinecraftEnvironment.cleanup();
}
```

### 4. 使用参数化测试

对于多个输入的相同逻辑：

```java
@ParameterizedTest
@ValueSource(ints = {-5, 0, 5, 10, 15})
@DisplayName("测试不同输入值")
void testWithDifferentValues(int value) {
    int result = MathHelper.clamp(value, 0, 10);
    assertTrue(result >= 0 && result <= 10);
}
```

---

## 🔍 如何测试复杂的器官行为

### 步骤 1：理解被测试代码的API

先阅读源代码，了解实际的类结构和方法签名。

### 步骤 2：识别可测试的单元

不要一次测试整个系统，找出可独立测试的小部分：

- 数据转换方法
- 计算逻辑
- 状态管理
- 条件判断

### 步骤 3：编写最小测试

从最简单的场景开始：

```java
@Test
void testBasicFunctionality() {
    // 最简单的输入
    // 最直接的验证
}
```

### 步骤 4：逐步增加复杂性

```java
@Test
void testEdgeCases() {
    // 边界情况
}

@Test
void testErrorHandling() {
    // 异常处理
}
```

---

## ⚠️ 常见陷阱

### 1. 不要假设 API

❌ **错误**：编写测试时假设方法签名
```java
MultiCooldown.set(cc, "key", 100); // 这个 API 可能不存在！
```

✅ **正确**：先查看实际代码，使用真实的 API
```java
MultiCooldown cooldown = MultiCooldown.builder(state).build();
cooldown.entry("key").setReadyAt(100);
```

### 2. 不要过度 Mock

只 Mock 真正需要的部分，尽量使用真实对象。

### 3. 测试应该快速

避免在测试中：
- 启动整个游戏
- 等待实际时间流逝
- 进行文件I/O（除非必要）

---

## 📊 测试覆盖率

运行测试并生成覆盖率报告：

```bash
./gradlew test jacocoTestReport
```

报告位置：`build/reports/jacoco/test/html/index.html`

---

## 🎯 下一步

1. **查看示例测试**：`src/test/java/net/tigereye/chestcavity/examples/SimpleTestExample.java`
2. **选择一个简单的工具方法**：从项目中找一个简单的静态方法练手
3. **编写你的第一个测试**：按照本指南的步骤操作
4. **运行测试**：确保测试通过

---

**记住**：测试的目的是增加信心，而不是增加负担。从简单开始，逐步改进！

---

**最后更新**：2025-01-28
