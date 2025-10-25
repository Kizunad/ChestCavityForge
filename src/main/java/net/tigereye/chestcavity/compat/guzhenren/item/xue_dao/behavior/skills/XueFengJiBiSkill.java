package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/**
 * 主动技3：血缝急闭（Blood Seal）
 *
 * <p>将附近敌人的流血值转为玩家临时生命(Absorption)，并清除自身流血类减益。
 *
 * <p>消耗：baseCost真元90、魂魄2
 *
 * <p>冷却：20秒
 */
public final class XueFengJiBiSkill {

  private static final String STATE_ROOT = "XueYiGu";
  private static final String COOLDOWN_READY_AT_KEY = "XueFengJiBi_ReadyAt";

  private static final int COOLDOWN_TICKS = 400; // 20 seconds
  private static final double SCAN_RADIUS = 4.0;

  // Base costs (1转1阶段标准)
  private static final double BASE_ZHENYUAN = 90.0;
  private static final double BASE_HUNPO = 2.0;

  // Absorption parameters
  private static final float BASE_ABSORPTION_PER_BLEED = 0.5f; // 50% of bleed value
  private static final float ABSORPTION_CAP_MULTIPLIER = 30.0f; // Base cap
  private static final int ABSORPTION_DURATION_TICKS = 100; // 5 seconds

  private XueFengJiBiSkill() {}

  public static void bootstrap() {
    // Register activation listener
    OrganActivationListeners.register(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_feng_ji_bi"),
        XueFengJiBiSkill::activate);
  }

