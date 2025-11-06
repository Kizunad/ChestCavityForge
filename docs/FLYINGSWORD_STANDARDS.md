# 飞剑模块重构｜规范文档（Standards）

## 1. 代码风格与命名
- Java 风格统一：与仓库现有 Checkstyle/Spotless 保持一致（若存在）。
- 命名：
  - 事件类/上下文：`*Context`、`*Hook`、`*Registry`；前置事件动词为 `onXxx`。
  - 系统类：`XxxSystem`（Movement/Combat/Defense/BlockBreak/Targeting/Progression/Lifecycle）。
  - 配置常量：`FlyingSwordTuning`；布尔开关以 `ENABLE_*` 约定。
  - MultiCooldown Key：`cc:flying_sword/<uuid>/<domain>`。

## 2. 目录结构
```
compat/guzhenren/flyingsword/
  api/  core/  systems/  events/  integration/  ai/  client/  tuning/
  ops/  calculator/  ui/
```
- api：对外稳定入口；core：实体与属性；systems：服务端系统；events：上下文与注册；integration：资源/冷却；ai：意图与轨迹；client：渲染；tuning：常量与配置。

## 3. 事件约定
- 前置事件可取消/改参；后置事件只读。
- Fire 顺序按注册顺序；短路遵守 `ctx.cancelled/cancelDefault/preventDespawn`。
- 默认钩子实现不得假设其他钩子存在；失败需捕获异常、降噪。

## 4. 资源与冷却

### Phase 4: 冷却管理规范

**统一冷却存储**：
- 所有飞剑冷却统一存储在 owner 的 `MultiCooldown` 附件中
- 使用 `FlyingSwordCooldownOps` 操作冷却，屏蔽附件获取细节
- Key 命名规范：`cc:flying_sword/<sword_uuid>/<domain>`
  - 攻击冷却：`cc:flying_sword/<sword_uuid>/attack`
  - 破块冷却：`cc:flying_sword/<sword_uuid>/block_break`
  - 能力冷却：`cc:flying_sword/<sword_uuid>/ability`

**禁止事项**：
- ❌ 不得在实体类中维护新的"裸"整型冷却字段（如 `attackCooldown2` 等）
- ❌ 不得绕过 `FlyingSwordCooldownOps` 直接操作 `MultiCooldown`
- ❌ 不得在冷却路径中引入阻塞 I/O 或昂贵的日志输出

**使用示例**：
```java
// 获取攻击冷却
int cooldown = FlyingSwordCooldownOps.getAttackCooldown(sword);

// 设置攻击冷却
FlyingSwordCooldownOps.setAttackCooldown(sword, 10);

// 检查是否就绪
if (FlyingSwordCooldownOps.isAttackReady(sword)) {
  // 执行攻击
}

// 每 tick 递减
FlyingSwordCooldownOps.tickDownAttackCooldown(sword);
```

### 资源消耗规范

**统一入口**：
- 维持消耗只在 `UpkeepSystem` 中调用 `ResourceOps`，并触发 `OnUpkeepCheck`
- 禁止绕过 `ResourceOps` 直接消耗/恢复主人资源

**维持失败策略**（Phase 4）：
- `RECALL`: 召回到物品栏（默认，兼容旧版）
- `STALL`: 停滞不动，速度冻结为 0
- `SLOW`: 减速移动，速度降低为配置的倍率

配置位置：`FlyingSwordTuning.UPKEEP_FAILURE_STRATEGY`

## 5. Git 提交规范
- 类型建议：feat / fix / refactor / docs / test / chore
- 范围：`[flyingsword]` 前缀；一句话概述 + 必要说明；涉及开关/事件需在描述中标注。

## 6. 文档编写
- 需求/框架/红线/规范/实施/阶段/测试文档置于 `docs/`；阶段文档置于 `docs/stages/`。
- Mermaid 图用于流程/序列说明；避免大段代码示例。

## 7. 测试约定

### Phase 6: 测试规范

**单元测试要求**：
1. 所有纯函数必须有单元测试
2. 测试覆盖率目标 ≥ 70%（整体），核心计算器 ≥ 80%
3. 每个测试方法只测试一个功能点
4. 使用 JUnit 5 和 assertions
5. 测试应该是确定性的（多次运行结果相同）

