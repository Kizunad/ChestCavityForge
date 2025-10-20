# ChestCavity Util 目录文档

本目录包含 ChestCavity 模组的核心工具类，为器官系统、伤害计算、反应系统等提供基础支持。

## 目录结构

```
util/
├── math/                    # 数学工具类
│   └── CurveUtil.java       # 曲线计算工具
├── reaction/                # 反应系统
│   ├── engine/              # 反应引擎
│   ├── tag/                 # 反应标签系统
│   └── ReactionRegistry.java # 反应注册表
├── retention/               # 器官保留规则
│   └── OrganRetentionRules.java
└── *.java                   # 主要工具类文件
```

## 核心工具类

### [`ChestCavityUtil.java`](ChestCavityUtil.java)

**功能**: 胸腔系统核心工具类，处理器官评分计算、伤害应用、呼吸系统等核心逻辑。

**主要方法**:
- [`runWithOrganHeal()`](ChestCavityUtil.java:50) - 器官治疗防护机制
- [`applyBoneDefense()`](ChestCavityUtil.java:74) - 骨骼防御计算
- [`applyBreathInWater()`](ChestCavityUtil.java:79) - 水下呼吸处理
- [`applyDefenses()`](ChestCavityUtil.java:192) - 综合防御应用
- [`evaluateChestCavity()`](ChestCavityUtil.java:457) - 胸腔系统评估

**使用场景**: 器官效果计算、伤害减免、呼吸系统、消化系统等核心功能。

### [`DoTManager.java`](DoTManager.java)

**功能**: 持续伤害(Damage over Time)管理器，处理周期性伤害调度和执行。

**主要特性**:
- 通过 `TickEngineHub` 注册服务器 Post tick 执行，解耦事件总线
- 基于服务器tick的伤害调度
- 支持音效和粒子效果
- 伤害聚合优化性能
- 与反应系统集成

**核心方法**:
- [`schedulePerSecond()`](DoTManager.java:126) - 每秒伤害调度
- [`cancelAttacker()`](DoTManager.java:180) - 取消攻击者的DoT
- [`getPendingForTarget()`](DoTManager.java:59) - 获取目标待执行DoT
- [`bootstrap()`](DoTManager.java:96) - 将 DoT 系统注册到统一 tick 枢纽

### [`TickEngineHub.java`](engine/TickEngineHub.java)

**功能**: 统一的服务器 Post tick 调度枢纽，按优先级串联各个逻辑引擎。

**主要特性**:
- 提供 `register(priority, engine)` 注册接口，优先级越小越先执行
- 默认优先级常量：`PRIORITY_REACTION`、`PRIORITY_DOT`
- 首次注册时自动绑定 `ServerTickEvent.Post`

**使用示例**:
- `ReactionRegistry.bootstrap()` 注册优先级 100
- `DoTManager.bootstrap()` 注册优先级 200

### [`DoTTypes.java`](DoTTypes.java)

**功能**: DoT类型常量定义，统一管理所有持续伤害类型。

**定义的DoT类型**:
- `YAN_DAO_HUO_YI_AURA` - 焰道火衣光环
- `HUN_DAO_SOUL_FLAME` - 魂道魂焰
- `SHUANG_XI_FROSTBITE` - 霜息冻伤
- `YIN_YUN_CORROSION` - 阴云腐蚀

### [`AbsorptionHelper.java`](AbsorptionHelper.java)

**功能**: 吸收护盾辅助工具，安全地管理吸收容量和护盾值。

**核心方法**:
- [`applyAbsorption()`](AbsorptionHelper.java:29) - 应用吸收护盾
- [`clearAbsorptionCapacity()`](AbsorptionHelper.java:63) - 清除吸收容量

**特性**: 自动属性管理、容量钳制、只增模式支持。

### [`NBTCharge.java`](NBTCharge.java)

**功能**: NBT电荷值读写工具，统一管理物品的电荷状态。

**核心方法**:
- [`getCharge()`](NBTCharge.java:37) - 读取电荷值
- [`setCharge()`](NBTCharge.java:56) - 设置电荷值

### [`NBTWriter.java`](NBTWriter.java)

**功能**: NBT数据写入工具，提供安全的NBT操作接口。

**核心方法**:
- [`updateCustomData()`](NBTWriter.java:22) - 更新自定义数据
- [`updateState()`](NBTWriter.java:37) - 更新状态数据

## 数学工具

### [`MathUtil.java`](MathUtil.java)

**功能**: 基础数学工具，提供水平距离计算等常用数学函数。

### [`math/CurveUtil.java`](math/CurveUtil.java)

**功能**: 曲线计算工具，支持指数逼近和钳制计算。

