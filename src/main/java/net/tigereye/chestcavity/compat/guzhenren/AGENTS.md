---
**状态**: ChestCavityForge Guzhenren 兼容模块 - 蛊真人器官系统集成
---

# 古真人器官系统开发指南

**目标**: 为古真人器官系统提供完整的开发规范、最佳实践和迁移指导，确保代码质量、可维护性和一致性。

---

## 开发前准备

### 项目基础
- **项目根目录**: `/home/kiz/Code/java/ChestCavityForge`
- **工作目录**: 始终在项目根目录下工作
- **构建验证**: 
  - `./gradlew compileJava` - 编译检查（每次修改后必须运行）
  - `./gradlew test` - 运行测试（重要修改后必须运行）
  - `./gradlew runClient` / `./gradlew runServer` - 手动测试

### 测试框架（强制要求）
- **状态**: ✅ 已搭建，可运行
- **框架**: JUnit 5 + Mockito
- **测试目录**: `src/test/java/net/tigereye/chestcavity/`
- **Mock 工具**: `src/test/java/net/tigereye/chestcavity/util/mock/`
- **运行**: `./gradlew test` | `./scripts/run-tests.sh [all|unit|coverage|quick]`
- **限制**: Minecraft 核心类（Player、ServerLevel、LivingEntity 等）**无法被 Mockito mock**
- **解决**: 将业务逻辑从 Minecraft 类中抽离为纯函数进行测试

---

## 核心架构组件

### 器官注册系统
**职责**: 统一管理古真人器官的注册和行为定义
- **主入口**: [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java)
- **规范定义**: [`OrganIntegrationSpec`](module/OrganIntegrationSpec.java)
- **支持的事件监听器**:
  - `SlowTickListener` - 慢速tick（每秒执行）
  - `OnHitListener` - 命中事件处理
  - `IncomingDamageListener` - 承受伤害处理
  - `RemovalListener` - 器官移除清理
  - `OnEquipHook` - 装备事件处理

### 资源操作助手
**职责**: 统一管理真元、精力、魂魄等资源的消耗和查询
- [`ResourceOps`](util/behavior/ResourceOps.java) - 真元/精力/魂魄资源操作
  - `consumeStrict()` - 严格消耗（仅玩家）
  - `consumeWithFallback()` - 带回退的消耗（非玩家回退到HP）
  - `consumeHunpoStrict()` - 魂魄消耗
- [`GuzhenrenResourceBridge`](util/bridge/GuzhenrenResourceBridge.java) - 底层资源读取接口

### 联动账本管理
**职责**: 统一管理联动通道和贡献者账本
- [`LedgerOps`](util/behavior/LedgerOps.java) - 联动通道和账本操作
  - `ensureChannel()` - 确保通道存在
  - `adjust()` - 调整通道值
  - `set()/remove()` - 设置/移除贡献者

### 冷却时间管理
**职责**: 统一管理所有器官的冷却时间
- [`MultiCooldown`](util/behavior/MultiCooldown.java) - 多键冷却管理
- **目标**: 替换所有手动计时器，防止负时间戳和状态漂移

### 吸收护盾管理
**职责**: 统一管理临时生命值（护盾）
- [`AbsorptionHelper`](util/behavior/AbsorptionHelper.java) - 护盾恢复与上限管理
- **特性**: 自动处理 MAX_ABSORPTION 属性注册，防止修饰符泄漏

---

## 灵魂系统开发

### 核心组件结构
- **灵魂假人**: `soul/fakeplayer/` 目录
- **大脑控制器**: `BrainController` + 子大脑模式
- **动作系统**: `soul/fakeplayer/actions/` 目录

### 皮肤同步系统（客户端）
- **目录**: [`client/skin/`](client/skin/)
- **SkinHandle**: 封装所有者 UUID、textures property/签名、模型、原始 URL 以及回退纹理
- **SkinResolver**: 异步下载 `textures.minecraft.net` 皮肤并做双层缓存

### 开发新动作步骤
1. 在 `soul/fakeplayer/actions/` 创建动作类
2. 实现 `Action` 接口
3. 在 `ActionRegistry` 中注册
4. 通过 `/soul action start <soul> <action>` 测试

---

## 测试驱动开发规范（强制）

### 可测试设计原则
在添加新器官或重构现有代码时，必须优先考虑可测试性：

