package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueYiGuEffects;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/**
 * 主动技1：血涌披身（Blood Aura）
 *
 * <p>开关型光环，范围2格，每0.5秒对敌人施加流血DoT。
 *
 * <p>消耗：每秒baseCost生命3点 + 真元 + 精力
 *
 * <p>冷却：8秒
 */
public final class XueYongPiShenSkill {

  private static final String STATE_ROOT = "XueYiGu";
  private static final String AURA_ACTIVE_KEY = "AuraActive";
  private static final String AURA_TICK_COUNTER_KEY = "AuraTick";
  private static final String COOLDOWN_READY_AT_KEY = "XueYongPiShen_ReadyAt";

  private static final int COOLDOWN_TICKS = 160; // 8 seconds
  private static final double AURA_RADIUS = 2.0;
  private static final int AURA_TICK_INTERVAL = 10; // 0.5 seconds (10 ticks)

  // Base costs (1转1阶段标准)
  private static final double BASE_ZHENYUAN_PER_SECOND = 20.0;
  private static final double BASE_JINGLI_PER_SECOND = 2.0;
  private static final float BASE_HEALTH_PER_SECOND = 3.0f;

  // Bleed DoT per hit (scaled by tier)
  private static final float BASE_BLEED_DAMAGE = 2.5f; // ~5 damage/sec at 0.5sec intervals
  private static final int BLEED_DURATION_TICKS = 60; // 3 seconds

  private XueYongPiShenSkill() {}

  public static void bootstrap() {
    // Register activation listener
    OrganActivationListeners.register(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_yong_pi_shen"),
        XueYongPiShenSkill::activate);
  }

