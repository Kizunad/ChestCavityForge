# 测试框架总结

## ✅ 已完成的工作

### 1. 测试框架搭建 ✓

**目录结构**：
```
src/test/java/
├── net/tigereye/chestcavity/
│   ├── examples/
│   │   └── SimpleTestExample.java      # 简单测试示例（可运行）
│   └── util/mock/
│       ├── MockMinecraftEnvironment.java  # Minecraft 环境 Mock 工具
│       └── MockChestCavityHelper.java     # ChestCavity Mock 助手
```

**依赖配置**：
- ✅ JUnit 5 (jupiter-api, jupiter-engine)
- ✅ Mockito (mockito-core, mockito-inline)
- ✅ 所有依赖已在 `build.gradle` 中配置

### 2. 文档创建 ✓

- ✅ `docs/TESTING.md` - 完整的测试框架使用指南
- ✅ `docs/HOW_TO_WRITE_TESTS.md` - 如何编写测试的教程
- ✅ `docs/MANUAL_TEST_CHECKLIST.md` - 游戏内手动测试清单
- ✅ `scripts/run-tests.sh` - 测试运行脚本（可执行）

### 3. 测试示例 ✓

已创建可运行的测试示例：`SimpleTestExample`

运行命令：
```bash
./gradlew test --tests "SimpleTestExample"
```

结果：✅ **BUILD SUCCESSFUL** - 所有测试通过

---

## ⚠️ 限制与注意事项

### 1. Minecraft 核心类无法 Mock

**问题**：Mockito 无法 mock 以下类：
- `net.minecraft.world.entity.player.Player`
- `net.minecraft.server.level.ServerLevel`
- 其他 Minecraft 核心类

**原因**：
- 这些类是 final 或有特殊的类加载器
- 即使使用 `mockito-inline` 也无法绕过

**解决方案**：

#### 方案 1：测试纯逻辑代码（推荐）
只测试不依赖 Minecraft 类的工具方法和逻辑：

```java
// ✅ 可测试
public class MathHelper {
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

@Test
void testClamp() {
    assertEquals(10, MathHelper.clamp(15, 0, 10));
}
```

#### 方案 2：创建可测试的包装层
将业务逻辑从 Minecraft 类中抽离：

```java
// ❌ 难以测试
public class OrganBehavior implements OrganSlowTickListener {
    @Override
    public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
        Player player = (Player) cc.owner;
        float health = player.getHealth(); // 依赖 Minecraft 类
        // ... 复杂逻辑
    }
}

// ✅ 易于测试
public class OrganLogic {
    public static float calculateValue(float health, float baseValue) {
        // 纯逻辑，不依赖 Minecraft 类
        return health > 10 ? baseValue * 1.5f : baseValue;
    }
}

public class OrganBehavior implements OrganSlowTickListener {
    @Override
    public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
        Player player = (Player) cc.owner;
        float result = OrganLogic.calculateValue(player.getHealth(), 100);
        // ... 使用 result
    }
}

// 测试
@Test
void testCalculateValue() {
    assertEquals(150f, OrganLogic.calculateValue(15f, 100f));
    assertEquals(100f, OrganLogic.calculateValue(5f, 100f));
}
```

#### 方案 3：使用 NeoForge GameTest 框架
对于必须在游戏环境中测试的功能，使用 NeoForge 提供的 GameTest：

```java
// 需要额外配置和学习 GameTest API
@GameTest
public void testOrganInGame(GameTestHelper helper) {
    // 在真实游戏环境中测试
}
```

参考：https://docs.neoforged.net/docs/misc/gametest/

---

## 📖 使用指南

### 快速开始

1. **查看示例**：
   ```bash
   cat src/test/java/net/tigereye/chestcavity/examples/SimpleTestExample.java
   ```

2. **运行示例测试**：
   ```bash
   ./gradlew test --tests "SimpleTestExample"
   ```

3. **阅读文档**：
   - 基础教程：`docs/HOW_TO_WRITE_TESTS.md`
   - 完整指南：`docs/TESTING.md`

### 编写你的第一个测试

1. 选择一个简单的工具方法（如 `ChestCavityUtil` 中的静态方法）
2. 创建测试类：`src/test/java/.../XXXTest.java`
3. 编写测试：
   ```java
   @Test
   void testSomeMethod() {
       int result = SomeUtil.someMethod(5);
       assertEquals(10, result);
   }
   ```
4. 运行测试：
   ```bash
   ./gradlew test --tests "XXXTest"
   ```

---

## 🎯 测试策略建议

### 优先级 1：工具类和纯逻辑

**适合测试**：
- 数学计算
- 字符串处理
- 数据转换
- 条件判断逻辑

**示例**：
- `MultiCooldown` 的计算逻辑
- `ResourceOps` 的资源计算
- 各种 Helper 类的静态方法