1. **抽离纯逻辑**: 将计算、判断等逻辑抽离为静态纯函数
2. **先写测试**: 为纯函数编写单元测试
3. **再集成**: 将纯函数集成到器官行为中

### 开发流程示例
```java
// 步骤1: 纯逻辑（可测试）
public final class CooldownLogic {
    public static int calculateScaledDuration(int baseDuration, float scaleFactor) {
        return Math.max(0, (int)(baseDuration * scaleFactor));
    }
}

// 步骤2: 测试
@Test
@DisplayName("缩放持续时间计算测试")
void testCalculateScaledDuration() {
    assertEquals(50, CooldownLogic.calculateScaledDuration(100, 0.5f));
    assertEquals(0, CooldownLogic.calculateScaledDuration(100, -1.0f));
    assertEquals(100, CooldownLogic.calculateScaledDuration(100, 1.0f));
}

// 步骤3: 器官行为中使用
public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
    int duration = CooldownLogic.calculateScaledDuration(baseCd, scale);
    MultiCooldown.entry("key").setReadyAt(gameTime + duration);
}
```

---

## 添加新器官完整流程

### 步骤1: 创建器官行为类
```java
public final class MyOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {
    public static final MyOrganBehavior INSTANCE = new MyOrganBehavior();
    
    @Override
    public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
        // 每秒执行逻辑
        // 使用 ResourceOps, LedgerOps, MultiCooldown 进行操作
    }
    
    @Override
    public void onHit(ChestCavityInstance cc, ItemStack organ, LivingEntity target, DamageSource source, float amount) {
        // 命中目标时执行
    }
    
    // 确保attached钩子
    public void ensureAttached(ChestCavityInstance cc, ItemStack organ) {
        // 初始化逻辑
    }
    
    // 装备钩子
    public void onEquip(ChestCavityInstance cc, ItemStack organ, LivingEntity entity) {
        // 装备时执行
    }
}
```

### 步骤2: 注册器官规范
在相应的 `*OrganRegistry.java` 中添加规范:
```java
private static final List<OrganIntegrationSpec> SPECS = List.of(
    OrganIntegrationSpec.builder(MY_ORGAN_ID)
        .addSlowTickListener(MyOrganBehavior.INSTANCE)
        .addOnHitListener(MyOrganBehavior.INSTANCE)
        .ensureAttached(MyOrganBehavior.INSTANCE::ensureAttached)
        .onEquip(MyOrganBehavior.INSTANCE::onEquip)
        .build()
);
```

### 步骤3: 确保注册
确认 [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java) 包含你的注册器。

### 步骤4: 数据定义
在 `src/main/resources/data/chestcavity/organs/guzhenren/` 下创建对应的器官JSON定义文件。

---

## 组合杀招（Combo）开发规范

### 目录结构要求
组合杀招必须遵循标准目录结构：
```
compat/guzhenren/item/combo/<family>/<skill>/
├── behavior/         # 核心行为逻辑
├── calculator/       # 纯函数计算模块
├── tuning/          # 平衡性与数值常量
├── messages/        # 玩家反馈文本
├── fx/              # 粒子与音效
├── state/           # (可选) 技能状态存储
└── runtime/         # (可选) 跨 tick 运行时服务
```

### 开发流程
1. **创建目录结构**: 按照上述标准创建目录
2. **编写计算逻辑**: 在 `calculator/` 目录下创建 `...Calculator.java`（纯函数）
3. **编写单元测试**: 在测试目录中为计算器创建测试类
4. **定义参数**: 在 `tuning/` 创建 `...Tuning.java`（常量定义）
5. **定义文案**: 在 `messages/` 创建 `...Messages.java`（文本常量）
6. **实现效果**: 在 `fx/` 创建 `...Fx.java`（视觉效果）
7. **组装行为**: 在 `behavior/` 创建 `...Behavior.java`（主逻辑）
8. **注册技能**: 在 `ComboSkillRegistry` 中注册

### 注册示例
```java
// 在 ComboSkillRegistry.java 中
register("guzhenren/my_skill", 
    "杀招·我的技能",
    iconLocation("guzhenren:my_organ"),
    List.of(OrganRequirements.required("guzhenren:my_organ")),
    OrganRequirements.optional(),
    ComboCategory.COMBAT,
    "我的组合杀招描述",
    mySkillBehavior::initialize
);
```