  /**
   * Toggles blood aura on/off when player activates skill.
   */
  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }

    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }
    ItemStack organ = organOpt.get();

    OrganState state = OrganState.of(organ, STATE_ROOT);
    boolean currentlyActive = state.getBoolean(AURA_ACTIVE_KEY, false);

    if (currentlyActive) {
      // Deactivate aura
      deactivateAura(player, cc, organ, state);
    } else {
      // Try to activate aura
      activateAura(player, cc, organ, state);
    }
  }

  /**
   * Activates blood aura if cooldown is ready.
   */
  private static void activateAura(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, OrganState state) {

    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry = cooldown.entry(COOLDOWN_READY_AT_KEY).withDefault(0L);

    Level level = player.level();
    long now = level.getGameTime();

    // Check cooldown
    if (!entry.isReady(now)) {
      long remaining = entry.getReadyTick() - now;
      sendFailure(player, "血涌披身冷却中，还需 " + (remaining / 20) + " 秒。");
      return;
    }

    // Check initial resource cost
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法获取真元，血涌披身启动失败。");
      return;
    }

    ResourceHandle handle = handleOpt.get();

    // Check if player has enough health
    if (player.getHealth() <= BASE_HEALTH_PER_SECOND + 1.0f) {
      sendFailure(player, "生命不足，无法维持血涌披身。");
      return;
    }

    // Activate aura
    state.setBoolean(AURA_ACTIVE_KEY, true);
    state.setInt(AURA_TICK_COUNTER_KEY, 0);

    // Play activation effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playAuraActivation(serverLevel, player);
    }

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    player.displayClientMessage(Component.literal("血涌披身 - 已激活"), true);
  }

  /**
   * Deactivates blood aura and starts cooldown.
   */
  private static void deactivateAura(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, OrganState state) {

    state.setBoolean(AURA_ACTIVE_KEY, false);
    state.setInt(AURA_TICK_COUNTER_KEY, 0);

    // Start cooldown
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry = cooldown.entry(COOLDOWN_READY_AT_KEY).withDefault(0L);

    Level level = player.level();
    long now = level.getGameTime();
    entry.setReadyAt(now + COOLDOWN_TICKS);

    // Schedule toast notification
    ActiveSkillRegistry.scheduleReadyToast(
        player,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_yong_pi_shen"),
        entry.getReadyTick(),
        now);

    // Play deactivation effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playAuraDeactivation(serverLevel, player);
    }

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    player.displayClientMessage(Component.literal("血涌披身 - 已停止"), true);
  }

  /**
   * Tick function called from organ behavior. Handles aura logic and resource consumption.
   */
  public static void tickAura(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    OrganState state = OrganState.of(organ, STATE_ROOT);

    boolean active = state.getBoolean(AURA_ACTIVE_KEY, false);
    if (!active) {
      return;
    }

    Level level = player.level();
    int tickCounter = state.getInt(AURA_TICK_COUNTER_KEY, 0);

    // Increment tick counter
    tickCounter++;
    state.setInt(AURA_TICK_COUNTER_KEY, tickCounter);

    // Every second (20 ticks), consume resources
    if (tickCounter % 20 == 0) {
      if (!consumeAuraResources(player)) {
        // Not enough resources, deactivate aura
        deactivateAura(player, cc, organ, state);
        sendFailure(player, "资源不足，血涌披身自动停止。");
        return;
      }
    }

    // Every 0.5 seconds (10 ticks), damage nearby enemies
    if (tickCounter % AURA_TICK_INTERVAL == 0) {
      damageNearbyEnemies(player, level);
    }

    // Play continuous aura effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playAuraMaintain(serverLevel, player, tickCounter);
    }
  }

  /**
   * Consumes per-second resources for maintaining aura.
   *
   * @return true if resources were successfully consumed, false otherwise
   */
  private static boolean consumeAuraResources(ServerPlayer player) {
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }

    ResourceHandle handle = handleOpt.get();

    // Check health
    if (player.getHealth() <= BASE_HEALTH_PER_SECOND + 1.0f) {
      return false;
    }

    // Consume scaled resources (按转数缩放)
    if (handle.consumeScaledZhenyuan(BASE_ZHENYUAN_PER_SECOND).isEmpty()) {
      return false;
    }
    if (handle.consumeScaledJingli(BASE_JINGLI_PER_SECOND).isEmpty()) {
      return false;
    }

    // Consume health (using organ cost damage type)
    GuzhenrenResourceCostHelper.drainHealth(
        player, BASE_HEALTH_PER_SECOND, 1.0f, player.damageSources().generic());

    return true;
  }

  /**
   * Damages all nearby enemies within aura radius.
   */
  private static void damageNearbyEnemies(ServerPlayer player, Level level) {
    Vec3 center = player.position();
    AABB searchBox =
        new AABB(center.x - AURA_RADIUS, center.y - 1, center.z - AURA_RADIUS, center.x + AURA_RADIUS, center.y + player.getBbHeight() + 1, center.z + AURA_RADIUS);

    List<LivingEntity> nearbyEntities =
        level.getEntitiesOfClass(
            LivingEntity.class, searchBox, entity -> entity != player && !entity.isAlliedTo(player));

    for (LivingEntity target : nearbyEntities) {
      // Apply bleed DoT
      applyBleedEffect(player, target);

      // Play hit effect
      if (level instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playAuraHitTarget(serverLevel, target);
      }
    }
  }

  /**
   * Applies bleeding effect to target.
   */
  private static void applyBleedEffect(ServerPlayer player, LivingEntity target) {
    // TODO: Apply actual bleed DoT effect
    // This requires integration with the bleed system
    // For now, apply direct damage

    // Get tier-scaled damage
    double tier = getTierLevel(player);
    float damage = BASE_BLEED_DAMAGE * (float) (1.0 + tier * 0.2);

    // Apply damage
    target.hurt(player.damageSources().magic(), damage);
  }

  /**
   * Gets player's tier level (1-5).
   */
  private static double getTierLevel(ServerPlayer player) {
    // TODO: Implement tier detection
    // For now, return 2 (assume 2转)
    return 2.0;
  }

  /**
   * Finds xue yi gu organ in chest cavity.
   */
  private static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }

    net.minecraft.resources.ResourceLocation organId =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "xueyigu");

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        net.minecraft.resources.ResourceLocation itemId =
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (organId.equals(itemId)) {
          return Optional.of(stack);
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Checks if aura is currently active for a player.
   */
  public static boolean isAuraActive(ChestCavityInstance cc) {
    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return false;
    }

    OrganState state = OrganState.of(organOpt.get(), STATE_ROOT);
    return state.getBoolean(AURA_ACTIVE_KEY, false);
  }

  /**
   * Forces aura deactivation (called when organ is removed).
   */
  public static void forceDeactivate(ItemStack organ) {
    OrganState state = OrganState.of(organ, STATE_ROOT);
    state.setBoolean(AURA_ACTIVE_KEY, false);
    state.setInt(AURA_TICK_COUNTER_KEY, 0);
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
