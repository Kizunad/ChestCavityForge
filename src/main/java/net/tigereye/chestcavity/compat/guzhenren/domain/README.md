# 领域系统（Domain System）

通用的领域效果框架，用于实现各种道流派的领域技能。

## 📁 文件结构

```
domain/
├── Domain.java                    # 领域核心接口（含PNG渲染配置）
├── AbstractDomain.java            # 抽象基类（通用实现）
├── DomainManager.java             # 全局管理器（单例）
├── DomainTags.java                # 标签系统（NBT存储）
├── DomainHelper.java              # 辅助工具类
├── client/                        # 【通用】客户端渲染
│   └── DomainRenderer.java        # PNG纹理渲染器（所有领域通用）
├── network/                       # 【通用】网络同步
│   ├── DomainSyncPayload.java     # 领域同步包
│   ├── DomainRemovePayload.java   # 领域移除包
│   └── DomainNetworkHandler.java  # 网络处理工具
├── impl/                          # 具体领域实现
│   └── jianxin/                   # 剑心域
│       ├── JianXinDomain.java     # 主实现类
│       ├── tuning/                # 调参配置
│       │   └── JianXinDomainTuning.java
│       └── fx/                    # 粒子特效
│           └── JianXinDomainFX.java
└── README.md                      # 本文档
```

## 核心组件

### 1. Domain 接口
领域的核心接口，定义了所有领域必须实现的方法：
- `getDomainId()` - 唯一标识
- `getOwner()` / `getOwnerUUID()` - 主人信息
- `getLevel()` / `setLevel()` - 等级系统
- `getCenter()` / `getRadius()` - 位置和范围
- `getDomainType()` - 类型标识（如"jianxin"）
- `isInDomain()` - 判断位置/实体是否在领域内
- `isFriendly()` - 敌友判定
- `tick()` - 每tick更新
- `applyEffects()` - 应用效果到实体
- `isValid()` / `destroy()` - 生命周期管理
- **PNG渲染配置** (新增)：
  - `getTexturePath()` - PNG纹理路径（返回null则不渲染）
  - `getPngHeightOffset()` - 高度偏移（默认20.0）
  - `getPngAlpha()` - 透明度（默认0.5）
  - `getPngRotationSpeed()` - 旋转速度（默认0.5度/tick）

### 2. AbstractDomain 抽象类
提供领域的通用实现：
- 自动位置更新（跟随主人）
- 范围内实体检测
- 敌友判定默认实现
- 生命周期管理

### 3. DomainManager 单例管理器
全局管理所有活跃的领域：
- `registerDomain()` - 注册新领域
- `unregisterDomain()` - 移除领域
- `getDomain()` / `getAllDomains()` - 查询领域
- `getDomainsAt()` - 查询位置的领域
- `getHighestLevelDomainAt()` - 获取最高等级领域
- `tick()` - 更新所有领域（需要从服务端tick事件调用）

### 4. DomainTags 标签系统
为实体附加领域相关标签：
- `hasTag()` / `addTag()` / `removeTag()` - 基础标签操作
- `markEnterSwordDomain()` / `markLeaveSwordDomain()` - 剑域标记
- `grantUnbreakableFocus()` - 赋予无敌焦点
- NBT存储，自动序列化

### 5. DomainHelper 辅助工具
简化领域的创建和使用：
- `createOrGetJianXinDomain()` - 创建/获取剑心域
- `hasActiveJianXinDomain()` - 检查是否有激活的剑心域
- `resolveDomainConflict()` - 处理领域冲突

### 6. DomainRenderer 通用PNG渲染器（client/）
**所有领域类型通用**的PNG纹理渲染器：
- 在领域中心上方指定高度渲染PNG纹理
- 自动缩放到领域半径大小
- 支持透明度和旋转动画
- 客户端数据缓存（ConcurrentHashMap）
- 每个领域只需配置4个方法即可启用PNG渲染

### 7. Domain网络同步系统（network/）
**所有领域类型通用**的网络同步：
- `DomainSyncPayload` - 同步领域位置、大小、纹理等到客户端
- `DomainRemovePayload` - 通知客户端移除领域渲染
- `DomainNetworkHandler` - 向128格范围内玩家广播
- 自动序列化/反序列化，支持任意纹理路径

## 使用示例

### 创建剑心域

```java
// 在器官行为中创建剑心域
Player player = ...;
int jiandaoDaohen = 50;  // 从玩家数据获取
int schoolExperience = 80;

JianXinDomain domain = DomainHelper.createOrGetJianXinDomain(
    player, jiandaoDaohen, schoolExperience);
```

### 更新剑心域强度

```java
JianXinDomain domain = DomainHelper.getJianXinDomain(player);
if (domain != null) {
    // 更新剑道数值
    domain.updateJiandaoStats(newDaohen, newSchoolExp);

    // 触发强化状态（定心返本）
    domain.triggerEnhancement();
}
```