---

## 核心规范要求

### DoT/Reaction 系统（必须遵守）
- **所有 DoT 必须携带 `typeId`**
- **仅使用带 `typeId` 的 `DoTEngine.schedulePerSecond(...)` 重载**
- **统一在 `DoTTypes` 中声明类型标识**
- **反应系统**: `util/reaction/ReactionRegistry` + `ReactionStatuses`

### 攻击能力注册规范（统一做法）
- **注册模式**: 行为类使用 `enum` 单例，在 `static {}` 中调用 `OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)`
- **禁止**: 在构造函数里做注册或重逻辑
- **客户端**: 热键仅以字面 `ResourceLocation` 加入列表，不引用行为类常量
- **网络**: 激活链路保持静默（INFO 以下）

### 真元消耗规范
- **推荐**: `ResourceOps.tryConsumeScaledZhenyuan(player, zhuanshu, jieduan, units)`
- **替代**: `tryConsumeTieredZhenyuan(player, zhuanshu, jieduan, Tier)`
- **禁止**: 直接传入 `baseCost` 的调用方式

### 资源操作统一要求
```java
// ✅ 推荐的资源消耗方式
ResourceOps.consumeStrict(player, zhenyuan, jingli);
ResourceOps.consumeWithFallback(entity, zhenyuan, jingli);
ResourceOps.consumeHunpoStrict(entity, hunpo);

// ✅ 推荐的联动通道操作
LinkageChannel channel = LedgerOps.ensureChannel(cc, MY_CHANNEL);
LedgerOps.adjust(cc, MY_CHANNEL, value);
LedgerOps.set(cc, MY_CHANNEL, contributor, value);

// ✅ 推荐的冷却管理
if (MultiCooldown.isReady(cc, COOLDOWN_KEY)) {
    MultiCooldown.set(cc, COOLDOWN_KEY, duration);
}

// ✅ 推荐的护盾管理
AbsorptionHelper.ensureCapacity(entity, MAX_ABSORPTION);
```

---

## 流派器官架构

### 已实现的流派家族
- **冰雪道** ([`bing_xue_dao/`](item/bing_xue_dao/)): 冰肌蛊、清熱蛊、双喜蛊等
- **力道** ([`li_dao/`](item/li_dao/)): 白石蛊、黑石蛊、自力更生蛊等  
- **魂道** ([`hun_dao/`](item/hun_dao/)): 小魂蛊、大魂蛊、体魄蛊等
- **剑道** ([`jian_dao/`](item/jian_dao/)): 剑影蛊及相关投射物
- **骨道** ([`gu_dao/`](item/gu_dao/)): 钢筋蛊、铁骨蛊等骨骼强化
- **炎道** ([`yan_dao/`](item/yan_dao/)): 火衣蛊、火油蛊等火焰相关
- **水道** ([`shui_dao/`](item/shui_dao/)): 全勇明蛊、水神蛊等
- **土道** ([`tu_dao/`](item/tu_dao/)): 石皮蛊、土墙蛊等

### 目录结构标准
```
compat/guzhenren/item/
├── bing_xue_dao/         # 冰雪道
│   ├── behavior/         # 器官行为
│   ├── OrganRegistry.java # 注册器
│   └── BingXueDaoClientAbilities.java # 客户端能力
├── li_dao/               # 力道
├── hun_dao/              # 魂道
├── jian_dao/             # 剑道
├── gu_dao/               # 骨道
├── yan_dao/              # 炎道
├── shui_dao/             # 水道
├── tu_dao/               # 土道
└── combo/                # 组合技能
    ├── bian_hua/         # 变化道
    ├── bing_xue/         # 冰雪道
    └── common/           # 通用组合
```

---

## 迁移计划与目标

### 待迁移项目
1. **雷/食/丑/酒系** - 手动计时器迁移到 `MultiCooldown`
2. **资源统一** - 剩余 `LinkageManager` 调用迁移到 Ops
3. **吸收提供者** - 冰肌蛊、全勇明蛊等吸收修饰符验证

### Guzhenren Ops 迁移（四步执行）
1. **盘点**: 用 `rg` 搜索 `LinkageManager.getContext|getOrCreateChannel|GuzhenrenResourceBridge.open|NBTCharge`
2. **迁移**: 按家族分批替换至对应 Ops
3. **一致性**: 确认无直接遗留调用
4. **验证**: 编译 + 游戏内实测

