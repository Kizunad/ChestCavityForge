---
**状态**: ChestCavityForge Guzhenren 兼容模块 - 为蛊真人模组提供胸腔器官系统集成
---

## 必须了解

- **项目根目录**: `/home/kiz/Code/java/ChestCavityForge`
- **工作目录**: 始终在项目根目录下工作
- **构建命令**:
  - `./gradlew compileJava` - 编译检查
  - `./gradlew test` - 运行测试
  - `./gradlew runClient` / `./gradlew runServer` - 手动测试

### 测试框架（2025-01-28 新增）
- **状态**: ✅ 已搭建，可运行
- **框架**: JUnit 5 + Mockito
- **测试目录**: `src/test/java/net/tigereye/chestcavity/`
- **Mock 工具**: `src/test/java/net/tigereye/chestcavity/util/mock/`
- **文档位置**:
  - `docs/TESTING.md` - 完整测试指南
  - `docs/HOW_TO_WRITE_TESTS.md` - 编写教程
  - `docs/TESTING_SUMMARY.md` - 框架总结
  - `docs/MANUAL_TEST_CHECKLIST.md` - 游戏内手动测试清单
- **运行脚本**: `./scripts/run-tests.sh [all|unit|coverage|quick]`
- **示例测试**: `src/test/java/net/tigereye/chestcavity/examples/SimpleTestExample.java` ✓

**⚠️ 重要限制**：
- Minecraft 核心类（Player、ServerLevel、LivingEntity 等）**无法被 Mockito mock**
- 原因：这些类是 final 或有特殊类加载器，即使 mockito-inline 也无法绕过

**✅ 推荐测试策略**：
1. **测试纯逻辑**：将业务逻辑从 Minecraft 类中抽离为纯函数
2. **可测试设计**：
   ```java
   // ❌ 难以测试（依赖 Minecraft 类）
   public void onTick(Player player) {
       float health = player.getHealth();
       if (health < 10) { /* 复杂逻辑 */ }
   }

   // ✅ 易于测试（纯函数）
   public static boolean needsHealing(float health) {
       return health < 10;
   }

   @Test
   void testNeedsHealing() {
       assertTrue(needsHealing(5));
       assertFalse(needsHealing(15));
   }
   ```
3. **游戏内测试**：对于必须在游戏环境中测试的功能，使用手动测试清单

---

## 模块概述

### 核心架构
蛊真人兼容模块采用**声明式集成架构**，通过 [`OrganIntegrationSpec`](module/OrganIntegrationSpec.java:1) 定义器官行为，由 [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java:1) 统一注册。

### 主要组件

#### 1. 器官注册系统
- **入口点**: [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java:1)
- **注册规范**: [`OrganIntegrationSpec`](module/OrganIntegrationSpec.java:1)
- **支持的事件监听器**:
  - `SlowTickListener` - 慢速tick（每秒）
  - `OnHitListener` - 命中事件
  - `IncomingDamageListener` - 承受伤害
  - `RemovalListener` - 器官移除
  - `OnEquipHook` - 装备事件

#### 2. 资源管理助手
- [`ResourceOps`](util/behavior/ResourceOps.java:1) - 真元/精力/魂魄资源操作
- [`LedgerOps`](util/behavior/LedgerOps.java:1) - 联动通道和账本操作
- [`MultiCooldown`](util/behavior/MultiCooldown.java:1) - 多键冷却管理

#### 3. 灵魂系统 (Soul)
- **灵魂假人**: [`soul/`](soul/) 目录
- **行动大脑**: `BrainController` + 子大脑模式
- **动作系统**: `actions/` 目录中的可执行动作

#### 4. 皮肤同步（客户端）
- **目录**: [`client/skin/`](client/skin/)
- **SkinHandle**: 封装所有者 UUID、textures property/签名、模型、原始 URL 以及回退纹理，用于渲染时构建统一请求。
- **SkinResolver**: 在客户端异步下载 `textures.minecraft.net` 皮肤、注册动态纹理并做双层缓存（handle + hash），下载失败或未完成时回退至默认皮肤，避免 TextureManager 报错；未来若需多层贴图可扩展 `SkinLayers`。
- **使用规范**: 渲染端调用 `SkinResolver.resolve(SkinHandle.from(entity))` 获取纹理；服务端需要在实体数据中写入 property/签名，已在 `XiaoGuangIllusionEntity` 等实现里完成。

---

## 开发与测试指南

### 测试驱动开发（推荐）

