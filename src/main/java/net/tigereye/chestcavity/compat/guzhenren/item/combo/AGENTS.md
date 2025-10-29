# 组合杀招（Combo）规范与最佳实践

**目标**：为“变化道”及其他家族的组合杀招提供一致的、可测试、可维护的工程结构。所有新建/迁移的 combo 均应遵循本文约定。

---

## 完整开发流程

开发一个新的组合杀招应严格遵循以下步骤，确保代码质量、可维护性和一致性。

### 步骤 1：创建目录结构
在 `compat/guzhenren/item/combo/<family>/<skill>/` 路径下，为新技能创建标准目录结构：
- `behavior/`: 核心行为逻辑
- `calculator/`: 纯函数计算模块
- `tuning/`: 平衡性与数值常量
- `messages/`: 玩家反馈文本
- `fx/`: 粒子与音效
- `state/`: (可选) 技能状态存储
- `runtime/`: (可选) 跨 tick 运行时服务

### 步骤 2：核心逻辑与单元测试 (TDD)
1.  **编写计算逻辑**: 在 `calculator/` 目录下创建 `...Calculator.java`。此类必须只包含纯函数（static methods），负责所有数值计算，如伤害、范围、持续时间等。
2.  **编写单元测试**: 在 `src/test/java/...` 对应路径下创建 `...CalculatorTest.java`。
    -   使用 JUnit 5 和 AssertJ 进行断言。
    -   为每个计算方法编写多个测试用例，覆盖边界条件（如0、负值、最大值）。
    -   遵循 `Arrange-Act-Assert` 模式。
    -   这是**强制步骤**，确保核心算法的正确性。

### 步骤 3：定义参数、文案与效果
1.  **参数 (`tuning/`)**: 创建 `...Tuning.java`，将所有魔法数字（如冷却时间、资源消耗、效果强度）定义为公开静态常量。
2.  **文案 (`messages/`)**: 创建 `...Messages.java`，将所有给玩家看的信息（如成功、失败、提示）定义为字符串常量和静态方法。
3.  **效果 (`fx/`)**: 创建 `...Fx.java`，将所有粒子效果和音效的播放逻辑封装在静态方法中。

### 步骤 4：组装行为逻辑
1.  **创建行为类**: 在 `behavior/` 目录下创建 `...Behavior.java`。
2.  **实现 `initialize()`**: 创建一个 `public static void initialize()` 方法，用于在 `ComboSkillRegistry` 中注册技能的激活监听器。
3.  **实现 `activate()`**: 创建一个 `private static void activate(...)` 方法，作为技能的入口。
    -   **前置检查**: 检查冷却、资源等条件。
    -   **调用计算**: 从 `...Calculator` 获取计算结果。
    -   **执行逻辑**: 调用 `...Fx` 播放效果，`...Messages` 发送消息。
    -   **设置冷却**: 使用 `MultiCooldown` 设置技能冷却。
    -   **发送Toast**: **必须**调用 `ComboSkillRegistry.scheduleReadyToast()` 来安排冷却完成提示。

### 步骤 5：注册技能与文档
1.  **注册技能**: 打开 `src/main/java/net/tigereye/chestcavity/skill/ComboSkillRegistry.java`。
    -   在 `bootstrap()` 方法中，调用 `register()` 方法。
    -   填写所有必要信息：技能ID、显示名称、**图标路径 (iconLocation)**、必需器官、可选器官/流派、分类、描述等。
    -   在 `initializer` 参数中，传入 `...Behavior::initialize`。
2.  **编写文档**: 在 `src/main/resources/assets/guzhenren/docs/combo/` 目录下创建对应的 `...json` 文件。
    -   **`id`**: 技能ID，必须与注册表中的一致。
    -   **`title`**: 技能的完整标题（例如 “杀招·太极错位”）。
    -   **`summary`**: 一句话总结技能效果。
    -   **`details`**: 详细描述技能的效果、消耗、冷却和需求。
    -   **`icon`**: 作为图标的**物品ID**（例如 `guzhenren:yin_yang_zhuan_shen_gu`）。
    -   **`iconTexture`**: 指向技能图标**纹理的文件路径**（例如 `guzhenren:textures/skill/yin_yang_tai_ji_swap.png`）。

### 步骤 6：最终验证
1.  运行 `./gradlew build` 或 `./gradlew check` 命令。
2.  确保编译通过，并且没有引入新的 Checkstyle 警告。
3.  运行 `./gradlew test` 确保所有单元测试（包括你为新技能编写的）都能通过。

---

## 行为约定
- **注册**: 技能的 `...Behavior.java` 必须提供一个 `public static void initialize()` 方法，通过 `ActivationBootstrap.register` 懒加载激活逻辑。
- **资源/冷却**:
  - 统一使用 `ComboSkillUtil.tryPayCost()` 处理资源消耗。
  - 统一使用 `MultiCooldown` 管理冷却，冷却状态应挂在核心承载器官的 `OrganState` 下。
  - 必须调用 `ComboSkillRegistry.scheduleReadyToast()` 来显示冷却就绪提示。
- **命名与日志**:
  - 类名严格以 `Behavior`, `Calculator`, `Tuning`, `Fx`, `Messages` 等后缀结尾。
  - 日志保持安静（INFO 以下），所有玩家反馈通过 `...Messages` 类处理。

---

## 示例
- **阴阳转身蛊系列**: `compat/guzhenren/item/combo/bian_hua/yin_yang/`
- **兽皮蛊系列**: `compat/guzhenren/item/combo/bian_hua/shou_pi/`
