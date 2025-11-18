package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JiandaoCooldownOps;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JiandaoDaohenOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
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
public enum DuochongjianyingGuOrganBehavior
    implements OrganOnHitListener, OrganIncomingDamageListener {
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
  private static final String KEY_COOLDOWN = "AbilityCooldown";
  private static final long BASE_COOLDOWN_TICKS = 120L * 20L;
  private static final long MIN_COOLDOWN_TICKS = 20L;

  static {
    // 注册主动技能激活监听器
    OrganActivationListeners.register(ABILITY_ID, DuochongjianyingGuOrganBehavior::activateAbility);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
        //BUG: 玩家有可能攻击自己，需要排除
    if (!(attacker instanceof ServerPlayer player)
        || attacker.level().isClientSide()
        || target == null
        || !target.isAlive()
        || organ.isEmpty()
        || !matchesOrgan(organ)) {
      return damage;
    }
    commandCloneToAttack(player, organ, target);
    return damage;
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
        //BUG: 玩家有可能攻击自己，需要排除
    if (!(victim instanceof ServerPlayer player)
        || victim.level().isClientSide()
        || organ.isEmpty()
        || !matchesOrgan(organ)) {
      return damage;
    }
    LivingEntity aggressor = resolveAggressor(source);
    if (aggressor != null && aggressor.isAlive()) {
      commandCloneToAttack(player, organ, aggressor);
    }
    return damage;
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
   *   <li>召唤/召回分身
   *   <li>打开分身界面: 通过空手Shift+右键Entity触发（见 {@link net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone#mobInteract}）
   * </ul>
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }

    List<ItemStack> organs = findOrgans(cc);
    if (organs.isEmpty()) {
      player.displayClientMessage(Component.literal("§c未找到多重剑影蛊!"), true);
      return;
    }

    List<ItemStack> usable = new ArrayList<>();
    boolean denied = false;
    for (ItemStack organ : organs) {
      if (isOwnedBy(organ, player)) {
        usable.add(organ);
      } else {
        denied = true;
      }
    }

    if (usable.isEmpty()) {
      if (denied) {
        player.displayClientMessage(Component.literal("§c胸腔中的多重剑影蛊归属不匹配"), true);
      } else {
        player.displayClientMessage(Component.literal("§c没有可用的多重剑影蛊"), true);
      }
      return;
    }

    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();

    List<MultiCooldown.Entry> cooldownEntries = new ArrayList<>(usable.size());
    long maxRemaining = 0L;
    for (ItemStack organ : usable) {
      MultiCooldown.Entry entry = cooldownEntry(cc, organ);
      cooldownEntries.add(entry);
      maxRemaining = Math.max(maxRemaining, entry.remaining(now));
    }

    if (maxRemaining > 0L) {
      long seconds = Math.max(1L, (maxRemaining + 19L) / 20L);
      player.displayClientMessage(
          Component.literal("§c分身冷却中，剩余 " + seconds + " 秒"),
          true);
      return;
    }

    double snapDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "jiandao:daohen_jiandao", Double.NaN);
    double snapLiupai =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "jiandao:liupai_jiandao", Double.NaN);
    double effectiveDaohen = resolveEffectiveDaohen(player, snapDaoHen);
    int liupaiExp = resolveJiandaoLiupaiExp(player, snapLiupai);
    long cooldownTicks =
        JiandaoCooldownOps.withJiandaoExp(BASE_COOLDOWN_TICKS, liupaiExp, MIN_COOLDOWN_TICKS);

    boolean anyActive = false;
    for (ItemStack organ : usable) {
      if (findClone(level, organ) != null) {
        anyActive = true;
        break;
      }
    }

    int processed = 0;
    if (anyActive) {
      int recalled = 0;
      for (ItemStack organ : usable) {
        if (recallCloneForOrgan(player, organ, true)) {
          recalled++;
        }
      }
      processed = recalled;
      if (recalled > 0) {
        player.displayClientMessage(Component.literal("§6已召回 " + recalled + " 个分身"), true);
      } else {
        player.displayClientMessage(Component.literal("§e没有可召回的分身"), true);
      }
    } else {
      int summoned = 0;
      for (ItemStack organ : usable) {
        if (summonCloneForOrgan(player, organ, effectiveDaohen)) {
          summoned++;
        }
      }
      processed = summoned;
      if (summoned > 0) {
        player.displayClientMessage(Component.literal("§6已召唤 " + summoned + " 个分身"), true);
      } else {
        player.displayClientMessage(Component.literal("§c召唤失败"), true);
      }
    }

    if (processed > 0) {
      long readyTick = now + cooldownTicks;
      for (MultiCooldown.Entry entry : cooldownEntries) {
        entry.setReadyAt(readyTick);
      }
    }
  }

  /**
   * 确保器官附加时的初始化
   *
   * <p>当器官首次放入胸腔时调用。
   */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
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

  private static boolean summonCloneForOrgan(
      ServerPlayer player, ItemStack organ, double effectiveDaohen) {
    ServerLevel level = player.serverLevel();
    if (findClone(level, organ) != null) {
      return false;
    }

    CompoundTag stateTag = getStateTag(organ);
    boolean hadOldClone = !stateTag.isEmpty() && stateTag.hasUUID(CLONE_UUID_KEY);
    if (hadOldClone) {
      PersistentGuCultivatorClone crossDimensionClone =
          findAndRecallCrossDimensionClone(level, player, organ);
      if (crossDimensionClone != null) {
        player.displayClientMessage(
            Component.literal("§e已自动召回其他维度的分身"),
            false);
      } else {
        player.displayClientMessage(
            Component.literal("§e旧分身已失联,数据已清理"),
            false);
      }
      cleanupCloneData(organ);
    }

    PersistentGuCultivatorClone newClone =
        summonClone(level, player, organ, computeAttributeMultiplier(effectiveDaohen));
    if (newClone == null) {
      return false;
    }

    updateStateTag(organ, tag -> {
      tag.putUUID(CLONE_UUID_KEY, newClone.getUUID());
      tag.putString(DIMENSION_KEY, level.dimension().location().toString());
    });

    return true;
  }

  private static boolean recallCloneForOrgan(ServerPlayer player, ItemStack organ, boolean silent) {
    ServerLevel level = player.serverLevel();
    PersistentGuCultivatorClone existingClone = findClone(level, organ);
    if (existingClone != null) {
      recallClone(organ, existingClone, player, silent);
      return true;
    }

    PersistentGuCultivatorClone crossDimensionClone =
        findAndRecallCrossDimensionClone(level, player, organ);
    if (crossDimensionClone != null) {
      if (!silent) {
        player.displayClientMessage(
            Component.literal("§e已自动召回其他维度的分身"),
            false);
      }
      return true;
    }

    CompoundTag stateTag = getStateTag(organ);
    if (!stateTag.isEmpty() && stateTag.hasUUID(CLONE_UUID_KEY)) {
      cleanupCloneData(organ);
      if (!silent) {
        player.displayClientMessage(
            Component.literal("§e旧分身记录已清理"),
            false);
      }
      return true;
    }

    return false;
  }

  private static void commandCloneToAttack(
      ServerPlayer player, ItemStack organ, LivingEntity target) {
    if (target == null || !target.isAlive()) {
      return;
    }
    PersistentGuCultivatorClone clone = findClone(player.serverLevel(), organ);
    if (clone == null || !clone.isAlive()) {
      return;
    }
    if (clone == target || clone.isAlliedTo(target)) {
      return;
    }
    clone.setAggressive(true);
    clone.setTarget(target);
    clone.setLastHurtByMob(target);
    clone.getNavigation().moveTo(target, 1.1);
  }

  @Nullable
  private static LivingEntity resolveAggressor(@Nullable DamageSource source) {
    if (source == null) {
      return null;
    }
    Entity direct = source.getDirectEntity();
    if (direct instanceof LivingEntity living) {
      return living;
    }
    Entity owner = source.getEntity();
    if (owner instanceof LivingEntity living) {
      return living;
    }
    return null;
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
      ItemStack organ,
      double attributeMultiplier
  ) {
    // 1. 计算生成位置 (玩家前方2格)
    Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0));

    // 2. 创建新分身 (使用静态工厂方法)
    PersistentGuCultivatorClone clone = PersistentGuCultivatorClone.spawn(
        level,
        player,
        spawnPos,
        attributeMultiplier
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
    recallClone(organ, clone, player, false);
  }

  private static void recallClone(
      ItemStack organ,
      PersistentGuCultivatorClone clone,
      Player player,
      boolean silent
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
    if (!silent) {
      player.displayClientMessage(
          Component.literal("§6分身已召回"),
          true
      );
    }
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

  private static List<ItemStack> findOrgans(ChestCavityInstance cc) {
    List<ItemStack> organs = new ArrayList<>();
    if (cc == null || cc.inventory == null) {
      return organs;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack)) {
        organs.add(stack);
      }
    }
    return organs;
  }

  private static boolean hasOrgan(ChestCavityInstance cc) {
    return !findOrgans(cc).isEmpty();
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return ORGAN_ID.equals(id);
  }

  private static MultiCooldown.Entry cooldownEntry(ChestCavityInstance cc, ItemStack organ) {
    OrganState state = OrganState.of(organ, STATE_ROOT);
    return MultiCooldown.builder(state).withSync(cc, organ).build().entry(KEY_COOLDOWN);
  }

  private static double resolveEffectiveDaohen(ServerPlayer player, double snapshotValue) {
    if (Double.isFinite(snapshotValue)) {
      return Math.max(0.0, snapshotValue);
    }
    long now = player.serverLevel().getGameTime();
    return Math.max(0.0, JiandaoDaohenOps.effectiveCached(player, now));
  }

  private static int resolveJiandaoLiupaiExp(ServerPlayer player, double snapshotValue) {
    double value;
    if (Double.isFinite(snapshotValue)) {
      value = snapshotValue;
    } else {
      value =
          ResourceOps.openHandle(player)
              .map(h -> h.read("liupai_jiandao").orElse(0.0))
              .orElse(0.0);
    }
    return (int) Math.max(0L, Math.round(value));
  }

  private static double computeAttributeMultiplier(double effectiveDaohen) {
    return 1.0 + Math.max(0.0, effectiveDaohen) / 1000.0;
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