#### 编写可测试的代码
在添加新器官或重构现有代码时，优先考虑可测试性：

1. **抽离纯逻辑**：将计算、判断等逻辑抽离为静态纯函数
2. **先写测试**：为纯函数编写单元测试
3. **再集成**：将纯函数集成到器官行为中

示例：
```java
// 1. 纯逻辑（可测试）
public final class CooldownLogic {
    public static int calculateScaledDuration(int baseDuration, float scaleFactor) {
        return Math.max(0, (int)(baseDuration * scaleFactor));
    }
}

// 2. 测试
@Test
void testCalculateScaledDuration() {
    assertEquals(50, CooldownLogic.calculateScaledDuration(100, 0.5f));
    assertEquals(0, CooldownLogic.calculateScaledDuration(100, -1.0f));
}

// 3. 器官行为中使用
public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
    int duration = CooldownLogic.calculateScaledDuration(baseCd, scale);
    MultiCooldown.entry("key").setReadyAt(gameTime + duration);
}
```

运行测试：`./gradlew test --tests "CooldownLogicTest"`

### 添加新器官

#### 步骤1: 创建器官行为类
```java
public final class MyOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {
    public static final MyOrganBehavior INSTANCE = new MyOrganBehavior();
    
    @Override
    public void onSlowTick(ChestCavityInstance cc, ItemStack organ) {
        // 每秒执行逻辑
    }
    
    @Override
    public void onHit(ChestCavityInstance cc, ItemStack organ, LivingEntity target, DamageSource source, float amount) {
        // 命中目标时执行
    }
}
```

#### 步骤2: 注册到对应流派
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

#### 步骤3: 确保在集成模块中注册
确认 [`GuzhenrenIntegrationModule`](module/GuzhenrenIntegrationModule.java:41) 包含你的注册器。

### 常用助手模式

#### 资源消耗
```java
// 严格消耗（仅玩家）
ResourceOps.consumeStrict(player, zhenyuan, jingli);

// 带回退的消耗（非玩家回退到HP）
ResourceOps.consumeWithFallback(entity, zhenyuan, jingli);

// 魂魄消耗
ResourceOps.consumeHunpoStrict(entity, hunpo);
```

#### 联动通道操作
```java
// 确保通道存在
LinkageChannel channel = LedgerOps.ensureChannel(cc, CHANNEL_ID);

// 调整通道值
LedgerOps.adjust(cc, CHANNEL_ID, adjustment);

// 设置贡献者
LedgerOps.set(cc, CHANNEL_ID, contributor, value);
```

#### 冷却管理
```java
// 检查冷却
if (MultiCooldown.isReady(cc, COOLDOWN_KEY)) {
    // 执行逻辑
    MultiCooldown.set(cc, COOLDOWN_KEY, duration);
}
```

---

## 流派器官目录