  /** Activates blood seal skill when player triggers it. */
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
      sendFailure(player, "血缝急闭冷却中，还需 " + (remaining / 20) + " 秒。");
      return;
    }

    // Check resource cost
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法获取真元，血缝急闭施放失败。");
      return;
    }

    ResourceHandle handle = handleOpt.get();

    // Find bleeding targets nearby
    List<LivingEntity> bleedingTargets = findBleedingTargets(player, (ServerLevel) level);

    if (bleedingTargets.isEmpty()) {
      sendFailure(player, "附近无流血目标。");
      return;
    }

    // Check Hunpo resource
    double currentHunpo = handle.getHunpo().orElse(0.0);
    if (currentHunpo < BASE_HUNPO) {
      sendFailure(player, "资源不足，血缝急闭施放失败。");
      return;
    }

    // Consume scaled resources
    if (handle.consumeScaledZhenyuan(BASE_ZHENYUAN).isEmpty()) {
      sendFailure(player, "资源不足，血缝急闭施放失败。");
      return;
    }
    if (handle.adjustHunpo(-BASE_HUNPO, true).isEmpty()) {
      sendFailure(player, "资源不足，血缝急闭施放失败。");
      return;
    }

    // Execute skill
    executeBloodSeal(player, bleedingTargets, (ServerLevel) level);

    // Start cooldown
    entry.setReadyAt(now + COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(
        player,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "guzhenren", "xue_yi_gu/xue_feng_ji_bi"),
        entry.getReadyTick(),
        now);

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  /** Finds all bleeding entities within scan radius. */
  private static List<LivingEntity> findBleedingTargets(ServerPlayer player, ServerLevel level) {
    Vec3 center = player.position();
    AABB searchBox =
        new AABB(
            center.x - SCAN_RADIUS,
            center.y - 2,
            center.z - SCAN_RADIUS,
            center.x + SCAN_RADIUS,
            center.y + player.getBbHeight() + 2,
            center.z + SCAN_RADIUS);

    List<LivingEntity> nearbyEntities =
        level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != player && !entity.isAlliedTo(player));

    List<LivingEntity> bleedingTargets = new ArrayList<>();
    for (LivingEntity entity : nearbyEntities) {
      // Check if entity is bleeding (has any damage-over-time effect)
      // TODO: Check for actual bleed effect once integrated with bleed system
      // For now, check for any harmful effect as proxy
      if (hasHarmfulEffect(entity)) {
        bleedingTargets.add(entity);
      }
    }

    return bleedingTargets;
  }

  /** Checks if entity has harmful effects (proxy for bleeding). */
  private static boolean hasHarmfulEffect(LivingEntity entity) {
    for (MobEffectInstance effect : entity.getActiveEffects()) {
      if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
        return true;
      }
    }
    return false;
  }

  /** Executes blood seal effect. */
  private static void executeBloodSeal(
      ServerPlayer player, List<LivingEntity> targets, ServerLevel level) {

    // Play scan effect
    XueYiGuEffects.playSealScan(level, player, targets);

    // Calculate total absorption amount
    float totalAbsorption = 0.0f;

    // For each bleeding target, extract blood value
    for (LivingEntity target : targets) {
      // TODO: Get actual bleed damage value from bleed system
      // For now, estimate based on target's missing health
      float missingHealth = target.getMaxHealth() - target.getHealth();
      float extractedValue = Math.min(missingHealth * 0.1f, 10.0f); // Cap per target

      totalAbsorption += extractedValue * BASE_ABSORPTION_PER_BLEED;

      // Deal minor damage to target (extracting blood)
      target.hurt(player.damageSources().magic(), extractedValue * 0.2f);
    }

    // Apply tier multiplier
    double tier = getTierLevel(player);
    float tierMultiplier = 1.0f + (float) (tier * 0.3f);
    totalAbsorption *= tierMultiplier;

    // Cap absorption
    float absorptionCap = ABSORPTION_CAP_MULTIPLIER * (float) tier;
    totalAbsorption = Math.min(totalAbsorption, absorptionCap);

    // Play absorption effect
    XueYiGuEffects.playSealAbsorption(level, player, targets, 10);

    // Apply absorption to player (delayed)
    final float finalAbsorptionAmount = totalAbsorption;
    level
        .getServer()
        .tell(
            new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 40,
                () -> {
                  applyAbsorption(player, finalAbsorptionAmount);
                  XueYiGuEffects.playSealAbsorptionGain(level, player, finalAbsorptionAmount);
                }));

    // Clear player's harmful effects (bleeding/wither)
    clearHarmfulEffects(player);

    // Success feedback
    player.displayClientMessage(
        Component.literal("血缝急闭成功！获得 " + (int) totalAbsorption + " 点临时生命"), true);
  }

  /** Applies absorption (temporary health) to player. */
  private static void applyAbsorption(ServerPlayer player, float amount) {
    if (amount <= 0) {
      return;
    }

    // 联动5: 魂盾叠层 - 如果装备了魂盾蛊，额外增加吸收量
    Optional<net.tigereye.chestcavity.interfaces.ChestCavityEntity> ccEntityOpt =
        net.tigereye.chestcavity.interfaces.ChestCavityEntity.of(player);
    if (ccEntityOpt.isPresent()) {
      net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance cc =
          ccEntityOpt.get().getChestCavityInstance();
      if (hasHunDunGu(cc)) {
        // 增加30%的吸收量
        amount *= 1.3f;
        player.displayClientMessage(Component.literal("魂盾叠层：吸收量增强！"), true);
      }
    }

    // Apply absorption effect
    MobEffectInstance existingAbsorption = player.getEffect(MobEffects.ABSORPTION);

    if (existingAbsorption != null) {
      // Stack with existing absorption (take higher amount, longer duration)
      float currentAmount = player.getAbsorptionAmount();
      float newAmount = Math.max(currentAmount, amount);
      int newDuration = Math.max(existingAbsorption.getDuration(), ABSORPTION_DURATION_TICKS);

      player.removeEffect(MobEffects.ABSORPTION);
      player.addEffect(
          new MobEffectInstance(MobEffects.ABSORPTION, newDuration, 0, false, false, true));
      player.setAbsorptionAmount(newAmount);
    } else {
      // Fresh absorption
      player.addEffect(
          new MobEffectInstance(
              MobEffects.ABSORPTION, ABSORPTION_DURATION_TICKS, 0, false, false, true));
      player.setAbsorptionAmount(amount);
    }
  }

  /**
   * 联动5: 魂盾叠层 - 检查是否装备了魂盾蛊。
   */
  private static boolean hasHunDunGu(net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }

    net.minecraft.resources.ResourceLocation hundunguId =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "hundungu");

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        net.minecraft.resources.ResourceLocation itemId =
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (hundunguId.equals(itemId)) {
          return true;
        }
      }
    }

    return false;
  }

  /** Clears harmful effects from player (bleeding, wither, poison). */
  private static void clearHarmfulEffects(ServerPlayer player) {
    List<MobEffectInstance> toRemove = new ArrayList<>();

    for (MobEffectInstance effect : player.getActiveEffects()) {
      // Remove harmful DoT effects
      if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
        // Specifically target DoT effects
        if (effect.getEffect() == MobEffects.WITHER
            || effect.getEffect() == MobEffects.POISON
            || effect.getEffect() == MobEffects.HARM) {
          toRemove.add(effect);
        }
      }
    }

    for (MobEffectInstance effect : toRemove) {
      player.removeEffect(effect.getEffect());
    }
  }

  /** Gets player's tier level (1-5). */
  private static double getTierLevel(ServerPlayer player) {
    // TODO: Implement tier detection
    // For now, return 2 (assume 2转)
    return 2.0;
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
