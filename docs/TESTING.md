# 测试框架使用指南

## 📚 测试框架概述

本项目采用分层测试策略，无需启动游戏即可完成大部分测试：

```
测试金字塔（从下到上）：
┌──────────────────────────────┐
│  E2E 游戏内测试（手动）     │  ← 最少，最慢
├──────────────────────────────┤
│  集成测试（Integration）     │  ← 中等数量
├──────────────────────────────┤
│  单元测试（Unit）            │  ← 最多，最快
└──────────────────────────────┘
```

---

## 🧪 测试类型

### 1. 单元测试（Unit Tests）

**目的**：测试单个类/方法的逻辑正确性

**位置**：`src/test/java/**/util/**Test.java`

**特点**：
- ✅ 速度快（毫秒级）
- ✅ 隔离性强（使用 Mock）
- ✅ 易于调试
- ✅ 覆盖边界情况

**示例**：
- `MultiCooldownTest.java` - 测试冷却系统
- `ResourceOpsTest.java` - 测试资源消耗
- `LedgerOpsTest.java` - 测试账本操作

**运行方式**：
```bash
# 运行所有单元测试
./gradlew test --tests "*Test"

# 运行特定测试类
./gradlew test --tests "MultiCooldownTest"

# 运行特定测试方法
./gradlew test --tests "MultiCooldownTest.testBasicCooldownSetAndCheck"
```

---

### 2. 集成测试（Integration Tests）

**目的**：测试多个组件协同工作的完整流程

**位置**：`src/test/java/**/integration/**IntegrationTest.java`

**特点**：
- ⚡ 中等速度
- 🔗 测试组件交互
- 📊 验证完整业务流程
- 🎯 模拟真实使用场景

**示例**：
- `OrganBehaviorIntegrationTest.java` - 测试器官完整生命周期

**运行方式**：
```bash
# 运行所有集成测试
./gradlew test --tests "*IntegrationTest"
```

---

### 3. 性能基准测试（Benchmarks）

**目的**：测量和对比性能指标

**位置**：`src/test/java/**/benchmark/**Benchmark.java`

**特点**：
- 📈 记录性能基准
- 🔄 对比重构前后差异
- ⚠️ 检测性能退化

**示例**：
- `PerformanceBenchmark.java` - 核心操作性能测试

**运行方式**：
```bash
# 运行性能基准测试
./gradlew test --tests "*Benchmark"

# 生成性能报告
./gradlew test --tests "PerformanceBenchmark.generatePerformanceReport"
```

**基准标准**：
| 操作 | 目标耗时 | 评估 |
|------|---------|------|
| MultiCooldown.isReady() | < 100 ns | 优秀 |
| MultiCooldown.set() | < 500 ns | 良好 |
| 资源消耗检查 | < 200 ns | 优秀 |
| 账本操作 | < 100 ns | 优秀 |
| 20个器官慢速Tick | < 50 µs | 优秀 |

---

### 4. 游戏内测试（Manual E2E）

**目的**：最终验证真实游戏环境中的表现

**何时使用**：
- ✅ 重大重构完成后
- ✅ 发布新版本前
- ✅ 单元/集成测试无法覆盖的场景（渲染、网络同步等）

**测试清单**：见 `docs/MANUAL_TEST_CHECKLIST.md`

---

## 🛠️ Mock 工具使用

### MockMinecraftEnvironment

用于创建模拟的 Minecraft 对象：

```java
// 创建模拟玩家
Player player = MockMinecraftEnvironment.createMockPlayer("TestPlayer");

// 创建模拟生物
LivingEntity entity = MockMinecraftEnvironment.createMockLivingEntity(EntityType.ZOMBIE);

// 推进游戏时间（用于测试冷却）
MockMinecraftEnvironment.advanceGameTime(100); // 100 ticks = 5 秒

// 测试结束后清理
@AfterEach
void tearDown() {
    MockMinecraftEnvironment.cleanup();
}
```

### MockChestCavityHelper

用于操作模拟的胸腔实例：

