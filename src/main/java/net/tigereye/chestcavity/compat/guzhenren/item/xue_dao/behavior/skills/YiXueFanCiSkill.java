package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueYiGuEffects;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/**
 * 主动技4：溢血反刺（Blood Reflect）
 *
 * <p>3秒窗口期，将承受的近战伤害反射50%为流血DoT给攻击者。
 *
 * <p>消耗：baseCost真元110、念头3
 *
 * <p>冷却：25秒
 */
public final class YiXueFanCiSkill {

  private static final String STATE_ROOT = "XueYiGu";
  private static final String COOLDOWN_READY_AT_KEY = "YiXueFanCi_ReadyAt";
  private static final String REFLECT_WINDOW_END_KEY = "ReflectWindowEnd";
  private static final String REFLECT_TICK_COUNTER_KEY = "ReflectTick";

  private static final int COOLDOWN_TICKS = 500; // 25 seconds
  private static final int REFLECT_WINDOW_DURATION = 60; // 3 seconds

  // Base costs (1转1阶段标准)
  private static final double BASE_ZHENYUAN = 110.0;
  private static final double BASE_NIANTOU = 3.0;

  // Reflect parameters
  private static final float REFLECT_PERCENTAGE = 0.5f; // 50% reflect
  private static final float REFLECT_CAP_PER_HIT = 40.0f; // Max reflect per hit
  private static final int BLEED_DURATION_TICKS = 100; // 5 seconds

  private YiXueFanCiSkill() {}

  public static void bootstrap() {
    // Register activation listener
    OrganActivationListeners.register(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/yi_xue_fan_ci"),
        YiXueFanCiSkill::activate);
  }

  /** Activates blood reflect skill when player triggers it. */
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
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry = cooldown.entry(COOLDOWN_READY_AT_KEY).withDefault(0L);

    Level level = player.level();
    long now = level.getGameTime();

    // Check cooldown
    if (!entry.isReady(now)) {
      long remaining = entry.getReadyTick() - now;
      sendFailure(player, "溢血反刺冷却中，还需 " + (remaining / 20) + " 秒。");
      return;
    }

    // Check resource cost
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法获取真元，溢血反刺施放失败。");
      return;
    }

    ResourceHandle handle = handleOpt.get();

    // Check Niantou resource
    double currentNiantou = handle.getNiantou().orElse(0.0);
    if (currentNiantou < BASE_NIANTOU) {
      sendFailure(player, "资源不足，溢血反刺施放失败。");
      return;
    }

    // Consume scaled resources
    if (handle.consumeScaledZhenyuan(BASE_ZHENYUAN).isEmpty()) {
      sendFailure(player, "资源不足，溢血反刺施放失败。");
      return;
    }
    if (handle.adjustNiantou(-BASE_NIANTOU, true).isEmpty()) {
      sendFailure(player, "资源不足，溢血反刺施放失败。");
      return;
    }

    // Activate reflect window
    long windowEnd = now + REFLECT_WINDOW_DURATION;
    state.setLong(REFLECT_WINDOW_END_KEY, windowEnd);
    state.setInt(REFLECT_TICK_COUNTER_KEY, 0);

    // Play activation effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playReflectWindowStart(serverLevel, player);
    }

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    player.displayClientMessage(Component.literal("溢血反刺 - 反刺窗口开启（3秒）"), true);

    // Schedule cooldown after window ends
    entry.setReadyAt(windowEnd + (COOLDOWN_TICKS - REFLECT_WINDOW_DURATION));
    ActiveSkillRegistry.scheduleReadyToast(
        player,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/yi_xue_fan_ci"),
        entry.getReadyTick(),
        now);
  }

  /** Tick function for reflect window maintenance. */
  public static void tickReflectWindow(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    OrganState state = OrganState.of(organ, STATE_ROOT);

    Level level = player.level();
    long now = level.getGameTime();
    long windowEnd = state.getLong(REFLECT_WINDOW_END_KEY, 0L);

    if (now >= windowEnd || windowEnd == 0L) {
      // Window ended
      if (windowEnd != 0L && windowEnd != Long.MAX_VALUE) {
        // Just ended, play end effect
        if (level instanceof ServerLevel serverLevel) {
          XueYiGuEffects.playReflectWindowEnd(serverLevel, player);
        }
        player.displayClientMessage(Component.literal("溢血反刺 - 反刺窗口结束"), true);

        state.setLong(REFLECT_WINDOW_END_KEY, 0L);
        state.setInt(REFLECT_TICK_COUNTER_KEY, 0);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
      return;
    }

    // Window is active
    int tickCounter = state.getInt(REFLECT_TICK_COUNTER_KEY, 0);
    tickCounter++;
    state.setInt(REFLECT_TICK_COUNTER_KEY, tickCounter);

    // Play maintain effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playReflectWindowMaintain(serverLevel, player, tickCounter);
    }
  }

  /**
   * Handles incoming damage during reflect window. Called from damage listener.
   *
   * @param player The player with reflect active
   * @param attacker The entity that attacked
   * @param damage The damage amount received
   * @param cc Chest cavity instance
   * @return true if reflect was triggered
   */
  public static boolean handleReflectDamage(
      ServerPlayer player, LivingEntity attacker, float damage, ChestCavityInstance cc) {

    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return false;
    }

    ItemStack organ = organOpt.get();
    OrganState state = OrganState.of(organ, STATE_ROOT);

    Level level = player.level();
    long now = level.getGameTime();
    long windowEnd = state.getLong(REFLECT_WINDOW_END_KEY, 0L);

    // Check if window is active
    if (now >= windowEnd || windowEnd == 0L) {
      return false;
    }

    // Calculate reflect damage
    float reflectDamage = damage * REFLECT_PERCENTAGE;
    reflectDamage = Math.min(reflectDamage, REFLECT_CAP_PER_HIT);

    if (reflectDamage <= 0) {
      return false;
    }

    // Apply reflected damage as bleed DoT
    applyReflectBleed(player, attacker, reflectDamage);

    // Play reflect effect
    if (level instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playReflectTrigger(serverLevel, player, attacker, reflectDamage);
    }

    return true;
  }

  /** Applies reflected damage as bleeding effect on attacker. */
  private static void applyReflectBleed(
      ServerPlayer player, LivingEntity attacker, float totalDamage) {
    // TODO: Apply actual bleed DoT through bleed system
    // For now, apply direct damage spread over time

    // Apply instant damage (in real implementation, use tick-based bleed)
    attacker.hurt(player.damageSources().thorns(player), totalDamage * 0.2f);
  }

  /** Checks if reflect window is currently active. */
  public static boolean isReflectActive(ChestCavityInstance cc) {
    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return false;
    }

    ItemStack organ = organOpt.get();
    OrganState state = OrganState.of(organ, STATE_ROOT);

    long windowEnd = state.getLong(REFLECT_WINDOW_END_KEY, 0L);
    if (windowEnd == 0L) {
      return false;
    }

    // Check if window is still active (need to get current time)
    // This is a simplified check; real implementation should pass level
    return windowEnd > 0L && windowEnd != Long.MAX_VALUE;
  }

  /** Forces reflect window to end (called when organ is removed). */
  public static void forceDeactivate(ItemStack organ) {
    OrganState state = OrganState.of(organ, STATE_ROOT);
    state.setLong(REFLECT_WINDOW_END_KEY, 0L);
    state.setInt(REFLECT_TICK_COUNTER_KEY, 0);
  }

  /** Finds xue yi gu organ in chest cavity. */
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

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }
}
