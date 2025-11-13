# Capabilities API 集成方案

## 问题陈述

**现状**:
- `PersistentGuCultivatorClone` 实体提供了 `getInventory()` 公共方法访问其 7-slot `ItemStackHandler`
- `Entity.getCapability()` 在 NeoForge 1.21.1 中是 **final 方法**,无法覆盖
- 现有代码使用 `target.getCapability(ItemHandler.ENTITY, null)` 获取实体物品栏能力
- 外部代码期望通过标准Capability API访问实体物品栏,而不是直接调用方法

**目标**:
为 `PersistentGuCultivatorClone` 实体注册 ItemHandler capability,使外部代码可以通过标准API访问其物品栏。

---

## 技术分析

### 1. NeoForge 1.21.1 Capabilities API 架构

#### 1.1 能力类型系统

NeoForge 提供三种能力类型:
```java
// 方块能力 (带方向上下文)
BlockCapability<T, C>

// 实体能力 (带可选上下文)
EntityCapability<T, C>

// 物品能力 (ItemAccess上下文)
ItemCapability<T, C>
```

#### 1.2 官方提供的内置能力

**位置**: `net.neoforged.neoforge.capabilities.Capabilities`

```java
public static final class Item {
    // 实体物品栏能力 (无方向/上下文)
    public static final EntityCapability<ResourceHandler<ItemResource>, @Nullable Void> ENTITY
        = EntityCapability.createVoid(create("item_handler"), ResourceHandler.asClass());

    // 实体物品栏自动化能力 (带方向,用于漏斗等)
    public static final EntityCapability<ResourceHandler<ItemResource>, @Nullable Direction> ENTITY_AUTOMATION
        = EntityCapability.createSided(create("item_handler_automation"), ResourceHandler.asClass());
}
```

**关键发现**:
- ✅ 使用的键是 `Capabilities.Item.ENTITY` (不是 `Capabilities.ItemHandler.ENTITY`)
- ✅ 返回类型是 `ResourceHandler<ItemResource>` (不是 `IItemHandlerModifiable`)
- ⚠️ 现有代码中的 `ItemHandler.ENTITY` 可能是旧版本遗留的命名或错误导入

#### 1.3 能力注册机制

**注册事件**: `RegisterCapabilitiesEvent` (Mod总线事件)

**注册方法**:
```java
public <T, C extends @Nullable Object, E extends Entity>
void registerEntity(
    EntityCapability<T, C> capability,   // 能力键
    EntityType<E> entityType,            // 实体类型
    ICapabilityProvider<? super E, C, T> provider  // 能力提供器
)
```

**Provider函数签名**:
```java
@FunctionalInterface
interface ICapabilityProvider<T, C, R> {
    @Nullable R getCapability(T object, C context);
}
```

### 2. 原版实体能力注册实例

**位置**: `CapabilityHooks.java:153-168`

```java
public static void registerFallbackVanillaProviders(RegisterCapabilitiesEvent event) {
    // 为所有实体类型注册fallback能力提供器
    for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
        event.registerEntity(Capabilities.Item.ENTITY, entityType, (entity, ctx) -> {
            if (entity instanceof AbstractHorse horse)
                return VanillaContainerWrapper.of(horse.getInventory());
            else if (entity instanceof LivingEntity livingEntity) {
                var handsWrapper = LivingEntityEquipmentWrapper.of(livingEntity, EquipmentSlot.Type.HAND);
                var armorWrapper = LivingEntityEquipmentWrapper.of(livingEntity, EquipmentSlot.Type.HUMANOID_ARMOR);
                return new CombinedResourceHandler<>(handsWrapper, armorWrapper);
            }
            return null;
        });
    }
}
```

**关键观察**:
1. **返回 `ResourceHandler<ItemResource>`**: 不是旧API的 `IItemHandlerModifiable`
2. **使用Wrapper包装原生容器**: `VanillaContainerWrapper.of()` 包装原版Container接口
3. **条件性返回**: 使用 `instanceof` 检查,不匹配时返回 `null`

### 3. 新旧API对比

| 方面 | 旧API (1.20.x及更早) | 新API (NeoForge 1.21.1) |
|------|---------------------|------------------------|
| 能力键命名 | `Capabilities.ItemHandler.ENTITY` | `Capabilities.Item.ENTITY` |
| 返回类型 | `IItemHandler` / `IItemHandlerModifiable` | `ResourceHandler<ItemResource>` |
| 注册方式 | 覆盖 `Entity.getCapability()` | `RegisterCapabilitiesEvent.registerEntity()` |
| 包装工具 | `InvWrapper` | `VanillaContainerWrapper` |

---

## 实施方案

### 方案A: 使用 VanillaContainerWrapper (推荐)

#### 原理
`ItemStackHandler` 实现了 `IItemHandler` 接口,NeoForge 提供 `VanillaContainerWrapper.of()` 可以包装实现了Container接口的对象。虽然 `ItemStackHandler` 不直接实现 `Container`,但可以通过适配器模式包装。