**测试组织**：
- **Calculator 测试**：测试所有计算方法
  - 位置：`test/java/.../flyingsword/calculator/`
  - 覆盖：伤害、维持、经验、耐久、工具方法
  - 示例：`FlyingSwordCalculatorTest.java`

- **Integration 测试**：测试资源消耗、冷却管理
  - 位置：`test/java/.../flyingsword/integration/`
  - 覆盖：UpkeepOps、CooldownOps、ResourceOps
  - 示例：`UpkeepOpsTest.java`

- **Motion 测试**：测试运动学计算
  - 位置：`test/java/.../flyingsword/motion/`
  - 覆盖：KinematicsOps、SteeringOps（如果可测）
  - 示例：`KinematicsOpsTest.java`

- **Systems 测试**：测试系统集成（如果环境允许）
  - 位置：`test/java/.../flyingsword/systems/`
  - 覆盖：CombatSystem、UpkeepSystem、MovementSystem
  - 优先级：低（需要 Minecraft 环境）

**测试命名规范**：
- 测试类：`<ClassName>Test.java`
- 测试方法：`test<MethodName>_<Scenario>`
- 例如：
  - `testCalculateDamage_WithHighSpeed()`
  - `testCalculateUpkeep_OrbitMode()`
  - `testComputeIntervalUpkeepCost_ZeroInterval()`

**测试编写指南**：
```java
@Test
public void testCalculateDamage_WithSpeedBonus() {
    // Arrange: 设置测试数据
    double baseDamage = 10.0;
    double velocity = 2.0;
    double vRef = 1.0;
    double velDmgCoef = 1.0;
    double levelScale = 1.0;

    // Act: 执行被测方法
    double result = FlyingSwordCalculator.calculateDamage(
        baseDamage, velocity, vRef, velDmgCoef, levelScale);

    // Assert: 验证结果
    // speedFactor = (2/1)^2 * 1.0 = 4.0
    // damage = 10 * 1.0 * (1 + 4.0) = 50
    assertEquals(50.0, result, 0.001);
}
```

**边界条件测试要求**：
- 必须测试负值输入
- 必须测试零值输入
- 必须测试超出范围的输入
- 必须测试极端值（极大/极小）

**测试执行**：
```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests FlyingSwordCalculatorTest

# 生成覆盖率报告（如果配置了 Jacoco）
./gradlew test jacocoTestReport
```

**不得在测试中做的事**：
- ❌ 不得在测试中引入 Minecraft 服务器依赖（单元测试必须纯函数）
- ❌ 不得修改既有功能的行为来"适配"测试（测试应验证现有行为，而非改变行为）
- ❌ 不得编写依赖执行顺序的测试（每个测试应独立）
- ❌ 不得在测试中使用随机数（除非测试随机性本身）

**手动测试**：
- 自动化测试无法覆盖的功能（实体交互、渲染、网络同步等）必须人工测试
- 参考：`docs/FLYINGSWORD_MANUAL_TEST_CHECKLIST.md`
- 每个 Phase 完成后至少执行核心功能测试
- 发布前执行完整手动测试清单

**测试覆盖率监控**：
- 目标：整体覆盖率 ≥ 70%
- 优先级：
  - P0：Calculator（目标 ≥ 80%）
  - P1：UpkeepOps、CooldownOps（目标 ≥ 80%）
  - P2：KinematicsOps（目标 ≥ 70%）
  - P3：Systems（尽力而为，需要环境）

**回归测试**：
- 每次代码修改后运行 `./gradlew test`
- 确保所有测试通过再提交代码
- 如果测试失败，优先修复代码而非修改测试

## 8. 性能与调试

### 性能要求
- 单个飞剑 tick 开销 < 0.05ms（默认关闭高级功能时）
- 10 个飞剑同时存在时服务器 TPS ≥ 18
- 客户端渲染帧时间 < 16ms（60 FPS）

### 调试日志
- 默认静音（INFO 级别）
- 开发模式下可启用 DEBUG 标志
- 不得在生产环境中留有 System.out.println

## 9. 兼容性要求
- 向后兼容：旧存档加载后飞剑仍可使用
- 事件系统：外部模组可注册钩子扩展功能
- 配置文件：提供 TOML/JSON 配置文件支持（可选）