### 目标规范
- 每家族仅 1 主动 + 1 被动，其余通过 Combo 触发
- 无直接 `LinkageChannel.adjust` 遗留
- 冷却集中在 `MultiCooldown`
- 护盾统一 `AbsorptionHelper`
- 所有 DoT 均携带 `typeId`

---

## 数据驱动架构

### 器官数据定义
**路径**: `src/main/resources/data/chestcavity/organs/guzhenren/`
- **人类器官**: `human/` 目录
- **动物器官**: `animal/` 目录
- **结构**: 按流派组织，包含器官属性、兼容性等

### 兼容性类型定义
**路径**: `src/main/resources/data/chestcavity/types/compatibility/guzhenren/`
- **结构**: 按实体类型组织
- **内容**: 定义不同实体类型的胸腔兼容性

### 实体分配映射
**路径**: `src/main/resources/data/chestcavity/entity_assignment/guzhenren/`
- **内容**: 将古真人实体类型映射到胸腔类型

---

## 验证与调试

### 编译验证
```bash
# 基础编译
./gradlew compileJava

# 完整测试
./gradlew test

# 代码风格检查
./gradlew check

# 手动测试
./gradlew runClient
```

### 调试工具与命令
```bash
# 灵魂导航状态
/soul nav dump <soulName>

# 联动通道状态  
/linkage dump

# 灵魂动作
/soul action start <soul> <action>

# 代码搜索
rg "pattern" src/main/java

# Git状态
git status -sb
```

### 验证清单
- **装备/卸下**: 不应留下账本残留或负冷却
- **计时器状态**: 应在物品移除和区块重载后保持一致
- **日志噪音**: 保持调试开关默认关闭

---

## 最佳实践

### 代码规范
1. **使用现有助手**: 优先使用 `LedgerOps`、`ResourceOps`、`MultiCooldown`
2. **避免手动操作**: 不要直接操作 `LinkageManager` 或原始冷却计时器
3. **资源检查**: 所有资源操作都要检查返回值
4. **边界条件**: 明确定义所有边界情况

### 测试规范
1. **纯逻辑优先**: 复杂计算、条件判断抽离为纯函数并编写测试
2. **测试命名**: 使用清晰的 `@DisplayName` 注解
3. **AAA模式**: Arrange（准备）→ Act（执行）→ Assert（断言）
4. **边界测试**: 必须测试边界值和异常情况
5. **重构保护**: 重构前先有测试，重构后测试必须通过

### 性能考虑
- **慢速tick**: 资源维护和状态同步放在慢速tick中
- **事件驱动**: 命中/伤害等高频事件保持轻量
- **冷却管理**: 使用 `MultiCooldown` 避免重复计时器

---

## 快速参考

### 关键文件
- **主入口**: [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java)
- **器官规范**: [`OrganIntegrationSpec`](module/OrganIntegrationSpec.java)
- **资源助手**: [`ResourceOps`](util/behavior/ResourceOps.java)
- **联动助手**: [`LedgerOps`](util/behavior/LedgerOps.java)
- **冷却管理**: [`MultiCooldown`](util/behavior/MultiCooldown.java)
- **护盾助手**: [`AbsorptionHelper`](util/behavior/AbsorptionHelper.java)

### 常用操作模板
```java
// 资源消耗模板
if (ResourceOps.consumeWithFallback(entity, zhenyuan, jingli)) {
    // 消耗成功，执行逻辑
}

// 联动账本模板
LinkageChannel channel = LedgerOps.ensureChannel(cc, MY_CHANNEL);
LedgerOps.adjust(cc, MY_CHANNEL, value);

// 冷却管理模板
String cooldownKey = "guzhenren.family.organ.ability";
if (MultiCooldown.isReady(cc, cooldownKey)) {
    // 执行能力
    MultiCooldown.set(cc, cooldownKey, duration);
}

// DoT应用模板
DoTEngine.schedulePerSecond(target, damage, duration, DoTTypes.FAMILY_SPECIFIC);
```

---

**最后更新**: 2025-10-18  
**维护者**: ChestCavityForge 开发团队  
**版本**: v2.0 - 全面规范化版本