### 移除剑心域

```java
// 退出冥想时移除领域
DomainHelper.removeJianXinDomain(player);
```

### 服务端Tick集成

```java
// 在服务端tick事件中更新所有领域
@SubscribeEvent
public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
        for (ServerLevel level : server.getAllLevels()) {
            DomainManager.getInstance().tick(level);
        }
    }
}
```

### 检查实体是否在领域内

```java
LivingEntity entity = ...;

// 检查是否在任意领域内
if (DomainHelper.isInAnyDomain(entity)) {
    // 实体在某个领域中
}

// 获取实体所在的最高等级领域
Domain highestDomain = DomainHelper.getHighestLevelDomainAt(entity);
if (highestDomain != null) {
    int level = highestDomain.getLevel();
    String type = highestDomain.getDomainType();
}

// 使用标签系统检查
if (DomainTags.isInSwordDomain(entity)) {
    UUID owner = DomainTags.getSwordDomainOwner(entity);
    int level = DomainTags.getSwordDomainLevel(entity);
}
```

## 为新领域添加PNG渲染

任何领域都可以轻松启用PNG渲染，只需4步：

### 1. 准备PNG纹理
将PNG文件放在资源包中：
```
src/main/resources/assets/你的命名空间/textures/domain/你的纹理.png
```

### 2. 在领域类中覆盖方法
```java
@Override
public ResourceLocation getTexturePath() {
    return ResourceLocation.fromNamespaceAndPath(
        "你的命名空间", "textures/domain/你的纹理.png");
}

@Override
public double getPngHeightOffset() {
    return 20.0; // 可自定义高度
}

@Override
public float getPngAlpha() {
    return 0.6f; // 可自定义透明度
}

@Override
public float getPngRotationSpeed() {
    return 1.0f; // 可自定义旋转速度
}
```

### 3. 无需其他代码！
通用的`DomainRenderer`和网络同步系统会自动：
- ✅ 在客户端渲染PNG纹理
- ✅ 每秒同步领域数据
- ✅ 处理领域创建和销毁
- ✅ 缩放到正确大小
- ✅ 添加旋转动画

### 4. 可选：动态配置
你可以根据领域状态动态调整渲染：
```java
@Override
public float getPngAlpha() {
    // 根据领域等级调整透明度
    return 0.3f + (getLevel() * 0.1f);
}

@Override
public ResourceLocation getTexturePath() {
    // 根据状态切换纹理
    return isEnhanced()
        ? ResourceLocation.parse("my:enhanced_texture.png")
        : ResourceLocation.parse("my:normal_texture.png");
}
```

## 实现新的领域类型

### 1. 创建目录结构

```
domain/impl/my_custom_domain/
├── MyCustomDomain.java
├── tuning/
│   └── MyCustomDomainTuning.java
└── fx/
    └── MyCustomDomainFX.java
```

### 2. 创建Tuning配置类

```java
package net.tigereye.chestcavity.compat.guzhenren.domain.impl.my_custom_domain.tuning;

public final class MyCustomDomainTuning {
    private MyCustomDomainTuning() {}

    public static final double BASE_RADIUS = 8.0;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;
    // ... 其他配置参数
}
```

### 3. 创建FX粒子特效类

```java
package net.tigereye.chestcavity.compat.guzhenren.domain.impl.my_custom_domain.fx;

public final class MyCustomDomainFX {
    private MyCustomDomainFX() {}

    public static void spawnBorderParticles(ServerLevel level, MyCustomDomain domain) {
        // 边界粒子效果
    }

    public static void spawnCreationEffect(ServerLevel level, Vec3 center, double radius) {
        // 创建特效
    }

    // ... 其他特效方法
}
```

### 4. 实现领域主类

```java
package net.tigereye.chestcavity.compat.guzhenren.domain.impl.my_custom_domain;

import net.tigereye.chestcavity.compat.guzhenren.domain.AbstractDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.my_custom_domain.tuning.MyCustomDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.my_custom_domain.fx.MyCustomDomainFX;

public class MyCustomDomain extends AbstractDomain {

    public static final String TYPE = "my_custom_domain";

    public MyCustomDomain(LivingEntity owner, Vec3 center, int level) {
        super(owner, center, level);
    }

    @Override
    public String getDomainType() {
        return TYPE;
    }

    @Override
    public double getRadius() {
        return MyCustomDomainTuning.BASE_RADIUS;
    }

    @Override
    public void tick(ServerLevel level) {
        super.tick(level);
        // 粒子特效
        MyCustomDomainFX.spawnBorderParticles(level, this);
    }

    @Override
    public void applyEffects(ServerLevel level, LivingEntity entity, boolean isFriendly) {
        if (isFriendly) {
            // 友方增益
        } else {
            // 敌方减益
        }
    }
}
```

### 5. 注册到管理器