```java
// 创建胸腔实例
ChestCavityInstance cc = MockChestCavityHelper.createMockChestCavity(player);

// 添加器官
ItemStack organ = MockMinecraftEnvironment.createMockOrganStack(
    ResourceLocation.parse("guzhenren:bai_shi_gu"), 1
);
MockChestCavityHelper.addOrgan(cc, organ);

// 检查器官
boolean hasOrgan = MockChestCavityHelper.hasOrgan(cc,
    ResourceLocation.parse("guzhenren:bai_shi_gu"));

// 清空胸腔
MockChestCavityHelper.clearOrgans(cc);
```

---

## 📝 编写测试的最佳实践

### 1. 遵循 AAA 模式

```java
@Test
void testExample() {
    // Arrange（准备）：设置测试环境
    Player player = MockMinecraftEnvironment.createMockPlayer("Test");
    ChestCavityInstance cc = MockChestCavityHelper.createMockChestCavity(player);

    // Act（执行）：执行被测试的操作
    MultiCooldown.set(cc, "test_key", 100);

    // Assert（断言）：验证结果
    assertFalse(MultiCooldown.isReady(cc, "test_key"));
}
```

### 2. 使用描述性的测试名称

```java
// ✅ 好的命名
@Test
@DisplayName("资源不足时能力激活失败")
void testAbilityFailsWithInsufficientResources() { }

// ❌ 差的命名
@Test
void test1() { }
```

### 3. 每个测试只验证一件事

```java
// ✅ 好的做法
@Test
void testCooldownSetCorrectly() {
    MultiCooldown.set(cc, "key", 100);
    assertFalse(MultiCooldown.isReady(cc, "key"));
}

@Test
void testCooldownExpiresCorrectly() {
    MultiCooldown.set(cc, "key", 100);
    MockMinecraftEnvironment.advanceGameTime(100);
    assertTrue(MultiCooldown.isReady(cc, "key"));
}

// ❌ 差的做法（一个测试做太多事）
@Test
void testEverything() {
    // 测试设置、检查、过期、清除...
}
```

### 4. 清理测试环境

```java
@BeforeEach
void setUp() {
    // 每个测试前重置
    MockMinecraftEnvironment.resetGameTime();
    mockPlayer = MockMinecraftEnvironment.createMockPlayer("Test");
}

@AfterEach
void tearDown() {
    // 每个测试后清理
    MockMinecraftEnvironment.cleanup();
}
```

---

## 🚀 快速开始

### 运行所有测试

```bash
./gradlew test
```

### 运行并生成覆盖率报告

```bash
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html
```

### 持续测试（文件改动时自动运行）

```bash
./gradlew test --continuous
```

### 只运行失败的测试

```bash
./gradlew test --rerun-tasks
```

---

## 📊 测试覆盖率目标

| 组件 | 目标覆盖率 | 当前状态 |
|------|-----------|---------|
| MultiCooldown | 80%+ | ⏳ 待测 |
| ResourceOps | 80%+ | ⏳ 待测 |
| LedgerOps | 80%+ | ⏳ 待测 |
| DoTEngine | 70%+ | ⏳ 待测 |
| 器官行为 | 60%+ | ⏳ 待测 |

---

## 🔧 故障排查

### 问题：Mockito 无法 mock final 类

**解决方案**：已配置 `mockito-inline`，支持 mock final 类。

如果仍有问题，检查：
```bash
# 确认依赖存在
./gradlew dependencies | grep mockito
```

### 问题：测试无法找到 Minecraft 类

**解决方案**：确保测试类路径正确：

```gradle
// build.gradle 中已配置
test {
    useJUnitPlatform()
    classpath += sourceSets.main.runtimeClasspath
}
```

### 问题：性能测试结果不稳定

**解决方案**：
1. 增加预热迭代次数
2. 运行多次取平均值
3. 关闭其他程序减少干扰

---

## 📚 参考资料

- [JUnit 5 官方文档](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 官方文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [NeoForge 测试指南](https://docs.neoforged.net/docs/misc/gametest/)

---

## ✅ 测试清单（重构时使用）

每次重构完成后，确保：

- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 性能基准无退化
- [ ] 代码覆盖率达标
- [ ] 手动游戏内测试通过（关键功能）

---

**最后更新**：2025-01-28
**维护者**：ChestCavityForge 开发团队