**核心方法**:
- [`expApproach()`](math/CurveUtil.java:7) - 指数逼近计算
- [`clamp()`](math/CurveUtil.java:16) - 数值钳制

## 反应系统

### [`reaction/ReactionRegistry.java`](reaction/ReactionRegistry.java)

**功能**: 反应系统注册表，管理DoT类型与实体状态之间的化学反应规则。

**核心特性**:
- 多元素反应规则（火、冰、魂、腐蚀等）
- 状态标签系统集成
- 伤害取消和转换机制
- 免疫时间管理

**注册的规则类型**:
- 智慧道规则（魂焰×智慧标记）
- 火系通用规则（点燃刷新、过热爆炸）
- 土心烟云规则（土墙庇护、石火反震）
- 毒道规则（毒燃闪爆、凝霜毒晶）
- 骨道规则（骨蚀裂、骨煅灼）

### [`reaction/tag/`](reaction/tag/) 目录

**功能**: 反应标签系统，提供便携化的状态标记机制。

**核心文件**:
- [`ReactionTagKeys.java`](reaction/tag/ReactionTagKeys.java) - 标签键常量
- [`ReactionTagOps.java`](reaction/tag/ReactionTagOps.java) - 标签操作
- [`ReactionTagAliases.java`](reaction/tag/ReactionTagAliases.java) - 标签别名

## 网络与客户端工具

### [`NetworkUtil.java`](NetworkUtil.java)

**功能**: 网络同步工具，处理客户端与服务器之间的胸腔数据同步。

**核心方法**:
- [`SendS2CChestCavityUpdatePacket()`](NetworkUtil.java:16) - 发送胸腔更新包
- [`sendOrganSlotUpdate()`](NetworkUtil.java:33) - 发送器官槽更新

### [`ClientOrganUtil.java`](ClientOrganUtil.java)

**功能**: 客户端器官工具，处理器官品质显示和兼容性检查。

**核心方法**:
- [`displayOrganQuality()`](ClientOrganUtil.java:29) - 显示器官品质
- [`displayCompatibility()`](ClientOrganUtil.java:68) - 显示兼容性

## 其他工具类

### [`CombatUtil.java`](CombatUtil.java)

**功能**: 战斗工具类，计算威胁等级和战斗状态。

**核心方法**:
- [`calculateThreatLevel()`](CombatUtil.java:16) - 计算威胁等级
- [`isInCombat()`](CombatUtil.java:49) - 检查战斗状态

### [`CommonOrganUtil.java`](CommonOrganUtil.java)

**功能**: 通用器官工具，提供传送、粒子效果等常用功能。

**核心方法**:
- [`teleportRandomly()`](CommonOrganUtil.java:23) - 随机传送
- [`spawnParticles()`](CommonOrganUtil.java:77) - 生成粒子

### [`OrganDataPacketHelper.java`](OrganDataPacketHelper.java)

**功能**: 器官数据包辅助工具，处理网络数据序列化。

### [`retention/OrganRetentionRules.java`](retention/OrganRetentionRules.java)

**功能**: 器官保留规则，管理死亡时保留的器官物品。

**核心方法**:
- [`registerItem()`](retention/OrganRetentionRules.java:23) - 注册保留物品
- [`shouldRetain()`](retention/OrganRetentionRules.java:35) - 检查是否保留

## 辅助工具类

### [`AnimationPathHelper.java`](AnimationPathHelper.java)
**功能**: 动画路径辅助工具

### [`EntitySpawnUtil.java`](EntitySpawnUtil.java)
**功能**: 实体生成工具

### [`Pair.java`](Pair.java)
**功能**: 键值对数据结构

### [`ProjectileParameterReceiver.java`](ProjectileParameterReceiver.java)
**功能**: 抛射体参数接收器

### [`ScoreboardUpgradeManager.java`](ScoreboardUpgradeManager.java)
**功能**: 计分板升级管理器

### [`SingleplayerTickController.java`](SingleplayerTickController.java)
**功能**: 单人游戏tick控制器

## 设计原则

1. **单一职责**: 每个工具类专注于特定领域
2. **无状态设计**: 大多数工具类为静态方法，避免状态管理
3. **防御性编程**: 严格的空值检查和边界条件处理
4. **性能优化**: 使用聚合和缓存优化高频操作
5. **模块化**: 清晰的目录结构，便于维护和扩展

## 使用建议

- 优先使用现有的工具类，避免重复实现
- 遵循工具类的设计模式，保持代码一致性
- 在添加新功能时，考虑是否适合现有工具类或需要新建
- 注意线程安全性，特别是在服务器环境中