### 优先级 2：业务逻辑抽离

**重构策略**：
1. 识别复杂的业务逻辑
2. 将逻辑抽取为纯函数
3. 对纯函数编写单元测试
4. 原有代码调用这些纯函数

### 优先级 3：集成测试（游戏内）

**使用场景**：
- 器官装备/卸下流程
- 玩家交互
- 实体行为
- 渲染效果

**方法**：
- 手动测试（使用 `docs/MANUAL_TEST_CHECKLIST.md`）
- NeoForge GameTest（需额外配置）

---

## 📊 当前测试覆盖率

| 组件 | 可测试性 | 优先级 |
|------|---------|--------|
| 工具类 (Utils) | ✅ 高 | 🔥 高 |
| 逻辑计算 | ✅ 高 | 🔥 高 |
| 数据结构 | ✅ 中 | 🟡 中 |
| 器官行为 | ⚠️ 低 | 🔵 低 |
| UI/渲染 | ❌ 很低 | 🔵 低 |

**建议**：先从高优先级、高可测试性的组件开始。

---

## 🔧 测试工具

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "SimpleTestExample"

# 持续监听模式
./gradlew test --continuous

# 生成覆盖率报告
./gradlew test jacocoTestReport
```

### 测试脚本

已创建便捷脚本 `scripts/run-tests.sh`：

```bash
# 运行所有测试
./scripts/run-tests.sh all

# 仅运行单元测试
./scripts/run-tests.sh unit

# 生成覆盖率报告
./scripts/run-tests.sh coverage

# 快速检查（编译 + 静态分析 + 快速测试）
./scripts/run-tests.sh quick
```

---

## 📝 下一步建议

### 短期（1-2周）
1. ✅ 为重构计划中的核心工具类编写测试：
   - `MultiCooldown` 的 Entry/EntryInt 逻辑
   - `OrganState` 的数据操作
   - 各种计算 Helper

2. ✅ 重构时遵循"测试先行"：
   - 先抽离纯逻辑
   - 为纯逻辑写测试
   - 再进行重构
   - 测试保证不破坏功能

### 中期（1-2月）
1. 逐步提升测试覆盖率到 60%+（针对可测试代码）
2. 建立 CI/CD 流程，自动运行测试
3. 考虑引入 GameTest 进行集成测试

### 长期
1. 持续重构，提升代码可测试性
2. 维护测试用例与代码同步更新
3. 建立性能基准测试

---

## ❓ FAQ

### Q: 为什么不能 mock Player/ServerLevel？
A: Minecraft 核心类有特殊限制，Mockito 无法处理。解决方案是抽离业务逻辑为纯函数。

### Q: 没有测试是否可以进行重构？
A: 可以，但风险更高。建议先为关键逻辑写测试，再重构。

### Q: 测试覆盖率目标是多少？
A: 对于可测试代码，目标是 60-80%。UI/渲染代码不强求。

### Q: 如何测试依赖 Minecraft 的代码？
A:
1. 优先抽离逻辑为纯函数
2. 使用接口隔离依赖
3. 必要时使用 GameTest

---

**总结**：测试框架已搭建完成，可以开始编写单元测试。建议从简单的工具类开始，逐步积累经验。

---

**最后更新**：2025-01-28
**状态**：✅ 框架可用，示例测试通过

---

## 冰雪道（去 Ledger 化）

### 冷却三点校验
- **exp=0**: 技能冷却时间应为基础值。
- **exp=5000**: 技能冷却时间应接近 `base - (base - 20) * 0.5`。
- **exp=10001**: 技能冷却时间应为下限 `20t`。

### 乘区三点校验
- **daohen=0**: 技能伤害/效果强度应为基础值。
- **daohen=0.25**: 技能伤害/效果强度应为基础值的 `1.25` 倍。
- **daohen=0.5**: 技能伤害/效果强度应为基础值的 `1.5` 倍。

### 被动行为
- **效果验证**: 确认清冷、抗冻、粉雪行走、再生与免疫等效果正常触发，且相关提示与粒子效果不会刷屏。
- **执行环境**: 确认所有被动逻辑仅在服务端执行。

### 非玩家实体
- **兼容性**: 确认被动效果对非玩家`LivingEntity`（如怪物）同样生效。
- **主动触发**: 在必要时，非玩家实体应能由其被动逻辑触发相应的主动技能效果。

### 回归检查
- **状态残留**: 装备或卸下冰雪道器官后，不应有任何状态（如冷却、属性加成）残留。
- **Ledger调用**: 确认 `bing_xue_dao` 相关的代码中已无对 `Ledger` 系统的任何调用。
- **日志监控**: 检查游戏日志，确保无负冷却时间或账本重建相关的警告或错误。
