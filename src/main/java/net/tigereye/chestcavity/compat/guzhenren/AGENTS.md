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

## 开发指南

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

### 开发工具
```bash
# 构建检查
./gradlew compileJava

# 运行测试
./gradlew test

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