#### 实施步骤

**步骤 1**: 在 `PersistentGuCultivatorClone` 中添加 Container 适配器

**文件**: `PersistentGuCultivatorClone.java`

**位置**: 在 `getInventory()` 方法后添加

```java
/**
 * 获取物品栏 (用于 AI 访问和外部代码访问)
 */
public ItemStackHandler getInventory() {
    return inventory;
}

/**
 * 获取物品栏的 Container 视图 (用于 Capability 包装)
 *
 * <p>NeoForge 1.21.1 能力系统需要 ResourceHandler<ItemResource> 类型,
 * 通过 VanillaContainerWrapper 包装 Container 接口可以桥接到新API。
 */
public net.minecraft.world.Container getContainerView() {
    return new net.minecraft.world.SimpleContainer(inventory.getSlots()) {
        @Override
        public int getContainerSize() {
            return inventory.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return inventory.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            // 通知变化 (可选,用于同步)
        }

        @Override
        public boolean stillValid(Player player) {
            return PersistentGuCultivatorClone.this.isOwnedBy(player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < inventory.getSlots(); i++) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    };
}
```

**步骤 2**: 在主Mod类中注册能力

**文件**: `ChestCavity.java`

**位置**: 在构造函数中添加监听器 (约78行 `public ChestCavity(IEventBus modEventBus)` 内)

```java
public ChestCavity(IEventBus modEventBus) {
    // ... 现有代码 ...

    bus.addListener(this::registerCapabilities);  // 添加这一行

    // ... 现有代码 ...
}

/**
 * 注册自定义实体能力
 */
private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
    // 为 PersistentGuCultivatorClone 注册 ItemHandler 能力
    event.registerEntity(
        net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY,
        CCEntities.PERSISTENT_GU_CULTIVATOR_CLONE.get(),
        (entity, context) -> {
            if (entity instanceof net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone) {
                return net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(clone.getContainerView());
            }
            return null;
        }
    );
}
```

**优点**:
- ✅ 遵循NeoForge官方模式 (与CapabilityHooks.java中原版实体一致)
- ✅ 使用官方包装工具,兼容性最佳
- ✅ 返回正确的 `ResourceHandler<ItemResource>` 类型

**缺点**:
- ⚠️ 需要实现完整的 `Container` 适配器 (约50行代码)
- ⚠️ 多一层间接调用 (但性能影响可忽略)

---

### 方案B: 自定义 ResourceHandler 实现 (备选)

如果方案A的Container适配器过于复杂,可以直接实现 `ResourceHandler<ItemResource>` 接口。

#### 实施步骤

**步骤 1**: 创建 ItemStackHandler 的 ResourceHandler 适配器

**新建文件**: `ItemStackHandlerResourceAdapter.java`

**位置**: `net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity`

```java
package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * 将 ItemStackHandler 适配为 ResourceHandler<ItemResource>
 *
 * <p>用于桥接旧的 IItemHandler API 和新的 ResourceHandler API。
 */
public class ItemStackHandlerResourceAdapter implements ResourceHandler<ItemResource> {
    private final ItemStackHandler handler;

    public ItemStackHandlerResourceAdapter(ItemStackHandler handler) {
        this.handler = handler;
    }

    @Override
    public long insert(@NotNull ItemResource resource, long maxAmount, boolean simulate) {
        ItemStack stack = resource.toStack((int) maxAmount);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack remaining = handler.insertItem(i, stack, simulate);
            if (remaining.isEmpty()) {
                return maxAmount;
            }
            stack = remaining;
        }
        return maxAmount - stack.getCount();
    }

    @Override
    public long extract(@NotNull ItemResource resource, long maxAmount, boolean simulate) {
        long extracted = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (resource.matches(slotStack)) {
                int toExtract = (int) Math.min(maxAmount - extracted, slotStack.getCount());
                ItemStack extractedStack = handler.extractItem(i, toExtract, simulate);
                extracted += extractedStack.getCount();
                if (extracted >= maxAmount) {
                    break;
                }
            }
        }
        return extracted;
    }

    @Override
    public @NotNull Iterator<ItemResource> iterator() {
        // 返回所有非空槽位的迭代器
        return new Iterator<>() {
            private int currentSlot = 0;

            @Override
            public boolean hasNext() {
                while (currentSlot < handler.getSlots()) {
                    if (!handler.getStackInSlot(currentSlot).isEmpty()) {
                        return true;
                    }
                    currentSlot++;
                }
                return false;
            }

            @Override
            public ItemResource next() {
                ItemStack stack = handler.getStackInSlot(currentSlot++);
                return ItemResource.of(stack);
            }
        };
    }
}
```

**步骤 2**: 在 PersistentGuCultivatorClone 中添加方法

```java
/**
 * 获取物品栏的 ResourceHandler 视图 (用于 Capability)
 */
public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>
    getResourceHandler() {
    return new ItemStackHandlerResourceAdapter(inventory);
}
```