```java
MyCustomDomain domain = new MyCustomDomain(owner, center, level);
DomainManager.getInstance().registerDomain(domain);
```

## 剑心域（JianXinDomain）

### 调参配置 (tuning/)

所有数值参数集中在 `JianXinDomainTuning.java`：
- **基础属性**: 半径、等级范围
- **友方效果**: 资源恢复速率、强化倍数
- **敌方效果**: 速度减慢、攻击减慢
- **实力判定**: 道痕权重、经验权重
- **剑气反噬**: 基础伤害、硬直时间
- **粒子特效**: 生成频率、密度

### 粒子特效 (fx/)

所有**粒子**视觉效果在 `JianXinDomainFX.java`：
- `spawnBorderParticles()` - 边界光环（青色火焰）
- `spawnCenterParticles()` - 中心能量涌动
- `spawnCreationEffect()` - 创建特效
- `spawnDestructionEffect()` - 销毁特效
- `spawnEnhancementEffect()` - 强化状态特效
- `spawnCounterAttackEffect()` - 剑气反噬特效
- `tickDomainEffects()` - 每tick更新

### PNG渲染配置（覆盖Domain接口方法）

剑心域的PNG渲染由**通用系统**处理，只需在JianXinDomain中配置：
```java
@Override
public ResourceLocation getTexturePath() {
    return ResourceLocation.fromNamespaceAndPath(
        "guzhenren", "textures/domain/jianxinyu_transparent_soft_preview.png");
}

@Override
public double getPngHeightOffset() {
    return 20.0; // 领域中心上方20格
}

@Override
public float getPngAlpha() {
    return enhanced ? 0.8f : 0.5f; // 强化状态下更不透明
}

@Override
public float getPngRotationSpeed() {
    return 0.5f; // 缓慢旋转
}
```

配置后，通用渲染器会自动：
- 在领域中心上方20格渲染PNG
- 自动缩放到领域半径（5.0格）
- 根据强化状态调整透明度
- 添加缓慢旋转动画
- 每秒同步一次到客户端

### 主要特性

**友方（同队伍）：**
- 资源恢复（真元/精力/念头）
- 剑势层数恢复

**敌方：**
- 移动速度减慢（15%）
- 攻击速度减慢（10%）
- 剑气反噬（剑修实力判定）

**强化状态（定心返本）：**
- 域内效果翻倍
- 持续2秒
- 白色发光粒子

### 标签系统
- `sword_domain` - 剑域标记
- `sword_domain_owner` - 主人UUID
- `sword_domain_source` - 来源标识"jianxin"
- `sword_domain_level` - 领域等级
- `in_sword_domain` - 实体在剑域中
- `unbreakable_focus` - 无视打断

## 领域冲突处理

当多个领域重叠时：
- 使用 `DomainManager.getHighestLevelDomainAt()` 获取最高等级领域
- 高等级领域压制低等级
- 同等级则双方效果都生效

## 性能优化

1. **弱引用缓存主人**
   ```java
   private final WeakReference<LivingEntity> ownerRef;
   ```

2. **粒子特效频率控制**
   ```java
   if (tickCount % JianXinDomainTuning.BORDER_PARTICLE_INTERVAL == 0) {
       // 生成粒子
   }
   ```

3. **自动清理无效领域**
   ```java
   DomainManager 自动在 tick() 中移除无效领域
   ```

## TODO 集成项

当前实现的占位符功能，需要与现有系统集成：

1. **资源系统集成**
   - 真元/精力/念头恢复
   - 剑势层数管理

2. **剑道数据集成**
   - 从玩家数据获取道痕和流派经验
   - LinkageChannel "jianxin" 存储

3. **剑气反噬完整实现**
   - 检测 useItem 事件
   - 判断物品是否有 `guzhenren:jiandao` 标签
   - 取消useItem并触发反噬

4. **冥想状态管理**
   - 冥想buff效果
   - 攻击打断机制（检查`unbreakable_focus`）
   - 失心惩罚（剑势冻结）

5. **服务端Tick注册**
   - 在合适的事件中调用 `DomainManager.tick()`

## 设计模式

- **接口/抽象类** - 领域核心定义
- **单例模式** - DomainManager全局管理
- **策略模式** - 不同领域类型的效果实现
- **观察者模式** - 标签系统监听实体状态
- **工厂模式** - DomainHelper创建领域
- **弱引用** - 避免内存泄漏

## 扩展性

框架支持任意类型的领域：
- 剑心域（JianXinDomain）- 已实现
- 魂域（HunDomain）- 待实现
- 毒域（DuDomain）- 待实现
- 其他道流派领域 - 按需扩展

每个领域类型独立目录，包含：
- 主实现类
- tuning/ 调参配置
- fx/ 粒子特效

---

**框架版本**: 1.0
**最后更新**: 2025-01
**依赖**: Minecraft NeoForge 1.21.1