### 已实现的流派
- **冰雪道** ([`bing_xue_dao/`](item/bing_xue_dao/)): 冰肌蛊、清熱蛊、双喜蛊等
- **力道** ([`li_dao/`](item/li_dao/)): 白石蛊、黑石蛊、自力更生蛊等  
- **魂道** ([`hun_dao/`](item/hun_dao/)): 小魂蛊、大魂蛊、体魄蛊等
- **剑道** ([`jian_dao/`](item/jian_dao/)): 剑影蛊及相关投射物
- **骨道** ([`gu_dao/`](item/gu_dao/)): 钢筋蛊、铁骨蛊等骨骼强化
- **炎道** ([`yan_dao/](item/yan_dao/)): 火衣蛊、火油蛊等火焰相关
- **水道** ([`shui_dao/](item/shui_dao/)): 全勇明蛊、水神蛊等
- **土道** ([`tu_dao/](item/tu_dao/)): 石皮蛊、土墙蛊等
- **和其他20+流派**...

### 特殊系统
- **蛊方系统** ([`gufang/`](gufang/)): 炼蛊配方加载和注册

---

## 灵魂系统 (Soul System)

### 核心组件
- **灵魂假人**: 基于 `FakePlayer` 的灵魂实体
- **大脑控制器**: `BrainController` 统一调度
- **动作系统**: 可并发执行的灵魂行为

### 大脑模式
- `AUTO` - 根据订单自动选择
- `COMBAT` - 战斗模式（强制战斗+治疗）
- `IDLE` - 待机模式
- `SURVIVAL` - 生存模式（规避优先）

### 动作类型
- `action/force_fight` - 强制战斗（独占）
- `action/heal` - 治疗（紧急并发）
- `action/guard` - 守卫

---

## 未来迁移
- 五行化痕已迁移至 `item/combo/wuxing/hua_hen/`，并拆分为 Runtime/Cost/Fx/Messages 等模块，旧版行为类移除。

### 兼容层目录整形计划（compat/guzhenren）

#### 问题盘点（杂乱/繁冗/错位）
- 体量过大、职责混杂（应拆为“主动/被动/Combo”）
  - `compat/guzhenren/item/combo/wuxing/hua_hen/`（已完成拆分，供其他家族参考）
  - `compat/guzhenren/item/bian_hua_dao/behavior/YinYangZhuanShenGuBehavior.java`
  - `compat/guzhenren/item/bian_hua_dao/behavior/ShouPiGuOrganBehavior.java`
  - `compat/guzhenren/item/feng_dao/behavior/QingFengLunOrganBehavior.java`
  - `compat/guzhenren/item/guang_dao/behavior/XiaoGuangGuOrganBehavior.java`
  - `compat/guzhenren/item/bing_xue_dao/behavior/BingJiGuOrganBehavior.java`
- 分层违规：上层模块反向引用 compat
  - `util/reaction/ReactionRegistry.java` 直接引用 `compat/guzhenren/util/CombatEntityUtil`（需上提为通用 util）
- 位置不当（应归属家族/子模块）
  - `compat/guzhenren/entity/summon/OwnedSharkEntity.java`（建议移至 `item/bian_hua_dao/entity/`）
  - `compat/guzhenren/commands/WuxingConfigCommand.java`、`WuxingGuiBianConfigCommand.java`（建议移至家族命令子目录）
- 重复/旧式助手（应统一到现有 Ops）
  - `util/behavior/Cooldown.java`、`CooldownOps.java`（以 `MultiCooldown` 统一替换）
  - `item/common/DamageOverTimeHelper.java`（DoT 逻辑已统一到 `DoTEngine` + `DoTTypes`）
- 灵魂兽模块分散、命名层级不合理
  - `util/hun_dao/soulbeast/**` 包含 API/状态/命令/伤害钩子（建议合流至 `compat/guzhenren/soulbeast/**`，远期上提到 `chestcavity/soul/beast/**` 并做桥接）
- 跨域工具放错层（应上提为通用）
  - `compat/guzhenren/util/{CombatEntityUtil, TrueDamageHelper, DamagePipeline, OrganPresenceUtil}.java`

#### 目标重定位与规范
- 通用工具上提
  - `compat/guzhenren/util/CombatEntityUtil` → `chestcavity/util/CombatEntityUtil`
  - `compat/guzhenren/util/TrueDamageHelper` → `chestcavity/util/TrueDamageHelper`
  - `compat/guzhenren/util/DamagePipeline` → `chestcavity/util/DamagePipeline`
  - `item/common/DamageOverTimeHelper` → `chestcavity/util/dot/DamageOverTimeHelper`（或保留门面）
- 家族归类与实体就位
  - `entity/summon/OwnedSharkEntity` → `item/bian_hua_dao/entity/OwnedSharkEntity`
  - 五行相关命令 → `item/bian_hua_dao/commands/`
- 冷却与行为助手统一
  - 标记 `Cooldown*.java` 为 Deprecated；使用点迁至 `MultiCooldown`
  - 护盾统一 `AbsorptionHelper`；资源/账本统一 `ResourceOps`/`LedgerOps`
- 灵魂兽模块合流
  - `util/hun_dao/soulbeast/**` → `compat/guzhenren/soulbeast/**`（短期）
  - 长期：抽取为 `chestcavity/soul/beast/**`，compat 仅保留桥接/注册

#### 执行顺序（安全改动优先）
1) 解除反向依赖：上提 `CombatEntityUtil`，修正 `ReactionRegistry` 与各行为引用
2) 实体/命令归位：移动 `OwnedSharkEntity` 与五行命令至家族目录并修正注册
3) 冷却收敛：标记 `Cooldown*.java` 废弃并按桶迁移使用点至 `MultiCooldown`
4) 灵魂兽合流：整合到 `compat/guzhenren/soulbeast/**`；必要时再上提为通用
5) 行为拆分：按家族实施“一主动 + 一被动 + Combo”拆分与 Ops 接线

#### 验收标准
- Reaction 顶层不再引用 compat；所有跨域工具位于 `chestcavity/util/**`
- 冷却全部由 `MultiCooldown` 管理；无手写时间戳与负冷却日志
- 护盾通过 `AbsorptionHelper` 统一；资源/账本通过 `ResourceOps`/`LedgerOps` 统一
- 行为拆分后，每家族仅保留 1 主动 + 1 被动；其他经 `ComboSkillRegistry` 触发
- DoT 均携带 `typeId`，反应规则由 `ReactionRegistry` 驱动

#### 风险与回滚
- 目录搬迁引起的注册/反射路径失效 → 渐进式迁移+编译校验
- 被动合并导致重复触发 → 单例入口+限流与去重
- Combo 窗口偏差 → `MultiCooldown`/计数阈值配合调参开关

### 一主一被动重构计划（统一器官行为）

#### 背景与目标
- 目标：将 `compat/guzhenren/item` 全部器官行为统一为“1 主动（Active）+ 1 被动（Passive）”，其余扩展逻辑迁移为 Combo 杀招或数据驱动附加效果。
- 原则：不回退既有逻辑与数值体验；统一冷却、资源、护盾与反应系统的接入点，减少分散实现和负冷却/账本残留等问题。

#### 范围
- 目录：`src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/**`（含各流派家族）；客户端热键注册与网络载荷链路；DoT 与 Reaction 接线；冷却、资源、联动账本、吸收护盾。
- 数据：资源定义与器官数据驱动目录保持不变（`src/main/resources/data/chestcavity/`）；如需更改仅更新行为映射，不改动数据结构。

#### 设计原则
- 主动唯一：每个家族仅保留一个主动触发的攻击/姿态/爆发能力，使用字面 `ResourceLocation` 作为激活 ID。
- 被动唯一：每个家族仅保留一个被动入口，集中处理 tick/onHit/onHurt 等常驻效果，避免重复监听与叠算。
- Combo 承载：除主动/被动之外的技能全部改造为 `ComboSkill`，以命中序列、状态标签或姿态窗口触发。
- 统一助手：
  - 冷却：`util/behavior/MultiCooldown`
  - 资源：`util/behavior/ResourceOps`（必要时只读 `GuzhenrenResourceBridge`）
  - 账本：`util/behavior/LedgerOps`
  - 护盾：`AbsorptionHelper`（含 MAX_ABSORPTION 修饰符管理）
  - DoT/反应：`DoTEngine` + `DoTTypes` + `util/reaction/ReactionRegistry`

#### 主动/被动定义
- 主动（Active）
  - 激活入口：`OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)`（enum 单例在 static 代码块注册）
  - 冷却：仅用 `MultiCooldown`（禁止自建时间戳）；键命名 `guzhenren.<family>.<organ>.active`。
  - 资源：通过 `ResourceOps` 统一消耗与失败回滚；必要时写入 `LedgerOps` 以记录贡献。
  - 网络：`ChestCavityHotkeyPayload.handle -> OrganActivationListeners.activate(id, cc)`；静默（INFO 以下）。
  - 客户端：`*ClientAbilities.onClientSetup(FMLClientSetupEvent)` 仅以字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`；不引用行为类常量；跳过占位 `chestcavity:attack_abilities`。
- 被动（Passive）
  - 合并被动监听至家族唯一类：tick/onHit/onHurt/onEquipChange 等；去重并防抖。
  - 抽取重复逻辑至助手（资源、账本、护盾、反应），删除手写钳制/属性清理/自建定时器。

#### Combo 杀招（ComboSkill）
- 目录：`src/main/java/net/tigereye/chestcavity/skill/ComboSkillRegistry.java` 登记；具体触发在各命中/状态事件处发出信号或由 Combo 引擎维护窗口。
- 触发：命中链阈值、状态标签（如 `reaction/*_mark`）、姿态/格挡/位移窗口等。
- 冷却：独立 `MultiCooldown` 键，如 `guzhenren.<family>.<organ>.combo.<name>`。
- 与反应联动：必要时在 `ReactionRegistry.preApplyDoT`/`post` 钩子中完成取消/收尾处理。

##### Combo/Synergy 统一与容量规划
- 术语统一：Synergy 与 Combo 视为同一概念，统一纳入 `ComboSkill` 体系；不再单独维护 “synergy” 分支。历史命名按 `.../combo/<name>` 归档，必要时保留别名映射。
- 触发分类（标准化）：
  - 连击/计数：命中 N 次、时间窗 T 内的节律触发；可附加“收尾技”判定。
  - 状态/标签：`ReactionTagOps` 上的标记堆叠与相消（例：`frost_mark`、`toxic_mark`）。
  - 姿态窗口：格挡成功、闪避成功、短时架势（GUARD/PARRY/DASH）。
  - 资源阈值：`ResourceOps`（真元/精力/魂魄）达到/消耗某阈值。
  - 环境/领域：残留域、地形、光照、水域等上下文。
  - 账本/附件：`LedgerOps` 通道值、附件状态（如 YinYang/Wuxing 附件）。
- 目录组织：
  - 核心框架：跨杀招复用的上下文、消息等工具统一放在 `compat/guzhenren/item/combo/framework/`。
  - 杀招实现：每个杀招拆分独立子目录 `compat/guzhenren/item/combo/<theme>/<combo>/`（例：`wuxing/gui_bian/` 下含 `runtime/logic/fx/state`）。
  - 状态附件：随杀招放在其子目录下的 `state/`，不要再集中在旧的 `combo/state` 目录。
- 注册分片：
  - 顶层由 `ComboSkillRegistry` 提供统一注册入口；`GuzhenrenIntegrationModule` 在构造阶段调用各家族 `<Family>ComboRegistry.registerAll()`。
  - 命名规范：`chestcavity:guzhenren/<family>/<organ>/combo/<name>`，避免与主动/被动冲突。
- 冷却与去重：
  - 冷却键命名：`guzhenren.<family>.<organ>.combo.<name>`；支持作用域（Scope）：`PER_ACTOR`/`PER_TARGET`/`AREA`（以键后缀或参数编码实现）。
  - 去重策略：同一时窗内重复触发合并为一次执行；对同目标支持“更强者覆盖”。
- 并发控制：
  - 排他组（Exclusion Group）：同组 Combo 不并发；组键建议 `guzhenren.<family>.exg.<label>`。
  - 协同组（Coop Group）：允许并发但共享冷却上限；用于治疗/位移类组合技。
- 扩展与数据化：
  - 先代码化（接口/构件/路由），后续考虑数据驱动清单（如 `data/.../combos/*.json`）以声明触发条件和效果拼装。
  - 统计与调试：为每家族路由加入最小日志开关与计数器，默认关闭，便于容量压力下定位热点。

#### 伤害类型与管线重规划（Damage/DoT/附加/Reaction）
- 目标：统一伤害分类、注册与使用入口，避免各处手写 `new DamageSource`/直接 `hurt(...)` 造成的不可控分歧，确保免疫/抗性/反应/统计一致。
- 伤害分类（语义层）：
  - 直伤（Direct）：普通命中或能力瞬时伤害（可为物理/魔法/元素）。
  - DoT：按秒结算的持续伤害；必须带 `DoTTypes.*` 的 `typeId`；反应在 `preApplyDoT` 执行。
  - 附加伤害（Bonus/Extra）：在命中链上追加的少量脉冲伤害（同 tick 内或短窗内多段）；用于“触发式加成”。
  - Reaction/AoE：由 `ReactionRegistry` 或 `EffectsEngine` 触发的范围或二次伤害（爆燃/碎裂/蒸汽）。
  - True/穿透：忽略护甲或部分减免的“真伤”；通过 DamageType+Tag 定义，替代手动绕过流程。
  - 灵魂/精神：对灵魂实体或带魂魄抗性有特别交互（如 `HUN_DAO_SOUL_FLAME`）。
- 注册与命名（实现层）：
  - DamageTypes：集中到 `compat/guzhenren/registry/GRDamageTypes`（或上提为通用 `CCDamageTypes`）；命名 `chestcavity:guzhenren/<family>/<name>` 或 `chestcavity:reaction/<name>`。
  - DamageTags：集中到 `GRDamageTags`；提供范畴标签（`FIRE`/`FROST`/`LIGHTNING`/`TOXIC`/`CORROSION`/`BONE`/`SOUL`/`REACTION`/`DOT`/`TRUE`）。
  - DamageSources：集中到 `GRDamageSources` 暴露工厂方法（含 attacker/indirect/position 变体）。
  - DoTTypes：保持在 `util/DoTTypes`；所有持续伤害都要绑定唯一 `typeId` 并在此集中声明。
- 使用规范：
  - Direct/附加伤害：统一经 `compat/guzhenren/util/DamagePipeline` 或未来上提的通用入口；按需叠加 DamageTag/上下文并调用 `hurt`。
  - DoT：只用 `DoTEngine.schedulePerSecond(..., typeId=DoTTypes.XXX)` 重载，`typeId==null` 禁止；必要时在 `EffectsEngine` 附带视觉主题。
  - Reaction：`ReactionRegistry`/`EffectsEngine` 只引用集中注册的 `GRDamageTypes`；不得内联创建 `DamageSource`。
  - True/穿透：通过 DamageType+Tag 实现（如 `TRUE`/`BYPASS_ARMOR`），避免手动“无视护甲”逻辑。
  - 免疫/抗性：统一使用 `ReactionTagOps`/状态效果或实体属性；DamagePipeline 层做一次前置判定，拒绝或折算；避免各处重复写免疫判断。
- 迁移步骤：
  1) 盘点所有 `damageSources().*`/`new DamageSource`/`hurt(...)` 直接用法，替换为 `GRDamageSources`/`DamagePipeline`；建立“调用映射表”。
  2) 将 Reaction 中内联的 `magic`/`fire`/`indirectMagic` 等改为 `GRDamageTypes`；保留 vanilla 类型仅在明确需要 vanilla 行为时使用。
  3) DoT 全量补齐 `DoTTypes` 映射；未声明者统一新增类型并文档化。
  4) TrueDamageHelper 等工具上提为通用后，通过 DamageType+Tag 重写实现，去除手写穿透分支。
  5) 编译+定向手测（免疫/抗性/反应触发是否一致），逐桶推进。

#### 真元 BaseCost 规范（ConsumeScaled 支持）
- 新增：`net/tigereye/chestcavity/guzhenren/util/ZhenyuanBaseCosts.java`
  - `unitFactor(zhuanshu, jieduan)`: 返回该“转/阶段”的缩放分母 D；scaled = base/D。
  - `baseForUnits(zhuanshu, jieduan, units)`: 期望在“设计阶段”消耗 units 点真元，返回应使用的 baseCost。
  - `baseForTier(zhuanshu, jieduan, Tier)`: 推荐分级（TINY/SMALL/MEDIUM/HEAVY/BURST）→ baseCost。
  - 便捷常量：`UNIT_AT_1Z0..6Z0`、`UNIT_AT_1Z1..1Z3` 等，便于直接乘以倍数。
- 使用范式：
  - 定义：在“一转初(阶段1)”消耗 SMALL（3 点）真元 → `double base = ZhenyuanBaseCosts.baseForTier(1, 1, Tier.SMALL);`
  - 扣减：`ResourceOps.tryConsumeScaledZhenyuan(player, base)` 或 `handle.consumeScaledZhenyuan(base)`。
  - 说明：玩家实际消耗随其当前“转/阶段”自动缩放，开发仅需统一选取设计基准。

- 验收：
  - 代码中不再出现内联 `new DamageSource`；`ReactionRegistry` 不依赖 compat 层工具。
  - 所有 DoT 均带 `typeId` 且能触发预期反应；附加伤害通过统一入口发出。
  - 抗性/免疫判定集中且无重复；统计与日志（如伤害类型计数）可按开关启用。

#### Lint 规则（ResourceOps 纯 baseCost 用法预警）
- Checkstyle 已添加正则检查：如检测到 `ResourceOps.tryConsumeScaledZhenyuan(xxx, baseCost)` 的两参用法（单行调用）将发出 WARN，建议改用：
  - `tryConsumeScaledZhenyuan(handle/player/entity, zhuanshu, jieduan, units)`，或
  - `tryConsumeTieredZhenyuan(handle/player/entity, zhuanshu, jieduan, Tier)`。
- 运行方式：
  - `./gradlew check` 或 `./gradlew lint`（仅告警，不阻断构建）。

#### DoT/Reaction 统一规范（必须遵守）
- 所有 DoT 必须携带 `typeId`：仅使用带 `typeId` 的 `DoTEngine.schedulePerSecond(...)` 重载，`typeId==null` 将抛异常。
- 统一在 `DoTTypes` 中声明/复用类型标识（例：`YAN_DAO_HUO_YI_AURA`、`HUN_DAO_SOUL_FLAME`、`SHUANG_XI_FROSTBITE`、`YIN_YUN_CORROSION`）。
- 反应系统：`util/reaction/ReactionRegistry` + `ReactionStatuses`。默认规则为火衣光环 + 油涂层 => 爆炸并移除油；当前不设置连锁屏蔽（窗口=0）。
- 在 DoT 伤害执行前会调用 `ReactionRegistry.preApplyDoT(...)`，可取消当次伤害；新增反应用 `ReactionRegistry.register(...)`。

#### 注册与目录结构
- 行为类依旧在各家族目录的 `behavior/` 下；主动/被动合并为家族唯一实现。
- Combo 放入 `compat/guzhenren/item/combo/`（新增目录）；原散落的“次能力”迁入此处。
- 激活 ID 命名：`chestcavity:guzhenren/<family>/<organ>/active`
- 冷却键命名：`guzhenren.<family>.<organ>.active|passive|combo.<name>`（被动通常不设冷却，若需要则按该约定）。

#### 迁移流程
1) 库存盘点：`rg` 搜索 `LinkageManager.getContext|getOrCreateChannel|GuzhenrenResourceBridge.open|NBTCharge|MultiCooldown|LedgerOps|ResourceOps`，建立“家族→能力→触发→资源→冷却→DoT/反应→输出”清单。
2) 行为映射：为每个家族确定“唯一主动/唯一被动”，将其余列为 Combo；记录能力 ID、冷却键、资源阈值与反应标签。
3) 分桶重构：按家族/桶迁移（雷/食/丑/酒 → 冰/毒/骨 → 金/怒/变），删除重复钳制/计时/属性清理代码；统一助手调用。
4) 接线统一：
   - 主动：`OrganActivationListeners` 静态注册；客户端热键为字面 ID；网络静默链路。
   - 冷却：集中 `MultiCooldown`；移除手动时间戳；装备/卸下时清理剩余状态。
   - 护盾：`AbsorptionHelper` 恢复与上限；移除自建修饰符；避免堆叠泄漏。
   - 资源/账本：`ResourceOps`/`LedgerOps` 统一调整；禁止直接 `LinkageChannel.adjust`/`ledger.remove`。
   - DoT/反应：按上文规范接线，确保 `typeId` 完整。
5) 验证与回归：
   - 构建：`./gradlew compileJava`；重要更改后 `./gradlew test`。
   - 手测：装备/卸下/能力触发/护盾刷新/DoT 反应链；观察是否出现账本重建、负冷却、护盾残留。
   - 日志：保持静音（INFO 以下），除非显式开启调试开关。
6) 文档与跟踪：更新本文件与家族映射清单；记录每桶的完成与遗留。

#### 分批与优先级
- 第一批：雷/食/丑/酒（现仍存在手动计时/LinkageManager 接口的优先收敛）
- 第二批：冰/毒/骨（含 DoT/残留域/反应规则）
- 第三批：金/怒/变（姿态/护盾/怒气类）

#### 验收标准
- 每家族仅保留 1 主动 + 1 被动，其余通过 Combo 触发。
- 无直接 `LinkageChannel.adjust`/`ledger.remove` 等遗留；计时器集中在 `MultiCooldown`；护盾统一 `AbsorptionHelper`。
- 所有 DoT 均携带 `typeId` 且能触发预期反应；客户端热键仅字面 ID；网络激活静默。
- 装备/卸下不留下账本残留或负冷却；计时器在物品移除与区块重载后仍保持一致。

#### 风险与回滚
- 风险：被动合并后重复触发/叠算；对策：单例被动入口 + 去重与限流。
- 风险：Combo 触发窗口过窄/过宽；对策：`MultiCooldown`+计数阈值与调参开关。
- 回滚：保留原行为入口的最小桥接层（旧→新），支持按家族临时切换与灰度验证。

#### 记录与调试
- 跟踪项：账本重建警告、负冷却、护盾修饰符泄漏；在 `docs/` 或本文件末尾附带迁移检查表。
- 调试开关：保持 DEBUG 默认关闭；必要时提供针对家族的局部开关以便现场诊断。

### 开发新动作
1. 在 `soul/fakeplayer/actions/` 创建动作类
2. 实现 `Action` 接口
3. 在 `ActionRegistry` 中注册
4. 通过 `/soul action start <soul> <action>` 测试

---

## 最佳实践

### 代码规范
1. **使用现有助手**: 优先使用 `LedgerOps`、`ResourceOps`、`MultiCooldown`
2. **避免手动操作**: 不要直接操作 `LinkageManager` 或原始冷却计时器
3. **资源检查**: 所有资源操作都要检查返回值
4. **边界条件**: 明确定义所有边界情况

### 测试规范（新增）
1. **纯逻辑优先**：复杂计算、条件判断抽离为纯函数并编写测试
2. **测试命名**：使用清晰的 `@DisplayName` 注解，描述测试目的
3. **AAA 模式**：Arrange（准备）→ Act（执行）→ Assert（断言）
4. **边界测试**：必须测试边界值和异常情况
5. **重构保护**：重构前先有测试，重构后测试必须通过

**示例测试结构**：
```java
@DisplayName("资源消耗逻辑测试")
class ResourceConsumptionTest {
    @Test
    @DisplayName("资源充足时应成功消耗")
    void testConsumeSuccess() {
        // Arrange
        float available = 100f;
        float cost = 50f;

        // Act
        boolean result = canConsume(available, cost);

        // Assert
        assertTrue(result, "充足资源应可消耗");
    }
}
```

**快速测试**：
- 编译 + 测试：`./gradlew compileJava test`
- 仅运行特定测试：`./gradlew test --tests "ResourceConsumptionTest"`
- 生成覆盖率：`./gradlew test jacocoTestReport`

### 性能考虑
- **慢速tick**: 资源维护和状态同步放在慢速tick中
- **事件驱动**: 命中/伤害等高频事件保持轻量
- **冷却管理**: 使用 `MultiCooldown` 避免重复计时器

### 调试技巧
- 启用调试日志: `-Dchestcavity.debugSoul=true`
- 检查联动通道: `/linkage dump` 命令
- 灵魂状态: `/soul nav dump <id>` 查看导航状态

---

## 常见问题

### Q: 器官装备后没有效果？
A: 检查:
1. 是否正确注册到 `OrganIntegrationSpec`
2. `ensureAttached` 钩子是否设置
3. 联动通道是否创建 (`LedgerOps.ensureChannel`)

### Q: 灵魂假人不执行动作？
A: 检查:
1. 大脑模式是否正确设置
2. 动作是否在 `ActionRegistry` 中注册
3. 冷却时间是否冲突

### Q: 资源消耗没有生效？
A: 检查:
1. 玩家是否有足够资源
2. `ConsumptionResult` 返回值
3. 是否使用了正确的消耗方法（严格vs回退）

### Q: 冷却计时器不正常？
A: 检查:
1. 是否使用 `MultiCooldown` 而非手动计时
2. 冷却键是否唯一
3. 装备/移除时是否清理冷却状态

---

## 迁移指南

### 从旧代码迁移
1. **替换手动计时器** → 使用 `MultiCooldown`
2. **替换直接通道操作** → 使用 `LedgerOps`
3. **替换资源操作** → 使用 `ResourceOps`
4. **替换灵魂行为** → 使用动作系统

### 待迁移项目
- [ ] 雷/食/丑/酒系器官中仍使用手动 `LinkageManager` 的部分
- [ ] 原始冷却计时器迁移到 `MultiCooldown`
- [ ] 吸收提供者验证（冰肌蛊、全勇明蛊等）

---

## 快速参考

### 关键文件
- **主入口**: [`GuzhenrenIntegrationModule.java`](module/GuzhenrenIntegrationModule.java)
- **器官规范**: [`OrganIntegrationSpec.java`](module/OrganIntegrationSpec.java)
- **资源助手**: [`ResourceOps.java`](util/behavior/ResourceOps.java)
- **联动助手**: [`LedgerOps.java`](util/behavior/LedgerOps.java)
- **冷却管理**: [`MultiCooldown.java`](util/behavior/MultiCooldown.java)

### 测试相关文件（新增）
- **测试指南**: `../../../../../../../docs/TESTING.md`
- **编写教程**: `../../../../../../../docs/HOW_TO_WRITE_TESTS.md`
- **测试示例**: `../../../../../../../src/test/java/net/tigereye/chestcavity/examples/SimpleTestExample.java`
- **Mock 工具**: `../../../../../../../src/test/java/net/tigereye/chestcavity/util/mock/`
- **运行脚本**: `../../../../../../../scripts/run-tests.sh`

### 开发工具
```bash
# 构建检查
./gradlew compileJava

# 运行所有测试（新增）
./gradlew test

# 运行特定测试（新增）
./gradlew test --tests "SimpleTestExample"

# 生成测试覆盖率报告（新增）
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html

# 使用测试脚本（新增）
./scripts/run-tests.sh all       # 运行所有测试
./scripts/run-tests.sh unit      # 仅单元测试
./scripts/run-tests.sh coverage  # 生成覆盖率
./scripts/run-tests.sh quick     # 快速检查

# 搜索代码
rg "pattern" src/main/java

# 查看git状态
git status -sb
```

### 调试命令
```bash
# 灵魂导航状态
/soul nav dump <soulName>

# 联动通道状态  
/linkage dump

# 灵魂动作
/soul action start <soul> <action>
```

---

**最后更新**: 2025-10-18  
**维护者**: ChestCavityForge 开发团队