**步骤 3**: 在主Mod类中注册能力 (与方案A相同,但调用不同方法)

```java
private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
    event.registerEntity(
        net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY,
        CCEntities.PERSISTENT_GU_CULTIVATOR_CLONE.get(),
        (entity, context) -> {
            if (entity instanceof net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone) {
                return clone.getResourceHandler();  // 直接返回 ResourceHandler
            }
            return null;
        }
    );
}
```

**优点**:
- ✅ 更直接,无需Container适配层
- ✅ 完全控制insert/extract逻辑

**缺点**:
- ⚠️ 需要额外实现一个新类 (~80行代码)
- ⚠️ 需要正确实现 `ResourceHandler` 的语义 (insert/extract/iterator)
- ⚠️ 可能与NeoForge未来版本不完全兼容

---

## 推荐方案与时间线

### 推荐: **方案A (VanillaContainerWrapper)**

**理由**:
1. 遵循NeoForge官方模式,兼容性和未来支持最佳
2. 利用官方包装工具,减少潜在bug
3. 代码量适中 (约50行Container适配器 + 10行注册代码)

### 实施时间线

**阶段3: UI系统 + Capabilities实现** (当前阶段延后部分)

| 任务 | 文件 | 预计代码量 | 优先级 |
|------|------|----------|--------|
| 添加 `getContainerView()` 方法 | `PersistentGuCultivatorClone.java` | ~50 lines | P0 (必须) |
| 添加 `registerCapabilities()` 方法 | `ChestCavity.java` | ~15 lines | P0 (必须) |
| 测试能力访问 | 单元测试或手动测试 | N/A | P0 (必须) |
| (可选) 修正现有代码中的 `ItemHandler.ENTITY` 引用 | `SpawnHostileGuCultivatorAction.java` | ~5 lines | P1 (建议) |

**总预计**: ~65-70行新增代码

---

## 兼容性注意事项

### 1. 现有代码兼容性

**问题**: `SpawnHostileGuCultivatorAction.java:432` 使用:
```java
Object cap = target.getCapability(ItemHandler.ENTITY, null);
```

**解决方案**:
1. **临时兼容**: 如果 `ItemHandler.ENTITY` 是typo或旧导入,应改为:
   ```java
   Object cap = target.getCapability(Capabilities.Item.ENTITY, null);
   ```

2. **类型转换**: 新API返回 `ResourceHandler<ItemResource>`,不是 `IItemHandlerModifiable`
   - **选项A**: 保持现有逻辑,直接调用 `getInventory()` 方法 (对于已知类型)
   - **选项B**: 实现 `ResourceHandler` 到 `IItemHandler` 的反向适配器 (复杂,不推荐)

**推荐做法**:
```java
// 对于 PersistentGuCultivatorClone,直接使用类型检查
if (target instanceof PersistentGuCultivatorClone clone) {
    ItemStackHandler handler = clone.getInventory();
    // 使用 handler...
} else {
    // 对其他实体,尝试Capability
    var cap = target.getCapability(Capabilities.Item.ENTITY, null);
    if (cap instanceof ResourceHandler<ItemResource> resourceHandler) {
        // 使用新API...
    }
}
```

### 2. 第三方Mod兼容性

已注册的能力会被第三方mod自动识别,无需额外处理。例如:
- 漏斗/投掷器会使用 `ENTITY_AUTOMATION` (如需要,可额外注册)
- 其他蛊真人mod代码通过标准API访问

---

## 验收标准

实施完成后,应满足:

1. ✅ `PersistentGuCultivatorClone` 实体可通过 `entity.getCapability(Capabilities.Item.ENTITY, null)` 访问物品栏
2. ✅ 返回的能力类型为 `ResourceHandler<ItemResource>`
3. ✅ 现有 `getInventory()` 方法保持可用 (向后兼容)
4. ✅ 外部代码 (如其他mod) 可以通过标准Capability API与分身物品栏交互
5. ✅ 单元测试或集成测试验证能力注册成功

---

## 附录: 参考资料

### A. 关键源码位置

| 文件 | 路径 | 关键内容 |
|------|------|---------|
| RegisterCapabilitiesEvent | `NeoForge/src/.../capabilities/RegisterCapabilitiesEvent.java` | 注册API定义 |
| CapabilityHooks | `NeoForge/src/.../capabilities/CapabilityHooks.java` | 原版实体注册示例 (line 153-168) |
| Capabilities | `NeoForge/src/.../capabilities/Capabilities.java` | 内置能力定义 |
| VanillaContainerWrapper | `NeoForge/src/.../transfer/item/VanillaContainerWrapper.java` | 官方Container包装器 |

### B. 相关文档

- NeoForge官方文档: https://docs.neoforged.net/docs/datastorage/capabilities/
- Capability Rework公告: https://neoforged.net/news/20.3capability-rework/

---

**文档版本**: v1.0
**创建日期**: 2025-11-14
**作者**: Claude (Sonnet 4.5)
**状态**: 待审核
