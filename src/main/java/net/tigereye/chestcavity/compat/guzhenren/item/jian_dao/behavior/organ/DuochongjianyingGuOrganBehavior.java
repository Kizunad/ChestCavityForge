package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.util.NBTWriter;

/**
 * 多重剑影蛊器官行为
 *
 * <p>实现分身召唤/召回功能，通过器官系统触发。
 *
 * <p><strong>架构迁移说明 (2025-11-14):</strong>
 * <ul>
 *   <li>原方式: DuochongjianyingGuItem (物品模式 - 右键物品触发)
 *   <li>新方式: 器官模式 - 将蛊虫放入胸腔后通过主动技能触发
 *   <li>物品ID: guzhenren:duochongjianying (外部引用)
 *   <li>状态存储: 使用器官ItemStack的CustomData替代独立物品NBT
 * </ul>
 *
 * <p><strong>功能:</strong>
 * <ul>
 *   <li>主动技能（普通触发）: 召唤/召回分身
 *   <li>主动技能（Shift触发）: 打开分身界面
 *   <li>所有权绑定: 首次使用自动绑定玩家
 *   <li>跨维度支持: 惰性清理机制
 * </ul>
 *
 * @see PersistentGuCultivatorClone
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu
 */
public enum DuochongjianyingGuOrganBehavior {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "duochongjianying");

  /**
   * 主动技能ID: 多重剑影蛊分身
   */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "duochongjianying_fenshen");

  private static final String STATE_ROOT = "DuochongjianyingGu";
  private static final String OWNER_UUID_KEY = "OwnerUUID";
  private static final String CLONE_UUID_KEY = "CloneUUID";
  private static final String DIMENSION_KEY = "DimensionKey";
  private static final String CLONE_DATA_KEY = "CloneData";

  static {
    // 注册主动技能激活监听器
    OrganActivationListeners.register(ABILITY_ID, DuochongjianyingGuOrganBehavior::activateAbility);
  }

  /**
   * 主动技能激活回调
   *
   * <p>触发条件:
   * <ul>
   *   <li>玩家拥有多重剑影蛊器官
   *   <li>玩家手动触发主动技能（按键绑定或其他机制）
   * </ul>
   *
   * <p>行为:
   * <ul>
   *   <li>Shift + 触发: 打开分身界面
   *   <li>普通触发: 召唤/召回分身
   * </ul>
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }

    // 1. 查找器官ItemStack
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      player.displayClientMessage(Component.literal("§c未找到多重剑影蛊!"), true);
      return;
    }

    // 2. 所有权校验
    if (!isOwnedBy(organ, player)) {
      player.displayClientMessage(Component.literal("§c这不是你的蛊虫!"), true);
      return;
    }

    // 3. 检查玩家是否按下Shift键
    if (player.isShiftKeyDown()) {
      // 打开分身界面
      handleOpenInventory(player, organ);
    } else {
      // 召唤/召回分身
      handleSummonOrRecall(player, organ);
    }
  }

  /**
   * 确保器官附加时的初始化
   *
   * <p>当器官首次放入胸腔时调用。
   */
  public void ensureAttached(ChestCavityInstance cc, ItemStack organ) {
    if (organ.isEmpty() || cc == null) {
      return;
    }

    // 如果没有所有者UUID，在这里不自动绑定
    // 等待玩家首次激活时再绑定
  }

  // ============ 核心逻辑方法 (从 DuochongjianyingGuItem 迁移) ============

  /**
   * 处理打开分身界面
   *
   * <p>验证：
   * - 分身必须存在且存活
   * - 距离不超过10格
   */
  private static void handleOpenInventory(ServerPlayer player, ItemStack organ) {
    // 1. 检查分身是否存在
    PersistentGuCultivatorClone clone = findClone(player.serverLevel(), organ);

    if (clone == null) {
      player.displayClientMessage(
          Component.literal("§c分身未召唤或已死亡!"),
          true
      );
      return;
    }

    // 2. 检查距离 (最大10格)
    if (player.distanceToSqr(clone) > 100.0) {
      player.displayClientMessage(
          Component.literal("§c分身距离太远!"),
          true
      );
      return;
    }

    // 3. 打开界面
    clone.openInventoryMenu(player);
  }

  /**
   * 处理召唤或召回分身
   *
   * <p>逻辑：
   * - 分身存在 -> 召回（保存数据到器官NBT）
   * - 分身不存在 -> 召唤（从器官NBT恢复数据）
   *
   * <p>P1修复: 跨维度清理
   * - 在召唤新分身前,必须先清理旧分身 (防止资源泄漏)
   */
  private static void handleSummonOrRecall(ServerPlayer player, ItemStack organ) {
    ServerLevel level = player.serverLevel();

    // 1. 检查是否已有分身存在 (可能在不同维度)
    PersistentGuCultivatorClone existingClone = findClone(level, organ);

    if (existingClone != null) {
      // 存在 -> 召回
      recallClone(organ, existingClone, player);
    } else {
      // 不存在或已死亡 -> 准备召唤新分身

      // P1修复: 检查是否有悬挂的UUID (分身已死亡或在不可访问的维度)
      CompoundTag stateTag = getStateTag(organ);
      boolean hadOldClone = !stateTag.isEmpty() && stateTag.hasUUID(CLONE_UUID_KEY);

      if (hadOldClone) {
        // 有旧UUID但findClone返回null: 可能在其他维度或已死亡
        // 尝试跨维度查找并强制召回
        PersistentGuCultivatorClone crossDimensionClone =
            findAndRecallCrossDimensionClone(level, player, organ);

        if (crossDimensionClone != null) {
          // 成功召回跨维度分身
          player.displayClientMessage(
              Component.literal("§e已自动召回其他维度的分身"),
              false
          );
        } else {
          // 实体已死亡或无法访问: 清理UUID
          cleanupCloneData(organ);
          player.displayClientMessage(
              Component.literal("§e旧分身已失联,数据已清理"),
              false
          );
        }
      }

      // 召唤新分身
      PersistentGuCultivatorClone newClone = summonClone(level, player, organ);

      if (newClone != null) {
        // 保存UUID到器官NBT
        updateStateTag(organ, tag -> {
          tag.putUUID(CLONE_UUID_KEY, newClone.getUUID());
          tag.putString(DIMENSION_KEY, level.dimension().location().toString());
        });

        player.displayClientMessage(
            Component.literal("§6分身已召唤"),
            true
        );
      } else {
        player.displayClientMessage(
            Component.literal("§c召唤失败!"),
            true
        );
      }
    }
  }

  /**
   * 查找分身实体
   *
   * <p>边界检查：
   * - 维度一致性：跨维度时查找原维度的实体
   * - 实体存活性：已死亡时返回null
   *
   * @return 存活的分身实体（可能在不同维度），或null
   */
  @Nullable
  private static PersistentGuCultivatorClone findClone(ServerLevel currentLevel, ItemStack organ) {
    CompoundTag stateTag = getStateTag(organ);

    if (stateTag.isEmpty() || !stateTag.hasUUID(CLONE_UUID_KEY)) {
      return null;
    }

    UUID cloneUUID = stateTag.getUUID(CLONE_UUID_KEY);
    ServerLevel targetLevel = currentLevel;

    // 检查维度一致性
    if (stateTag.contains(DIMENSION_KEY)) {
      String savedDim = stateTag.getString(DIMENSION_KEY);
      String currentDim = currentLevel.dimension().location().toString();

      if (!savedDim.equals(currentDim)) {
        // 跨维度场景: 尝试访问原维度
        try {
          ResourceKey<net.minecraft.world.level.Level> dimensionKey = ResourceKey.create(
              Registries.DIMENSION,
              ResourceLocation.parse(savedDim)
          );
          targetLevel = currentLevel.getServer().getLevel(dimensionKey);
        } catch (Exception e) {
          // 维度解析失败
          return null;
        }

        // 如果无法访问原维度,返回null
        if (targetLevel == null) {
          return null;
        }
      }
    }

    // 查找实体
    Entity entity = targetLevel.getEntity(cloneUUID);

    if (entity instanceof PersistentGuCultivatorClone clone && clone.isAlive()) {
      return clone;
    }

    // 实体已死亡或不存在
    return null;
  }

  /**
   * 召唤新分身
   *
   * <p>流程：
   * 1. 计算生成位置（玩家前方2格）
   * 2. 调用 PersistentGuCultivatorClone.spawn()
   * 3. 从器官NBT恢复数据（如果有）
   * 4. 播放召唤特效
   */
  @Nullable
  private static PersistentGuCultivatorClone summonClone(
      ServerLevel level,
      Player player,
      ItemStack organ
  ) {
    // 1. 计算生成位置 (玩家前方2格)
    Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0));

    // 2. 创建新分身 (使用静态工厂方法)
    PersistentGuCultivatorClone clone = PersistentGuCultivatorClone.spawn(
        level,
        player,
        spawnPos
    );

    if (clone == null) {
      return null;
    }

    // 3. 如果器官NBT中有存储的数据,恢复它
    CompoundTag stateTag = getStateTag(organ);
    if (!stateTag.isEmpty() && stateTag.contains(CLONE_DATA_KEY)) {
      CompoundTag cloneData = stateTag.getCompound(CLONE_DATA_KEY);
      clone.deserializeFromItemNBT(cloneData);
    }

    // 4. 播放召唤特效
    level.playSound(
        null,
        clone.blockPosition(),
        SoundEvents.ENDERMAN_TELEPORT,
        SoundSource.PLAYERS,
        1.0f,
        0.8f
    );

    return clone;
  }

  /**
   * 召回分身
   *
   * <p>流程：
   * 1. 保存分身数据到器官NBT
   * 2. 移除增益效果
   * 3. 播放召回特效
   * 4. 移除分身实体
   * 5. 清除临时UUID
   */
  private static void recallClone(
      ItemStack organ,
      PersistentGuCultivatorClone clone,
      Player player
  ) {
    // 1. 保存分身数据到器官NBT (使用专用方法)
    CompoundTag cloneData = clone.serializeToItemNBT();
    updateStateTag(organ, tag -> {
      tag.put(CLONE_DATA_KEY, cloneData);
    });

    // 2. 移除增益效果（在召回前）
    clone.removeBoostEffect();

    // 3. 播放召回特效
    clone.level().playSound(
        null,
        clone.blockPosition(),
        SoundEvents.ENDERMAN_TELEPORT,
        SoundSource.PLAYERS,
        1.0f,
        1.2f
    );

    // 4. 移除分身实体
    clone.discard();

    // 5. 清除UUID (因为实体已不存在)
    updateStateTag(organ, tag -> {
      tag.remove(CLONE_UUID_KEY);
      tag.remove(DIMENSION_KEY);
    });

    // 6. 提示玩家
    player.displayClientMessage(
        Component.literal("§6分身已召回"),
        true
    );
  }

  /**
   * 跨维度查找并强制召回分身 (P1修复: 防止资源泄漏)
   *
   * <p>场景: 玩家在维度A召唤分身,切换到维度B后再次召唤
   * <p>行为: 主动召回维度A的分身 (保存数据),而不是留下它继续运行
   *
   * @return 被召回的分身,如果成功召回; null如果无法找到或召回
   */
  @Nullable
  private static PersistentGuCultivatorClone findAndRecallCrossDimensionClone(
      ServerLevel currentLevel,
      Player player,
      ItemStack organ
  ) {
    CompoundTag stateTag = getStateTag(organ);

    if (stateTag.isEmpty() || !stateTag.hasUUID(CLONE_UUID_KEY)) {
      return null;
    }

    UUID cloneUUID = stateTag.getUUID(CLONE_UUID_KEY);
    String savedDim = stateTag.contains(DIMENSION_KEY) ? stateTag.getString(DIMENSION_KEY) : null;

    if (savedDim == null) {
      return null;
    }

    // 尝试访问保存的维度
    try {
      ResourceKey<net.minecraft.world.level.Level> dimensionKey = ResourceKey.create(
          Registries.DIMENSION,
          ResourceLocation.parse(savedDim)
      );

      ServerLevel targetLevel = currentLevel.getServer().getLevel(dimensionKey);
      if (targetLevel == null) {
        return null;
      }

      // 查找实体
      Entity entity = targetLevel.getEntity(cloneUUID);
      if (!(entity instanceof PersistentGuCultivatorClone clone) || !clone.isAlive()) {
        return null;
      }

      // 找到了! 强制召回 (保存数据并移除实体)
      CompoundTag cloneData = clone.serializeToItemNBT();
      updateStateTag(organ, tag -> {
        tag.put(CLONE_DATA_KEY, cloneData);
      });

      // 移除实体
      clone.discard();

      // 清除UUID (即将召唤新分身)
      updateStateTag(organ, tag -> {
        tag.remove(CLONE_UUID_KEY);
        tag.remove(DIMENSION_KEY);
      });

      return clone;

    } catch (Exception e) {
      // 维度解析失败或其他异常
      return null;
    }
  }

  /**
   * 惰性清理悬挂的分身数据
   *
   * <p>调用时机：
   * - findClone() 返回null时
   * - 玩家再次尝试召唤时
   */
  private static void cleanupCloneData(ItemStack organ) {
    updateStateTag(organ, tag -> {
      tag.remove(CLONE_UUID_KEY);
      tag.remove(CLONE_DATA_KEY);
      tag.remove(DIMENSION_KEY);
    });
  }

  /**
   * 所有权校验
   *
   * <p>逻辑：
   * - 首次使用：自动绑定当前玩家
   * - 后续使用：检查UUID是否匹配
   */
  private static boolean isOwnedBy(ItemStack organ, Player player) {
    CompoundTag stateTag = getStateTag(organ);

    // 如果器官没有绑定所有者,首次使用时绑定
    if (stateTag.isEmpty() || !stateTag.hasUUID(OWNER_UUID_KEY)) {
      updateStateTag(organ, tag -> {
        tag.putUUID(OWNER_UUID_KEY, player.getUUID());
      });
      return true;
    }

    UUID ownerUUID = stateTag.getUUID(OWNER_UUID_KEY);
    return ownerUUID.equals(player.getUUID());
  }

  // ============ 辅助方法 ============

  /**
   * 在胸腔中查找多重剑影蛊器官
   */
  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  /**
   * 检查胸腔中是否有多重剑影蛊器官
   */
  private static boolean hasOrgan(ChestCavityInstance cc) {
    return !findOrgan(cc).isEmpty();
  }

  /**
   * 获取器官状态标签（只读）
   */
  private static CompoundTag getStateTag(ItemStack organ) {
    if (organ.isEmpty()) {
      return new CompoundTag();
    }
    CustomData data = organ.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return new CompoundTag();
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(STATE_ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
      return new CompoundTag();
    }
    return root.getCompound(STATE_ROOT);
  }

  /**
   * 更新器官状态标签
   */
  private static void updateStateTag(ItemStack organ, java.util.function.Consumer<CompoundTag> modifier) {
    if (organ.isEmpty()) {
      return;
    }
    NBTWriter.updateCustomData(organ, root -> {
      CompoundTag state = root.contains(STATE_ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)
          ? root.getCompound(STATE_ROOT)
          : new CompoundTag();
      modifier.accept(state);
      if (state.isEmpty()) {
        root.remove(STATE_ROOT);
      } else {
        root.put(STATE_ROOT, state);
      }
    });
  }
}
